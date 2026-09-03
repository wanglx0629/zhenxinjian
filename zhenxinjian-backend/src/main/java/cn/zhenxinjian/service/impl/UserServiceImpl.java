package cn.zhenxinjian.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.LoginDTO;
import cn.zhenxinjian.domain.dto.RegisterDTO;
import cn.zhenxinjian.domain.dto.UserDTO;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.query.UserQuery;
import cn.zhenxinjian.domain.vo.CaptchaVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.domain.vo.UserVO;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.UserService;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.websocket.WebSocketSessionRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * 用户 Service 实现
 * 作者: luote (luote) - https://luote996.cn
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            CommonConstant.ROLE_USER, CommonConstant.ROLE_ADMIN);

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final UserCacheService userCacheService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final ZhenxinjianProperties luoteProperties;

    @Override
    public CaptchaVO getCaptcha() {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 4);
        String uuid = IdUtil.simpleUUID();
        redisUtils.saveCaptcha(uuid, lineCaptcha.getCode().toLowerCase());
        CaptchaVO vo = new CaptchaVO();
        vo.setUuid(uuid);
        vo.setImage("data:image/png;base64," + lineCaptcha.getImageBase64());
        return vo;
    }

    @Override
    public LoginResultVO login(LoginDTO dto, String ip) {
        validateCaptcha(dto.getCaptchaUuid(), dto.getCaptcha());
        int failMax = Math.max(1, luoteProperties.getRedis().getLoginFailMax());
        if (redisUtils.getLoginFailCount(dto.getUsername()) >= failMax) {
            throw new BusinessException(CommonConstant.TOO_MANY_REQUESTS_CODE, ExceptionConstant.LOGIN_LOCKED);
        }
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            redisUtils.recordLoginFail(dto.getUsername());
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.BAD_CREDENTIALS);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ExceptionConstant.ACCOUNT_DISABLED);
        }
        redisUtils.clearLoginFail(dto.getUsername());
        // 新登录先踢旧会话（含 WebSocket），再签发新 Token，保证单点登录闭环
        webSocketSessionRegistry.kickUser(user.getId());
        String role = normalizeRole(user.getRole());
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role);
        redisUtils.saveToken(user.getId(), token);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        updateById(user);
        userCacheService.evict(user.getId());
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        maskPublicUser(userVO);
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUser(userVO);
        return result;
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            // 登出同时作废 HTTP Token 与 WebSocket 会话
            invalidateUserSession(userId);
        }
    }

    @Override
    public void register(RegisterDTO dto) {
        validateCaptcha(dto.getCaptchaUuid(), dto.getCaptcha());
        long count = count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ExceptionConstant.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StrUtil.blankToDefault(dto.getNickname(), dto.getUsername()));
        user.setEmail(dto.getEmail());
        user.setRole(CommonConstant.ROLE_USER);
        user.setStatus(1);
        user.setCreateBy("register");
        save(user);
    }

    @Override
    public IPage<UserVO> pageUsers(UserQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        if (StrUtil.isNotBlank(keyword) && keyword.length() > CommonConstant.MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, CommonConstant.MAX_KEYWORD_LENGTH);
        }
        final String finalKeyword = keyword;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .eq(StrUtil.isNotBlank(query.getRole()), User::getRole, query.getRole());
        if (StrUtil.isNotBlank(finalKeyword)) {
            wrapper.and(w -> w.like(User::getUsername, finalKeyword)
                    .or().like(User::getNickname, finalKeyword)
                    .or().like(User::getEmail, finalKeyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return page(query.toPage(), wrapper)
                .convert(user -> BeanUtil.copyProperties(user, UserVO.class));
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userCacheService.getById(id);
        if (user == null) {
            throw new BusinessException(ExceptionConstant.USER_NOT_FOUND);
        }
        LoginUserVO current = UserContext.get();
        boolean isAdmin = current != null && CommonConstant.ROLE_ADMIN.equals(current.getRole());
        if (!isAdmin && (current == null || !id.equals(current.getId()))) {
            throw new BusinessException(CommonConstant.FORBIDDEN_CODE, ExceptionConstant.ACCESS_DENIED);
        }
        UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
        if (!isAdmin) {
            maskPublicUser(vo);
        }
        return vo;
    }

    @Override
    public void addUser(UserDTO dto) {
        long count = count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ExceptionConstant.USERNAME_EXISTS);
        }
        if (StrUtil.isBlank(dto.getPassword())) {
            throw new BusinessException(ExceptionConstant.PASSWORD_REQUIRED);
        }
        validateRole(dto.getRole());
        User user = BeanUtil.copyProperties(dto, User.class);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(normalizeRole(dto.getRole()));
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        user.setCreateBy(UserContext.get() != null ? UserContext.get().getUsername() : "system");
        save(user);
    }

    @Override
    public void updateUser(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ExceptionConstant.USER_ID_REQUIRED);
        }
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException(ExceptionConstant.USER_NOT_FOUND);
        }
        validateRole(dto.getRole());
        Integer oldStatus = user.getStatus();
        String oldRole = normalizeRole(user.getRole());
        boolean passwordChanged = StrUtil.isNotBlank(dto.getPassword());
        String newRole = normalizeRole(dto.getRole());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus());
        user.setRole(newRole);
        user.setRemark(dto.getRemark());
        if (passwordChanged) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setUpdateBy(UserContext.get() != null ? UserContext.get().getUsername() : "system");
        updateById(user);
        userCacheService.evict(dto.getId());
        // 改密、禁用或角色变更后立即踢下线，避免权限滞后
        boolean disabled = dto.getStatus() != null && dto.getStatus() == 0
                && !Objects.equals(oldStatus, 0);
        boolean roleChanged = !Objects.equals(oldRole, newRole);
        if (passwordChanged || disabled || roleChanged) {
            invalidateUserSession(dto.getId());
        }
    }

    @Override
    public void deleteUser(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ExceptionConstant.USER_NOT_FOUND);
        }
        // 删除后立即作废会话，避免被删用户在 Token 过期前仍可调用接口
        invalidateUserSession(id);
        userCacheService.evict(id);
    }

    /**
     * 作废用户 HTTP Token 并踢掉全部 WebSocket 连接
     */
    private void invalidateUserSession(Long userId) {
        if (userId == null) {
            return;
        }
        redisUtils.removeToken(userId);
        webSocketSessionRegistry.kickUser(userId);
    }

    /**
     * 校验图形验证码
     */
    private void validateCaptcha(String uuid, String captcha) {
        String saved = redisUtils.getAndRemoveCaptcha(uuid);
        if (saved == null || !StrUtil.equalsIgnoreCase(saved, captcha)) {
            throw new BusinessException(ExceptionConstant.CAPTCHA_INVALID);
        }
    }

    /**
     * 校验角色合法性
     */
    private void validateRole(String role) {
        if (StrUtil.isNotBlank(role) && !ALLOWED_ROLES.contains(role)) {
            throw new BusinessException(ExceptionConstant.ROLE_INVALID);
        }
    }

    /**
     * 规范化角色，非法值降级为 USER
     */
    private String normalizeRole(String role) {
        return CommonConstant.ROLE_ADMIN.equals(role) ? CommonConstant.ROLE_ADMIN : CommonConstant.ROLE_USER;
    }

    /**
     * 脱敏公开用户信息（隐藏微信标识等敏感字段）
     */
    private void maskPublicUser(UserVO vo) {
        vo.setWechatOpenid(null);
        vo.setWechatUnionid(null);
        vo.setRemark(null);
    }
}

package cn.zhenxinjian.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.UserStatusEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.common.utils.SpringUtils;
import cn.zhenxinjian.domain.dto.GuestLoginDTO;
import cn.zhenxinjian.domain.dto.WechatLoginDTO;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.vo.GuestLoginResultVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.domain.vo.UserVO;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.GuestDataMigrator;
import cn.zhenxinjian.service.WechatAuthService;
import cn.zhenxinjian.websocket.WebSocketSessionRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * 微信/游客认证 Service 实现
 * 作者: wanglx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatAuthServiceImpl extends ServiceImpl<UserMapper, User> implements WechatAuthService {

    private final org.springframework.beans.factory.ObjectProvider<WxMaService> wxMaServiceProvider;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final UserCacheService userCacheService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVO wechatLogin(WechatLoginDTO dto, String ip) {
        // 1. code2session 换 openid（失败快速返回，不产生半成品用户）
        WxMaJscode2SessionResult session = code2Session(dto.getCode());
        String openid = session.getOpenid();

        // 2. openid 唯一绑定：找/建用户（并发兜底：唯一索引冲突后改查复用）
        User user = findOrCreateWechatUser(openid, session.getUnionid());
        // 非正常状态（冻结/注销）账号拒绝登录（不签发 token）
        if (user.getStatus() == null || UserStatusEnum.of(user.getStatus()) != UserStatusEnum.NORMAL) {
            throw new BusinessException(ExceptionConstant.ACCOUNT_DISABLED);
        }

        // 3. 游客数据迁移（幂等：merged_into 非空即跳过）
        if (StrUtil.isNotBlank(dto.getGuestKey())) {
            migrateGuest(dto.getGuestKey(), user.getId());
        }

        // 4. 签发 token（复用既有单点登录体系）
        webSocketSessionRegistry.kickUser(user.getId());
        String role = CommonConstant.ROLE_ADMIN.equals(user.getRole())
                ? CommonConstant.ROLE_ADMIN : CommonConstant.ROLE_USER;
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role,
                CommonConstant.USER_TYPE_WECHAT, null);
        redisUtils.saveToken(user.getId(), token);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        updateById(user);
        userCacheService.evict(user.getId());

        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.setWechatOpenid(null);
        userVO.setWechatUnionid(null);
        userVO.setRemark(null);
        result.setUser(userVO);
        return result;
    }

    @Override
    public GuestLoginResultVO guestLogin(GuestLoginDTO dto, String ip) {
        User guest = null;
        if (StrUtil.isNotBlank(dto.getGuestKey())) {
            // 带既有 guest_key：命中则复用记录，不重置 3 天起算
            guest = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.getGuestKey())
                    .eq(User::getUserType, CommonConstant.USER_TYPE_GUEST));
            if (guest != null && guest.getMergedInto() != null) {
                // 已合并（含宽限期内完成迁移）：身份作废，签发新游客
                log.info("guestKey 已合并，签发新游客身份: guestKey={}", dto.getGuestKey());
                guest = null;
            }
        }

        if (guest != null) {
            LocalDateTime expireAt = guest.getGuestExpireAt();
            if (expireAt == null || expireAt.plusDays(CommonConstant.GUEST_GRACE_DAYS).isBefore(LocalDateTime.now())) {
                // 到期超 7 天未合并：身份彻底失效，重新起算
                log.info("游客已超宽限期，重新签发: guestId={}", guest.getId());
                guest = createGuest(ip);
            } else if (expireAt.isBefore(LocalDateTime.now())) {
                // 已到期但在宽限期内：拒绝签发游客 token，强制引导授权（数据仍可迁移）
                throw new BusinessException(CommonConstant.GUEST_EXPIRED_CODE, ExceptionConstant.GUEST_EXPIRED);
            }
        } else {
            // 首次进入 / guestKey 无效：生成新游客
            guest = createGuest(ip);
        }

        // 签发游客 token（带 userType/gexp claim）
        webSocketSessionRegistry.kickUser(guest.getId());
        LocalDateTime expireAt = expireOf(guest);
        String token = jwtUtils.generateToken(guest.getId(), guest.getUsername(), CommonConstant.ROLE_USER,
                CommonConstant.USER_TYPE_GUEST,
                Date.from(expireAt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        redisUtils.saveToken(guest.getId(), token);
        guest.setLastLoginTime(LocalDateTime.now());
        guest.setLastLoginIp(ip);
        updateById(guest);

        GuestLoginResultVO result = new GuestLoginResultVO();
        result.setToken(token);
        result.setGuestKey(guest.getUsername());
        UserVO userVO = BeanUtil.copyProperties(guest, UserVO.class);
        userVO.setWechatOpenid(null);
        userVO.setWechatUnionid(null);
        userVO.setRemark(null);
        result.setUser(userVO);
        return result;
    }

    /**
     * code2session 交换 openid
     * <p>第三方错误码只进不出：微信 WxErrorException 的 errcode（40029 code 无效 / 40163 code 已使用等）
     * 仅在此识别并记日志，对前端一律归类为本系统错误码（40101 凭证无效 / 40102 服务不可用），
     * 不透传微信原始码与原始 message。</p>
     *
     * @param code wx.login() 获取的临时凭证
     * @return 含 openid/unionid 的会话结果
     * @throws BusinessException 40101（code 无效或已使用）/ 40102（未配置或微信服务不可达）
     */
    private WxMaJscode2SessionResult code2Session(String code) {
        // 未配置 appid：服务端配置缺失，归类 40102（不暴露内部配置细节）
        WxMaService wxMaService = wxMaServiceProvider.getIfAvailable();
        if (wxMaService == null) {
            log.warn("未配置 zhenxinjian.wechat.appid，微信登录不可用");
            throw new BusinessException(CommonConstant.WECHAT_UNAVAILABLE_CODE, ExceptionConstant.WECHAT_UNAVAILABLE);
        }
        WxMaJscode2SessionResult session;
        try {
            session = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            // 微信侧明确返回业务错误：按 errcode 分类，原始码仅记日志（40029=invalid code / 40163=code been used）
            log.warn("code2session 微信侧错误: errcode={}, errmsg={}", e.getError().getErrorCode(), e.getError().getErrorMsg());
            if (CommonConstant.WECHAT_ERR_CODE_INVALID == e.getError().getErrorCode()) {
                throw new BusinessException(CommonConstant.WECHAT_CODE_INVALID_CODE, ExceptionConstant.WECHAT_CODE_INVALID);
            }
            throw new BusinessException(CommonConstant.WECHAT_UNAVAILABLE_CODE, ExceptionConstant.WECHAT_UNAVAILABLE);
        } catch (Exception e) {
            // 网络超时 / JSON 解析失败等：归类 40102
            log.warn("code2session 调用失败: {}", e.getMessage());
            throw new BusinessException(CommonConstant.WECHAT_UNAVAILABLE_CODE, ExceptionConstant.WECHAT_UNAVAILABLE);
        }
        if (session == null || StrUtil.isBlank(session.getOpenid())) {
            throw new BusinessException(CommonConstant.WECHAT_CODE_INVALID_CODE, ExceptionConstant.WECHAT_CODE_INVALID);
        }
        return session;
    }

    /**
     * openid 找/建用户；并发首次登录依赖 uk_wechat_openid_active 唯一索引兜底
     */
    private User findOrCreateWechatUser(String openid, String unionid) {
        User existing = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getWechatOpenid, openid)
                .eq(User::getUserType, CommonConstant.USER_TYPE_WECHAT));
        if (existing != null) {
            return existing;
        }
        User user = new User();
        user.setWechatOpenid(openid);
        user.setWechatUnionid(unionid);
        user.setUserType(CommonConstant.USER_TYPE_WECHAT);
        // username/password NOT NULL：占位值（游客同策略）
        user.setUsername(CommonConstant.USERNAME_PREFIX_WECHAT + IdUtil.simpleUUID());
        user.setPassword(passwordEncoder.encode(IdUtil.fastSimpleUUID()));
        user.setNickname(CommonConstant.NICKNAME_WECHAT_DEFAULT);
        user.setWechatNickname(CommonConstant.NICKNAME_WECHAT_DEFAULT);
        user.setWechatBindStatus(CommonConstant.WECHAT_BIND_YES);
        user.setWechatBindTime(LocalDateTime.now());
        user.setRole(CommonConstant.ROLE_USER);
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        user.setCreateBy(CommonConstant.CREATE_BY_WECHAT_LOGIN);
        try {
            save(user);
            return user;
        } catch (DuplicateKeyException e) {
            // 并发首次登录：另一请求已建号，改查复用
            log.info("openid 并发建号冲突，改查复用: openid={}", openid);
            User concurrent = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getWechatOpenid, openid)
                    .eq(User::getUserType, CommonConstant.USER_TYPE_WECHAT));
            if (concurrent == null) {
                throw new BusinessException(CommonConstant.WECHAT_OPENID_CONFLICT_CODE,
                        ExceptionConstant.WECHAT_OPENID_CONFLICT);
            }
            return concurrent;
        }
    }

    /**
     * 创建游客记录：guest_key = UUID 作 username，guest_expire_at = now + 3d
     */
    private User createGuest(String ip) {
        User guest = new User();
        // guest_key = 前缀 + UUID，作为客户端持久化的游客标识（复用/迁移均以此命中）
        guest.setUsername(CommonConstant.USERNAME_PREFIX_GUEST + IdUtil.simpleUUID());
        guest.setPassword(passwordEncoder.encode(IdUtil.fastSimpleUUID()));
        guest.setNickname(CommonConstant.NICKNAME_GUEST_DEFAULT);
        guest.setUserType(CommonConstant.USER_TYPE_GUEST);
        // 3 天体验期自签发时刻起算，不复用不延长
        guest.setGuestExpireAt(LocalDateTime.now().plusDays(CommonConstant.GUEST_TRIAL_DAYS));
        guest.setRole(CommonConstant.ROLE_USER);
        guest.setStatus(UserStatusEnum.NORMAL.getCode());
        guest.setLastLoginIp(ip);
        guest.setCreateBy(CommonConstant.CREATE_BY_GUEST_LOGIN);
        save(guest);
        return guest;
    }

    /**
     * 游客迁移：命中有效游客 → 事务内调用全部 Migrator + 用户记录层合并
     */
    private void migrateGuest(String guestKey, Long formalId) {
        User guest = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, guestKey)
                .eq(User::getUserType, CommonConstant.USER_TYPE_GUEST));
        if (guest == null) {
            // guestKey 无效：静默跳过（不影响登录主流程）
            log.info("迁移跳过：guestKey 未命中, guestKey={}", guestKey);
            return;
        }
        if (guest.getMergedInto() != null) {
            // 幂等：已迁移过直接跳过
            log.info("迁移跳过：游客已合并, guestId={} → formalId={}", guest.getId(), guest.getMergedInto());
            return;
        }
        if (guest.getId().equals(formalId)) {
            return;
        }
        // 宽限期内（到期 7 天内）仍可迁移；超期不迁移
        if (guest.getGuestExpireAt() != null
                && guest.getGuestExpireAt().plusDays(CommonConstant.GUEST_GRACE_DAYS).isBefore(LocalDateTime.now())) {
            log.info("迁移跳过：游客已超 7 天宽限期, guestId={}", guest.getId());
            return;
        }
        // 调用全部业务 Migrator（当前阶段仅用户记录层；Change 2/5 各自补齐）
        Map<String, GuestDataMigrator> migrators = SpringUtils.getBeansOfType(GuestDataMigrator.class);
        for (GuestDataMigrator migrator : migrators.values()) {
            migrator.migrate(guest.getId(), formalId);
        }
        // 用户记录层迁移：merged_into 标记 + 软删（幂等判空由上方 mergedInto 保证）
        guest.setMergedInto(formalId);
        updateById(guest);
        removeById(guest.getId());
        userCacheService.evict(guest.getId());
        // 作废游客会话
        redisUtils.removeToken(guest.getId());
        webSocketSessionRegistry.kickUser(guest.getId());
        log.info("游客迁移完成: guestId={} → formalId={}", guest.getId(), formalId);
    }

    /**
     * 游客到期时间（防御：空则按 now+3d 兜底）
     */
    private LocalDateTime expireOf(User guest) {
        return guest.getGuestExpireAt() != null
                ? guest.getGuestExpireAt()
                : LocalDateTime.now().plusDays(CommonConstant.GUEST_TRIAL_DAYS);
    }
}

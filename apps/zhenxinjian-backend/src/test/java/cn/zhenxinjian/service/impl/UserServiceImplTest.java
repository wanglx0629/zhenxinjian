package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.enums.UserStatusEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.dto.LoginDTO;
import cn.zhenxinjian.domain.dto.UserDTO;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.SessionEvictor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户 Service 实现单元测试
 * 作者: wanglx
 */
class UserServiceImplTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private RedisUtils redisUtils;
    private UserCacheService userCacheService;
    private SessionEvictor sessionEvictor;
    private ZhenxinjianProperties zhenxinjianProperties;
    private SensitiveWordFilter sensitiveWordFilter;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);

        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtils = mock(JwtUtils.class);
        redisUtils = mock(RedisUtils.class);
        userCacheService = mock(UserCacheService.class);
        sessionEvictor = mock(SessionEvictor.class);
        zhenxinjianProperties = mock(ZhenxinjianProperties.class);
        sensitiveWordFilter = mock(SensitiveWordFilter.class);

        UserServiceImpl realService = new UserServiceImpl(passwordEncoder, jwtUtils, redisUtils,
                userCacheService, sessionEvictor, zhenxinjianProperties, sensitiveWordFilter);
        // ServiceImpl.baseMapper 是 protected 字段，需反射注入
        ReflectionTestUtils.setField(realService, "baseMapper", userMapper);
        // 用 spy 以便 stub getOne()（绕过 BaseMapper 默认方法问题）
        service = spy(realService);
    }

    /** 场景：用户名不存在 → 登录失败 */
    @Test
    void login_userNotFound_throwsException() {
        LoginDTO dto = loginDto();
        ZhenxinjianProperties.Redis redisProps = new ZhenxinjianProperties.Redis();
        redisProps.setLoginFailMax(3);
        when(zhenxinjianProperties.getRedis()).thenReturn(redisProps);
        when(redisUtils.getLoginFailCount("admin")).thenReturn(0L);
        when(redisUtils.getAndRemoveCaptcha(anyString())).thenReturn("A1B2");
        doReturn(null).when(service).getOne(any(LambdaQueryWrapper.class));

        assertThrows(BusinessException.class, () -> service.login(dto, "127.0.0.1"));
    }

    /** 场景：密码错误 → 登录失败并记录失败次数 */
    @Test
    void login_wrongPassword_throwsException() {
        LoginDTO dto = loginDto();
        ZhenxinjianProperties.Redis redisProps = new ZhenxinjianProperties.Redis();
        redisProps.setLoginFailMax(3);
        when(zhenxinjianProperties.getRedis()).thenReturn(redisProps);
        when(redisUtils.getLoginFailCount("admin")).thenReturn(0L);
        when(redisUtils.getAndRemoveCaptcha(anyString())).thenReturn("A1B2");

        User user = userEntity();
        user.setPassword("encrypted_real");
        doReturn(user).when(service).getOne(any(LambdaQueryWrapper.class));
        when(passwordEncoder.matches("admin123", "encrypted_real")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.login(dto, "127.0.0.1"));
        verify(redisUtils).recordLoginFail("admin");
    }

    /** 场景：登录失败次数超限 → 拒绝 */
    @Test
    void login_tooManyFails_throwsException() {
        LoginDTO dto = loginDto();
        ZhenxinjianProperties.Redis redisProps = new ZhenxinjianProperties.Redis();
        redisProps.setLoginFailMax(3);
        when(zhenxinjianProperties.getRedis()).thenReturn(redisProps);
        when(redisUtils.getLoginFailCount("admin")).thenReturn(3L);

        assertThrows(BusinessException.class, () -> service.login(dto, "127.0.0.1"));
    }

    /** 场景：正常登录 → 返回 token + userVO */
    @Test
    void login_success_returnsToken() {
        LoginDTO dto = loginDto();
        ZhenxinjianProperties.Redis redisProps = new ZhenxinjianProperties.Redis();
        redisProps.setLoginFailMax(3);
        when(zhenxinjianProperties.getRedis()).thenReturn(redisProps);
        when(redisUtils.getLoginFailCount("admin")).thenReturn(0L);
        when(redisUtils.getAndRemoveCaptcha(anyString())).thenReturn("A1B2");

        User user = userEntity();
        user.setPassword("encrypted_real");
        doReturn(user).when(service).getOne(any(LambdaQueryWrapper.class));
        when(passwordEncoder.matches("admin123", "encrypted_real")).thenReturn(true);
        when(jwtUtils.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token-xxx");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        LoginResultVO result = service.login(dto, "127.0.0.1");

        assertNotNull(result);
        assertEquals("jwt-token-xxx", result.getToken());
        assertNotNull(result.getUser());
        verify(redisUtils).clearLoginFail("admin");
        verify(redisUtils).saveToken(anyLong(), anyString());
    }

    /** 场景：登录后账号被禁用 → 拒绝登录 */
    @Test
    void login_disabledAccount_throwsException() {
        LoginDTO dto = loginDto();
        ZhenxinjianProperties.Redis redisProps = new ZhenxinjianProperties.Redis();
        redisProps.setLoginFailMax(3);
        when(zhenxinjianProperties.getRedis()).thenReturn(redisProps);
        when(redisUtils.getLoginFailCount("admin")).thenReturn(0L);
        when(redisUtils.getAndRemoveCaptcha(anyString())).thenReturn("A1B2");

        User user = userEntity();
        user.setPassword("encrypted_real");
        user.setStatus(UserStatusEnum.FROZEN.getCode());
        doReturn(user).when(service).getOne(any(LambdaQueryWrapper.class));
        when(passwordEncoder.matches("admin123", "encrypted_real")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.login(dto, "127.0.0.1"));
    }

    /** 场景：查询用户不存在 → 抛错 */
    @Test
    void getUserById_notFound_throwsException() {
        when(userCacheService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getUserById(999L));
    }

    /** 场景：添加用户密码为空 → 抛错 */
    @Test
    void addUser_blankPassword_throwsException() {
        UserDTO dto = new UserDTO();
        dto.setUsername("newadmin");
        dto.setPassword("");

        assertThrows(BusinessException.class, () -> service.addUser(dto));
    }

    /** 场景：正常添加用户 */
    @Test
    void addUser_success_inserts() {
        UserDTO dto = new UserDTO();
        dto.setUsername("newadmin");
        dto.setPassword("admin123");
        dto.setRole("ADMIN");
        dto.setNickname("管理员");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("admin123")).thenReturn("encrypted");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        service.addUser(dto);

        verify(userMapper).insert(any(User.class));
    }

    /** 场景：更新用户时改密码 → 踢下线 */
    @Test
    void updateUser_passwordChanged_invalidatesSession() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setPassword("newpass");
        dto.setRole("USER");
        dto.setStatus(UserStatusEnum.NORMAL.getCode());

        User user = userEntity();
        user.setPassword("old_encrypted");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("newpass")).thenReturn("new_encrypted");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        service.updateUser(dto);

        verify(redisUtils).removeToken(1L);
        verify(sessionEvictor).evict(1L);
    }

    private LoginDTO loginDto() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");
        dto.setCaptchaUuid("uuid");
        dto.setCaptcha("A1B2");
        return dto;
    }

    private User userEntity() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encrypted_real");
        user.setNickname("管理员");
        user.setRole("ADMIN");
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        return user;
    }
}
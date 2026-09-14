package cn.zhenxinjian.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.UserStatusEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.dto.GuestLoginDTO;
import cn.zhenxinjian.domain.dto.WechatLoginDTO;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.vo.GuestLoginResultVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.GuestMigrationOrchestrator;
import cn.zhenxinjian.service.SessionEvictor;
import cn.zhenxinjian.service.StorageService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.jsonwebtoken.Claims;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信/游客认证 Service 单元测试（无 Spring 容器，Mockito Mock MyBatis-Plus baseMapper）
 * 作者: wanglx
 */
class WechatAuthServiceImplTest {

    private static final String OPENID = "o-test-openid-777";
    private static final String GUEST_KEY = "guest_abc123";

    private UserMapper userMapper;
    private WxMaUserService wxMaUserService;
    private RedisUtils redisUtils;
    private JwtUtils jwtUtils;
    private GuestMigrationOrchestrator guestMigrationOrchestrator;
    private StorageService storageService;
    private WechatAuthServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 初始化 User 实体元数据：无 MyBatis 环境下 LambdaQueryWrapper 的列名解析依赖 TableInfo，
        // 未初始化时 paramNameValuePairs 为空 map，无法按条件值分发 stub（幂等，重复调用自动跳过）
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);

        userMapper = mock(UserMapper.class);
        WxMaService wxMaService = mock(WxMaService.class);
        wxMaUserService = mock(WxMaUserService.class);
        when(wxMaService.getUserService()).thenReturn(wxMaUserService);
        ObjectProvider<WxMaService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(wxMaService);

        ZhenxinjianProperties properties = new ZhenxinjianProperties();
        properties.getJwt().setSecret("zxj-test-jwt-secret-key-32bytes!!");
        properties.getJwt().setExpireMinutes(120);
        properties.getJwt().setRefreshThresholdMinutes(30);
        jwtUtils = new JwtUtils(properties);
        jwtUtils.validateSecret();

        redisUtils = mock(RedisUtils.class);
        UserCacheService userCacheService = mock(UserCacheService.class);
        SessionEvictor sessionEvictor = mock(SessionEvictor.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hashed");

        guestMigrationOrchestrator = mock(GuestMigrationOrchestrator.class);
        storageService = mock(StorageService.class);
        service = new WechatAuthServiceImpl(provider, jwtUtils, redisUtils,
                userCacheService, sessionEvictor, encoder, guestMigrationOrchestrator, storageService);
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    // ==================== code2session 三路 ====================

    @Test
    void wechatLoginSuccessShouldCreateWechatUser() throws WxErrorException {
        stubSession(OPENID, "union-1");
        // openid 未命中 → 建新号
        stubSelectBy(openid -> null, guestKey -> null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });

        LoginResultVO result = service.wechatLogin(dto("good-code", null), null, "127.0.0.1");

        assertNotNull(result.getToken());
        Claims claims = jwtUtils.parseToken(result.getToken());
        assertEquals("100", claims.getSubject());
        assertEquals(CommonConstant.USER_TYPE_WECHAT, jwtUtils.getUserType(claims));
        assertEquals("微信用户", result.getUser().getNickname());
        assertNull(result.getUser().getWechatOpenid());
        verify(redisUtils).saveToken(100L, result.getToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals(OPENID, saved.getWechatOpenid());
        assertEquals(CommonConstant.USER_TYPE_WECHAT, saved.getUserType());
    }

    @Test
    void wechatLoginWithInvalidCodeShouldThrow40101() throws WxErrorException {
        stubSession("", null);
        WechatLoginDTO dto = dto("bad-code", null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.wechatLogin(dto, null, "127.0.0.1"));
        assertEquals(CommonConstant.WECHAT_CODE_INVALID_CODE, ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLoginWithWxErrorShouldThrow40102() throws WxErrorException {
        when(wxMaUserService.getSessionInfo(anyString())).thenThrow(new RuntimeException("connect timeout"));
        WechatLoginDTO dto = dto("any-code", null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.wechatLogin(dto, null, "127.0.0.1"));
        assertEquals(CommonConstant.WECHAT_UNAVAILABLE_CODE, ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLoginWithWxErr40029ShouldThrow40101NotPassThrough() throws WxErrorException {
        // 微信侧 errcode=40029（invalid code）：后端归类为系统码 40101，不透传原始码
        when(wxMaUserService.getSessionInfo(anyString()))
                .thenThrow(new WxErrorException(WxError.fromJson(
                        "{\"errcode\":40029,\"errmsg\":\"invalid code\"}")));
        WechatLoginDTO dto = dto("bad-code", null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.wechatLogin(dto, null, "127.0.0.1"));
        assertEquals(CommonConstant.WECHAT_CODE_INVALID_CODE, ex.getCode());
        assertEquals(ExceptionConstant.WECHAT_CODE_INVALID, ex.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLoginWithWxOtherErrShouldThrow40102NotPassThrough() throws WxErrorException {
        // 微信侧其他 errcode（如 45011 频率限制）：归类系统码 40102，不透传原始码与文案
        when(wxMaUserService.getSessionInfo(anyString()))
                .thenThrow(new WxErrorException(WxError.fromJson(
                        "{\"errcode\":45011,\"errmsg\":\"api minute-quota reach limit\"}")));
        WechatLoginDTO dto = dto("any-code", null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.wechatLogin(dto, null, "127.0.0.1"));
        assertEquals(CommonConstant.WECHAT_UNAVAILABLE_CODE, ex.getCode());
        assertEquals(ExceptionConstant.WECHAT_UNAVAILABLE, ex.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLoginConcurrentInsertShouldReuseExisting() throws WxErrorException {
        stubSession(OPENID, null);
        User concurrent = wechatUser(200L, OPENID);
        // 首查未命中 → insert 冲突 → 复查命中并发记录（selectOne 双参为 getOne 实际链路）
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, concurrent);
        when(userMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null, concurrent);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uk_wechat_openid_active"));

        LoginResultVO result = service.wechatLogin(dto("good-code", null), null, "127.0.0.1");

        assertEquals("200", jwtUtils.parseToken(result.getToken()).getSubject());
    }

    // ==================== 游客签发与复用 ====================

    @Test
    void guestLoginWithoutKeyShouldCreateNewGuest() {
        stubSelectBy(openid -> null, guestKey -> null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(300L);
            return 1;
        });

        GuestLoginResultVO result = service.guestLogin(new GuestLoginDTO(), "127.0.0.1");

        assertNotNull(result.getGuestKey());
        assertTrue(result.getGuestKey().startsWith("guest_"));
        Claims claims = jwtUtils.parseToken(result.getToken());
        assertEquals("300", claims.getSubject());
        assertEquals(CommonConstant.USER_TYPE_GUEST, jwtUtils.getUserType(claims));
        // gexp = now + 3d
        Date gexp = jwtUtils.getGuestExpireAt(claims);
        assertNotNull(gexp);
        assertTrue(gexp.after(new Date()));
        assertTrue(gexp.before(Date.from(LocalDateTime.now().plusDays(CommonConstant.GUEST_TRIAL_DAYS + 1)
                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        verify(redisUtils).saveToken(300L, result.getToken());
    }

    @Test
    void guestLoginWithValidKeyShouldReuseWithoutReset() {
        User existing = guestUser(301L, GUEST_KEY, LocalDateTime.now().plusDays(1));
        stubSelectBy(openid -> null, guestKey -> existing);

        GuestLoginResultVO result = service.guestLogin(guestDto(GUEST_KEY), "127.0.0.1");

        assertEquals(GUEST_KEY, result.getGuestKey());
        verify(userMapper, never()).insert(any(User.class));
        // gexp 沿用既有到期时间（不重置）
        Date gexp = jwtUtils.getGuestExpireAt(jwtUtils.parseToken(result.getToken()));
        Date expected = Date.from(existing.getGuestExpireAt()
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        assertEquals(expected.getTime(), gexp.getTime());
    }

    @Test
    void guestLoginWithMergedKeyShouldIssueNewGuest() {
        User merged = guestUser(302L, GUEST_KEY, LocalDateTime.now().plusDays(1));
        merged.setMergedInto(999L);
        stubSelectBy(openid -> null, guestKey -> merged);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(303L);
            return 1;
        });

        GuestLoginResultVO result = service.guestLogin(guestDto(GUEST_KEY), "127.0.0.1");

        verify(userMapper).insert(any(User.class));
        assertTrue(result.getGuestKey().startsWith("guest_"));
    }

    @Test
    void guestLoginExpiredInGraceShouldThrow40201() {
        // 到期 1 天（3 天体验 + 7 天宽限内）→ 拒绝签发，强制授权
        User expired = guestUser(304L, GUEST_KEY, LocalDateTime.now().minusDays(1));
        stubSelectBy(openid -> null, guestKey -> expired);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.guestLogin(guestDto(GUEST_KEY), "127.0.0.1"));
        assertEquals(CommonConstant.GUEST_EXPIRED_CODE, ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void guestLoginBeyondGraceShouldReissue() {
        // 到期超 7 天宽限 → 重新起算新游客
        User stale = guestUser(305L, GUEST_KEY, LocalDateTime.now().minusDays(CommonConstant.GUEST_GRACE_DAYS + 1));
        stubSelectBy(openid -> null, guestKey -> stale);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(306L);
            return 1;
        });

        GuestLoginResultVO result = service.guestLogin(guestDto(GUEST_KEY), "127.0.0.1");

        verify(userMapper).insert(any(User.class));
        assertTrue(result.getGuestKey().startsWith("guest_"));
    }

    // ==================== 迁移与幂等 ====================

    @Test
    void wechatLoginWithGuestKeyShouldMigrate() throws WxErrorException {
        stubSession(OPENID, null);
        User formal = wechatUser(400L, OPENID);
        User guest = guestUser(401L, GUEST_KEY, LocalDateTime.now().plusDays(1));
        stubSelectBy(openid -> formal, guestKey -> guest);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        // MP 3.5.7 removeById 走 deleteById(Serializable id) 重载
        when(userMapper.deleteById(any(Long.class))).thenReturn(1);

        LoginResultVO result = service.wechatLogin(dto("good-code", GUEST_KEY), null, "127.0.0.1");

        assertEquals("400", jwtUtils.parseToken(result.getToken()).getSubject());
        // 业务数据迁移经编排者统一驱动
        verify(guestMigrationOrchestrator).migrateAll(401L, 400L);
        // 用户记录层迁移：merged_into 标记 + 软删游客 + 作废会话
        // （updateById 调用两次：游客 merged_into 标记 + 正式用户登录信息更新，均属正常）
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(u -> Long.valueOf(400L).equals(u.getMergedInto())));
        ArgumentCaptor<Long> deleteCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userMapper).deleteById(deleteCaptor.capture());
        assertEquals(401L, deleteCaptor.getValue());
        verify(redisUtils).removeToken(401L);
    }

    @Test
    void wechatLoginWithMigratedGuestKeyShouldSkipRepetition() throws WxErrorException {
        stubSession(OPENID, null);
        User formal = wechatUser(400L, OPENID);
        User guest = guestUser(401L, GUEST_KEY, LocalDateTime.now().plusDays(1));
        guest.setMergedInto(400L);
        stubSelectBy(openid -> formal, guestKey -> guest);

        service.wechatLogin(dto("good-code", GUEST_KEY), null, "127.0.0.1");

        // 幂等：不再重复迁移
        verify(userMapper, never()).deleteById(any(Long.class));
        verify(redisUtils, never()).removeToken(401L);
    }

    @Test
    void wechatLoginWithUnknownGuestKeyShouldSilentlySkip() throws WxErrorException {
        stubSession(OPENID, null);
        User formal = wechatUser(400L, OPENID);
        stubSelectBy(openid -> formal, guestKey -> null);

        LoginResultVO result = service.wechatLogin(dto("good-code", "guest_unknown"), null, "127.0.0.1");

        assertNotNull(result.getToken());
        verify(userMapper, never()).deleteById(any(Long.class));
    }

    // ==================== 辅助 ====================

    private interface QueryStub {
        User get(String value);
    }

    /**
     * 按 LambdaQueryWrapper 条件值区分 openid / guestKey 两种查询。
     * 注意：eq 的参数值懒解析，须先调 getSqlSegment() 触发填充 paramNameValuePairs；
     * 列名解析依赖 TableInfo（setUp 已初始化）。
     * 覆盖 selectOne 双参（getOne 实际链路）/ selectOne 单参 / selectList 三条链路
     */
    private void stubSelectBy(QueryStub openidStub, QueryStub guestKeyStub) {
        java.util.function.Function<Wrapper<User>, User> resolver = wrapper -> {
            if (wrapper instanceof LambdaQueryWrapper<User> lqw) {
                lqw.getSqlSegment();
                Collection<Object> values = lqw.getParamNameValuePairs().values();
                if (values.contains(OPENID)) {
                    return openidStub.get(OPENID);
                }
                if (values.contains(GUEST_KEY)) {
                    return guestKeyStub.get(GUEST_KEY);
                }
            }
            return null;
        };
        org.mockito.stubbing.Answer<User> answer = inv -> resolver.apply(inv.getArgument(0));
        when(userMapper.selectOne(any(Wrapper.class))).thenAnswer(answer);
        when(userMapper.selectOne(any(Wrapper.class), anyBoolean())).thenAnswer(answer);
        when(userMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            User user = resolver.apply(inv.getArgument(0));
            return user == null ? java.util.Collections.emptyList() : java.util.List.of(user);
        });
    }

    private void stubSession(String openid, String unionid) throws WxErrorException {
        WxMaJscode2SessionResult session = new WxMaJscode2SessionResult();
        session.setOpenid(openid);
        session.setUnionid(unionid);
        when(wxMaUserService.getSessionInfo(anyString())).thenReturn(session);
    }

    private WechatLoginDTO dto(String code, String guestKey) {
        WechatLoginDTO dto = new WechatLoginDTO();
        dto.setCode(code);
        dto.setGuestKey(guestKey);
        return dto;
    }

    private GuestLoginDTO guestDto(String guestKey) {
        GuestLoginDTO dto = new GuestLoginDTO();
        dto.setGuestKey(guestKey);
        return dto;
    }

    private User wechatUser(Long id, String openid) {
        User user = new User();
        user.setId(id);
        user.setUsername("wx_" + id);
        user.setWechatOpenid(openid);
        user.setUserType(CommonConstant.USER_TYPE_WECHAT);
        user.setRole(CommonConstant.ROLE_USER);
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        return user;
    }

    private User guestUser(Long id, String guestKey, LocalDateTime expireAt) {
        User user = new User();
        user.setId(id);
        user.setUsername(guestKey);
        user.setUserType(CommonConstant.USER_TYPE_GUEST);
        user.setGuestExpireAt(expireAt);
        user.setRole(CommonConstant.ROLE_USER);
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        return user;
    }
}

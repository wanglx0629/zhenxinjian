package cn.zhenxinjian.security;

import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.UserStatusEnum;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.po.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 游客 gexp 过期过滤器单元测试（无 Spring 容器）
 * 作者: wanglx
 */
class JwtAuthenticationFilterGuestTest {

    private JwtUtils jwtUtils;
    private RedisUtils redisUtils;
    private UserCacheService userCacheService;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        ZhenxinjianProperties properties = new ZhenxinjianProperties();
        properties.getJwt().setSecret("zxj-test-jwt-secret-key-32bytes!!");
        properties.getJwt().setExpireMinutes(120);
        properties.getJwt().setRefreshThresholdMinutes(30);
        properties.getSecurity().setPermitUrls(List.of("/auth/login"));
        jwtUtils = new JwtUtils(properties);
        jwtUtils.validateSecret();

        redisUtils = mock(RedisUtils.class);
        userCacheService = mock(UserCacheService.class);
        filter = new JwtAuthenticationFilter(jwtUtils, redisUtils,
                new SecurityJsonWriter(new ObjectMapper()), properties, userCacheService);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guestTokenBeforeExpireShouldPass() throws Exception {
        User guest = dbGuest(1L, LocalDateTime.now().plusDays(2));
        when(userCacheService.getById(1L)).thenReturn(guest);
        String token = jwtUtils.generateToken(1L, "guest_key", CommonConstant.ROLE_USER,
                CommonConstant.USER_TYPE_GUEST, futureDays(2));
        when(redisUtils.getToken(1L)).thenReturn(token);

        MockHttpServletRequest request = request(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
        verify(redisUtils).refreshTokenExpire(1L);
    }

    @Test
    void guestTokenWithExpiredGexpClaimShouldReturn40201() throws Exception {
        // claim 层 gexp 已过期 → 直接 40201（无须查库）
        String token = jwtUtils.generateToken(1L, "guest_key", CommonConstant.ROLE_USER,
                CommonConstant.USER_TYPE_GUEST, pastDays(1));

        MockHttpServletRequest request = request(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        // 40201 非合法 HTTP 状态 → HTTP 200 承载，业务码进 Result body
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":40201"));
        assertTrue(response.getContentAsString().contains("游客体验已到期"));
    }

    @Test
    void guestTokenDbExpiredShouldReturn40201() throws Exception {
        // claim 未过期但 DB guest_expire_at 已过（改库模拟到期）→ 40201
        User guest = dbGuest(2L, LocalDateTime.now().minusDays(1));
        when(userCacheService.getById(2L)).thenReturn(guest);
        String token = jwtUtils.generateToken(2L, "guest_key", CommonConstant.ROLE_USER,
                CommonConstant.USER_TYPE_GUEST, futureDays(2));
        when(redisUtils.getToken(2L)).thenReturn(token);

        MockHttpServletRequest request = request(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":40201"));
        assertTrue(response.getContentAsString().contains("游客体验已到期"));
    }

    @Test
    void illegalRole_fallsBackToUserWithWarn() throws Exception {
        // 库中角色为非法值 → 白名单校验安全降级 USER（非静默改写）
        User member = new User();
        member.setId(3L);
        member.setUsername("member");
        member.setRole("superadmin2");
        member.setStatus(UserStatusEnum.NORMAL.getCode());
        when(userCacheService.getById(3L)).thenReturn(member);
        String token = jwtUtils.generateToken(3L, "member", CommonConstant.ROLE_USER);
        when(redisUtils.getToken(3L)).thenReturn(token);

        MockHttpServletRequest request = request(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/info");
        request.setRequestURI("/api/user/info");
        request.addHeader(CommonConstant.HEADER_AUTH, CommonConstant.TOKEN_PREFIX + token);
        return request;
    }

    private User dbGuest(Long id, LocalDateTime expireAt) {
        User user = new User();
        user.setId(id);
        user.setUsername("guest_key");
        user.setUserType(CommonConstant.USER_TYPE_GUEST);
        user.setGuestExpireAt(expireAt);
        user.setRole(CommonConstant.ROLE_USER);
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        return user;
    }

    private Date futureDays(long days) {
        return Date.from(LocalDateTime.now().plusDays(days)
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    private Date pastDays(long days) {
        return Date.from(LocalDateTime.now().minusDays(days)
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
}
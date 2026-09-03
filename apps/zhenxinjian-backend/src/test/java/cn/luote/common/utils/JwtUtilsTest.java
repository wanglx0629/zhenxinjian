package cn.zhenxinjian.common.utils;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtUtils 单元测试（无 Spring 容器）
 * 作者: luote (luote) - https://luote996.cn
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        ZhenxinjianProperties properties = new ZhenxinjianProperties();
        properties.getJwt().setSecret("luote-test-jwt-secret-key-32bytes!!");
        properties.getJwt().setExpireMinutes(120);
        properties.getJwt().setRefreshThresholdMinutes(30);
        jwtUtils = new JwtUtils(properties);
        jwtUtils.validateSecret();
    }

    @Test
    void generateAndParseToken() {
        String token = jwtUtils.generateToken(1L, "admin", "ADMIN");
        assertNotNull(token);
        Claims claims = jwtUtils.parseToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("admin", claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertFalse(jwtUtils.isNearExpire(claims));
    }

    @Test
    void refreshTokenShouldKeepSubject() {
        String token = jwtUtils.generateToken(2L, "user", "USER");
        Claims claims = jwtUtils.parseToken(token);
        String refreshed = jwtUtils.refreshToken(claims);
        Claims next = jwtUtils.parseToken(refreshed);
        assertEquals("2", next.getSubject());
        assertEquals("user", next.get("username", String.class));
        assertTrue(refreshed.length() > 20);
    }

    @Test
    void parseInvalidTokenShouldThrowUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtils.parseToken("not-a-jwt"));
        assertEquals(CommonConstant.UNAUTHORIZED_CODE, ex.getCode());
        assertEquals(ExceptionConstant.TOKEN_INVALID, ex.getMessage());
    }
}

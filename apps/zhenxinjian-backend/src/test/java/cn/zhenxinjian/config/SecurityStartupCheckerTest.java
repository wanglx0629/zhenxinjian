package cn.zhenxinjian.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 启动安全自检单元测试（JWT 内容门禁：缺失/过短/弱特征任何环境拒启；CORS 保持 prod 强制）
 * 作者: wanglx
 */
class SecurityStartupCheckerTest {

    /** 32 字节强密钥（不含弱特征） */
    private static final String STRONG_SECRET = "zxj-test-jwt-secret-key-32bytes!!";

    private ZhenxinjianProperties properties;
    private Environment environment;
    private SecurityStartupChecker checker;

    @BeforeEach
    void setUp() {
        properties = new ZhenxinjianProperties();
        properties.getJwt().setSecret(STRONG_SECRET);
        properties.getSecurity().setCorsAllowedOrigins(List.of("https://admin.example.com"));
        environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        checker = new SecurityStartupChecker(properties, environment);
    }

    /** 场景：密钥空白（占位符未注入）→ 拒启 */
    @Test
    void blankSecret_rejected() {
        properties.getJwt().setSecret("");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> checker.run(null));
        assertTrue(e.getMessage().contains("JWT_SECRET"));
    }

    /** 场景：密钥 UTF-8 字节长度不足 32 → 拒启 */
    @Test
    void shortSecret_rejected() {
        properties.getJwt().setSecret("short-key");
        assertThrows(IllegalStateException.class, () -> checker.run(null));
    }

    /** 场景：历史弱默认密钥 → 拒启（含 change-in 特征） */
    @Test
    void legacyWeakDefault_rejected() {
        properties.getJwt().setSecret("zhenxinjian-jwt-secret-key-change-in-production-wanglx");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> checker.run(null));
        assertTrue(e.getMessage().contains("change-in"));
    }

    /** 场景：弱特征变体（自起名但含 change-in）→ 拒启 */
    @Test
    void weakFeatureVariant_rejected() {
        properties.getJwt().setSecret("my-super-secret-change-in-prod-2026!!");
        assertThrows(IllegalStateException.class, () -> checker.run(null));
    }

    /** 场景：弱内容 + 非 prod 命名 profile（production 拼写差异）→ 同样拒启（内容门禁不认 profile 名） */
    @Test
    void weakSecret_nonProdProfileName_stillRejected() {
        properties.getJwt().setSecret("zhenxinjian-jwt-secret-key-change-in-production-wanglx");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});
        assertThrows(IllegalStateException.class, () -> checker.run(null));
    }

    /** 场景：强密钥无 profile → 放行 */
    @Test
    void strongSecret_pass() {
        assertDoesNotThrow(() -> checker.run(null));
    }

    /** 场景：prod + CORS * → 拒启（CORS 门禁保持 prod 强制） */
    @Test
    void prodCorsStar_rejected() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        properties.getSecurity().setCorsAllowedOrigins(List.of("*"));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> checker.run(null));
        assertTrue(e.getMessage().contains("CORS_ORIGINS"));
    }

    /** 场景：非 prod + CORS * → 放行（仅告警，本地开发允许） */
    @Test
    void nonProdCorsStar_pass() {
        properties.getSecurity().setCorsAllowedOrigins(List.of("*"));
        assertDoesNotThrow(() -> checker.run(null));
    }
}

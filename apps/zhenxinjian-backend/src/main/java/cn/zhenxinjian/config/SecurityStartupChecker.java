package cn.zhenxinjian.config;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.utils.CorsOrigins;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 启动安全自检：JWT 密钥内容门禁（任何环境缺失/过短/含弱特征即拒启）+ 生产 CORS 强制
 * 作者: wanglx
 *
 * 口径：密钥检测内容本身不认 profile 名——非 prod 命名（production/pre 拼写差异）带弱密钥同样拒启，
 * 杜绝"误配环境即弱密钥生产化"；本地开发密钥写入 gitignored 的 application-dev.yml 或注入 JWT_SECRET。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityStartupChecker implements ApplicationRunner {

    /** 弱密钥特征标记（命中即拒，覆盖历史默认 zhenxinjian-jwt-secret-key-change-in-production-wanglx 及其变体） */
    private static final String WEAK_SECRET_FEATURE = "change-in";

    private final ZhenxinjianProperties zhenxinjianProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        checkJwtSecret();
        checkCors(isProd());
    }

    /**
     * 校验 JWT 密钥内容（按 UTF-8 字节长度，与 JwtUtils 一致；任何环境缺失/过短/含弱特征一律拒启）
     */
    private void checkJwtSecret() {
        String secret = zhenxinjianProperties.getJwt().getSecret();
        int byteLen = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (StrUtil.isBlank(secret) || byteLen < 32) {
            throw new IllegalStateException(
                    "JWT secret 未配置或 UTF-8 字节长度不足 32，请通过 JWT_SECRET 注入"
                            + "（本地开发可写入 gitignored 的 application-dev.yml）");
        }
        if (secret.contains(WEAK_SECRET_FEATURE)) {
            throw new IllegalStateException(
                    "JWT secret 含弱默认特征（" + WEAK_SECRET_FEATURE + "），禁止使用，请设置强随机 JWT_SECRET");
        }
    }

    /**
     * 生产环境要求显式 CORS，禁止 *
     */
    private void checkCors(boolean prod) {
        List<String> resolved = CorsOrigins.parse(zhenxinjianProperties.getSecurity().getCorsAllowedOrigins());
        boolean hasStar = resolved.stream().anyMatch("*"::equals);
        boolean empty = resolved.stream().noneMatch(StrUtil::isNotBlank);
        if (prod && (empty || hasStar)) {
            throw new IllegalStateException("生产环境必须配置非空且非 * 的 CORS_ORIGINS");
        }
        if (!prod && hasStar) {
            log.warn("[security] CORS 配置了 *，与 allowCredentials 冲突，请改为明确域名");
        }
    }

    private boolean isProd() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}

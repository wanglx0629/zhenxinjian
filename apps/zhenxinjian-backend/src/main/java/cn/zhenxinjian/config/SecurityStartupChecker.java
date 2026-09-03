package cn.zhenxinjian.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 启动安全自检：生产环境强制校验 JWT / CORS
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityStartupChecker implements ApplicationRunner {

    private static final String WEAK_DEFAULT = "zhenxinjian-jwt-secret-key-change-in-production-luote996-cn";

    private final ZhenxinjianProperties luoteProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = isProd();
        checkJwtSecret(prod);
        checkCors(prod);
    }

    /**
     * 校验 JWT 密钥（按 UTF-8 字节长度，与 JwtUtils 一致）
     */
    private void checkJwtSecret(boolean prod) {
        String secret = luoteProperties.getJwt().getSecret();
        int byteLen = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (StrUtil.isBlank(secret) || byteLen < 32) {
            String msg = "JWT secret 未配置或 UTF-8 字节长度不足 32，请通过 JWT_SECRET 注入";
            if (prod) {
                throw new IllegalStateException(msg);
            }
            log.warn("[security] {}", msg);
            return;
        }
        if (prod && WEAK_DEFAULT.equals(secret)) {
            throw new IllegalStateException("生产环境禁止使用默认 JWT secret，请设置强随机 JWT_SECRET");
        }
        if (!prod && WEAK_DEFAULT.equals(secret)) {
            log.warn("[security] 当前使用默认 JWT secret，仅适合本地开发，上线前务必更换");
        }
    }

    /**
     * 生产环境要求显式 CORS，禁止 *
     */
    private void checkCors(boolean prod) {
        List<String> origins = luoteProperties.getSecurity().getCorsAllowedOrigins();
        List<String> resolved = CollUtil.isEmpty(origins) ? List.of() : origins;
        if (resolved.size() == 1 && StrUtil.contains(resolved.get(0), ',')) {
            resolved = StrUtil.splitTrim(resolved.get(0), ',');
        }
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

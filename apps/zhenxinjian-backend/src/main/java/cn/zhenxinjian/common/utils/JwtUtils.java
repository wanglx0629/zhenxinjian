package cn.zhenxinjian.common.utils;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 作者: luote (luote) - https://luote996.cn
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final ZhenxinjianProperties luoteProperties;

    /**
     * 启动时校验 JWT 密钥长度（HS256 至少 32 字节）
     */
    @PostConstruct
    public void validateSecret() {
        String secret = luoteProperties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT密钥长度不足32字节，请配置 zhenxinjian.jwt.secret");
        }
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username, String role) {
        long expireMs = luoteProperties.getJwt().getExpireMinutes() * 60 * 1000L;
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.TOKEN_INVALID);
        }
    }

    /**
     * 判断 Token 是否即将过期（用于续期）
     */
    public boolean isNearExpire(Claims claims) {
        long thresholdMs = luoteProperties.getJwt().getRefreshThresholdMinutes() * 60 * 1000L;
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis() < thresholdMs;
    }

    /**
     * 续期 Token
     */
    public String refreshToken(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        return generateToken(userId, username, role);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(luoteProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}

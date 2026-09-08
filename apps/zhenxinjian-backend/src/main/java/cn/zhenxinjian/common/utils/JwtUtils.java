package cn.zhenxinjian.common.utils;

import cn.hutool.core.util.StrUtil;
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
 * 作者: wanglx
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final ZhenxinjianProperties zhenxinjianProperties;

    /**
     * 启动时校验 JWT 密钥长度（HS256 至少 32 字节）
     */
    @PostConstruct
    public void validateSecret() {
        String secret = zhenxinjianProperties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT密钥长度不足32字节，请配置 zhenxinjian.jwt.secret");
        }
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null, null);
    }

    /**
     * 生成 Token（支持游客身份）
     *
     * @param userType 用户类型（WECHAT/GUEST），null 视为 WECHAT
     * @param guestExpireAt 游客到期时间（仅 GUEST 时写入 gexp claim）
     */
    public String generateToken(Long userId, String username, String role, String userType, Date guestExpireAt) {
        long expireMs = zhenxinjianProperties.getJwt().getExpireMinutes() * 60 * 1000L;
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);
        claims.put("userType", StrUtil.blankToDefault(userType, CommonConstant.USER_TYPE_WECHAT));
        if (guestExpireAt != null) {
            claims.put("gexp", guestExpireAt.getTime());
        }
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
        long thresholdMs = zhenxinjianProperties.getJwt().getRefreshThresholdMinutes() * 60 * 1000L;
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
        String userType = claims.get("userType", String.class);
        Date gexp = getGuestExpireAt(claims);
        return generateToken(userId, username, role, userType, gexp);
    }

    /**
     * 读取用户类型 claim（缺省视为 WECHAT）
     */
    public String getUserType(Claims claims) {
        String userType = claims.get("userType", String.class);
        return StrUtil.blankToDefault(userType, CommonConstant.USER_TYPE_WECHAT);
    }

    /**
     * 读取游客到期时间 claim（gexp，毫秒时间戳；无则 null）
     */
    public Date getGuestExpireAt(Claims claims) {
        Long gexp = claims.get("gexp", Long.class);
        return gexp == null ? null : new Date(gexp);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(zhenxinjianProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}

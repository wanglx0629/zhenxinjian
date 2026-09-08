package cn.zhenxinjian.common.utils;

import cn.zhenxinjian.config.ZhenxinjianProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 作者: wanglx
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final StringRedisTemplate redisTemplate;
    private final ZhenxinjianProperties zhenxinjianProperties;

    /**
     * 存储 Token
     */
    public void saveToken(Long userId, String token) {
        String key = zhenxinjianProperties.getRedis().getTokenPrefix() + userId;
        long minutes = zhenxinjianProperties.getJwt().getExpireMinutes();
        redisTemplate.opsForValue().set(key, token, minutes, TimeUnit.MINUTES);
    }

    /**
     * 获取 Token
     */
    public String getToken(Long userId) {
        String key = zhenxinjianProperties.getRedis().getTokenPrefix() + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 Token（登出）
     */
    public void removeToken(Long userId) {
        String key = zhenxinjianProperties.getRedis().getTokenPrefix() + userId;
        redisTemplate.delete(key);
    }

    /**
     * 续期 Token
     */
    public void refreshTokenExpire(Long userId) {
        String key = zhenxinjianProperties.getRedis().getTokenPrefix() + userId;
        long minutes = zhenxinjianProperties.getJwt().getExpireMinutes();
        redisTemplate.expire(key, minutes, TimeUnit.MINUTES);
    }

    /**
     * 存储验证码
     */
    public void saveCaptcha(String uuid, String code) {
        String key = zhenxinjianProperties.getRedis().getCaptchaPrefix() + uuid;
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 获取并删除验证码
     */
    public String getAndRemoveCaptcha(String uuid) {
        String key = zhenxinjianProperties.getRedis().getCaptchaPrefix() + uuid;
        String code = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return code;
    }

    /**
     * 记录登录失败次数
     */
    public void recordLoginFail(String username) {
        String key = zhenxinjianProperties.getRedis().getLoginFailPrefix() + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            long ttlMinutes = Math.max(1L, zhenxinjianProperties.getRedis().getLoginFailTtlMinutes());
            redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);
        }
    }

    /**
     * 获取登录失败次数
     */
    public long getLoginFailCount(String username) {
        String key = zhenxinjianProperties.getRedis().getLoginFailPrefix() + username;
        String count = redisTemplate.opsForValue().get(key);
        return count == null ? 0L : Long.parseLong(count);
    }

    /**
     * 清除登录失败记录
     */
    public void clearLoginFail(String username) {
        String key = zhenxinjianProperties.getRedis().getLoginFailPrefix() + username;
        redisTemplate.delete(key);
    }
}

package cn.zhenxinjian.it;

import cn.zhenxinjian.common.utils.RedisUtils;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 集成测试专用 Mock Bean 配置 — 隔离 Redis / MinIO / OSS / 微信等外部依赖
 * 作者: wanglx
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public RedisUtils redisUtils() {
        return Mockito.mock(RedisUtils.class);
    }
}
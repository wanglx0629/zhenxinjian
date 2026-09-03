package cn.zhenxinjian.common.cache;

import cn.zhenxinjian.common.constant.CacheConstant;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.mapper.UserMapper;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户缓存服务（JetCache）
 * 穿透：cacheNullValue + CachePenetrationProtect
 * 击穿：CachePenetrationProtect 单线程回源
 * 雪崩：随机过期时间
 * 热点：CacheType.BOTH 本地 + Redis
 * 作者: luote (luote) - https://luote996.cn
 */
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final UserMapper userMapper;
    private final CacheValueGuard cacheValueGuard;

    /**
     * 按 ID 查询用户（带缓存）
     */
    @Cached(
            name = CacheConstant.USER,
            key = "#id",
            cacheType = CacheType.BOTH,
            syncLocal = true,
            cacheNullValue = true,
            expire = CacheConstant.DEFAULT_REMOTE_EXPIRE_SECONDS,
            localExpire = CacheConstant.DEFAULT_LOCAL_EXPIRE_SECONDS
    )
    @CachePenetrationProtect
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        cacheValueGuard.check(user);
        return user;
    }

    /**
     * 删除用户缓存
     */
    @CacheInvalidate(name = CacheConstant.USER, key = "#id")
    public void evict(Long id) {
        // JetCache 注解驱动失效

    }

    /**
     * 计算带随机抖动的过期时间（秒），供业务自定义缓存使用
     */
    public static int randomExpireSeconds(int baseSeconds) {
        return baseSeconds + ThreadLocalRandom.current().nextInt(CacheConstant.EXPIRE_JITTER_SECONDS + 1);
    }
}

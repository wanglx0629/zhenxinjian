package cn.zhenxinjian.common.constant;

/**
 * 缓存常量
 * 作者: wanglx
 */
public final class CacheConstant {

    private CacheConstant() {
    }

    /** JetCache 缓存区域：用户信息 */
    public static final String USER = "user";

    /** 空值占位缓存过期时间（秒），防穿透 */
    public static final int NULL_CACHE_EXPIRE_SECONDS = 60;

    /** 默认远程缓存过期时间（秒） */
    public static final int DEFAULT_REMOTE_EXPIRE_SECONDS = 3600;

    /** 默认本地缓存过期时间（秒），热点 key 就近读取 */
    public static final int DEFAULT_LOCAL_EXPIRE_SECONDS = 300;

    /** 过期时间随机抖动上限（秒），防雪崩 */
    public static final int EXPIRE_JITTER_SECONDS = 600;
}

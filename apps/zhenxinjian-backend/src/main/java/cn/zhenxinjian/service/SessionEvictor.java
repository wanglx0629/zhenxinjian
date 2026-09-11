package cn.zhenxinjian.service;

/**
 * 用户会话驱逐能力抽象（业务与长连接基建解耦的端口）
 * 作者: wanglx
 */
public interface SessionEvictor {

    /**
     * 驱逐指定用户的全部活跃长连接会话
     * <p>HTTP 会话不在此列：JWT Token 由 Redis 令牌体系单独作废</p>
     *
     * @param userId 用户ID
     */
    void evict(Long userId);
}

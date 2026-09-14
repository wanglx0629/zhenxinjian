package cn.zhenxinjian.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * NoopSessionEvictor 单元测试（空实现语义验证）
 * 作者: wanglx
 */
class NoopSessionEvictorTest {

    /** 场景：任意 userId 调用均不抛异常 */
    @Test
    void evict_anyUserId_doesNotThrow() {
        NoopSessionEvictor evictor = new NoopSessionEvictor();

        assertDoesNotThrow(() -> evictor.evict(1L));
        assertDoesNotThrow(() -> evictor.evict(null));
        assertDoesNotThrow(() -> evictor.evict(999999L));
    }
}
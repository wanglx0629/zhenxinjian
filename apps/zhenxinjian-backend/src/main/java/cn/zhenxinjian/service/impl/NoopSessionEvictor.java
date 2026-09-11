package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.service.SessionEvictor;
import org.springframework.stereotype.Component;

/**
 * 会话驱逐空实现：当前无长连接基建（WebSocket 演示板已剥离），保留调用点语义
 * 作者: wanglx
 */
@Component
public class NoopSessionEvictor implements SessionEvictor {

    @Override
    public void evict(Long userId) {
        // 无长连接基建，无需驱逐；HTTP Token 由 RedisUtils.removeToken 作废
    }
}

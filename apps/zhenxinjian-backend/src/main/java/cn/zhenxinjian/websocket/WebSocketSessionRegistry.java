package cn.zhenxinjian.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 会话注册表：支持按用户踢下线与连接数控制
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public void register(Long userId, WebSocketSession session) {
        sessions.put(session.getId(), session);
        if (userId != null) {
            userSessions.computeIfAbsent(userId, id -> new CopyOnWriteArraySet<>()).add(session.getId());
        }
    }

    /**
     * 移除会话
     */
    public void unregister(WebSocketSession session) {
        sessions.remove(session.getId());
        Object userIdAttr = session.getAttributes().get(DemoWebSocketInterceptor.ATTR_USER_ID);
        if (userIdAttr instanceof Long userId) {
            Set<String> set = userSessions.get(userId);
            if (set != null) {
                set.remove(session.getId());
                if (set.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
    }

    /**
     * 当前在线会话数
     */
    public int size() {
        return sessions.size();
    }

    /**
     * 指定用户当前连接数
     */
    public int countByUser(Long userId) {
        Set<String> set = userSessions.get(userId);
        return set == null ? 0 : set.size();
    }

    /**
     * 遍历在线会话
     */
    public Iterable<WebSocketSession> all() {
        return sessions.values();
    }

    /**
     * 踢下指定用户的全部 WebSocket 连接
     */
    public void kickUser(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> set = userSessions.remove(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        for (String sessionId : set) {
            WebSocketSession session = sessions.remove(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.close(CloseStatus.NORMAL.withReason("账号会话已失效"));
                } catch (IOException e) {
                    log.warn("[ws] 踢下线失败: sessionId={}, err={}", sessionId, e.getMessage());
                }
            }
        }
        log.info("[ws] 已踢下线用户全部连接: userId={}, count={}", userId, set.size());
    }
}

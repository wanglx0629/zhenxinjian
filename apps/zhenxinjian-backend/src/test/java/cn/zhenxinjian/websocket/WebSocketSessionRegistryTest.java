package cn.zhenxinjian.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WebSocketSessionRegistry 单元测试（手写 Session stub，避免高版本 JDK 下 Mockito 插桩失败）。
 * 作者: wanglx
 */
class WebSocketSessionRegistryTest {

    private WebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry();
    }

    @Test
    void registerAndCountByUser() {
        StubSession s1 = new StubSession("s1", 1L);
        StubSession s2 = new StubSession("s2", 1L);
        registry.register(1L, s1);
        registry.register(1L, s2);
        assertEquals(2, registry.countByUser(1L));
        registry.unregister(s1);
        assertEquals(1, registry.countByUser(1L));
    }

    @Test
    void kickUserShouldCloseOpenSession() {
        StubSession session = new StubSession("kick-1", 9L);
        registry.register(9L, session);
        registry.kickUser(9L);
        assertTrue(session.closed.get());
        assertEquals(0, registry.countByUser(9L));
    }

    @Test
    void kickUserNullShouldDoNothing() {
        StubSession session = new StubSession("keep-1", 3L);
        registry.register(3L, session);
        registry.kickUser(null);
        assertFalse(session.closed.get());
        assertEquals(1, registry.countByUser(3L));
    }

    /**
     * 最小可用 WebSocketSession 实现，仅覆盖注册表用到的方法。
     */
    private static final class StubSession implements WebSocketSession {

        private final String id;
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final AtomicBoolean open = new AtomicBoolean(true);
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private StubSession(String id, Long userId) {
            this.id = id;
            // unregister 从 attributes 读取用户 ID
            this.attributes.put(DemoWebSocketInterceptor.ATTR_USER_ID, userId);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return null;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return new HttpHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) {
        }

        @Override
        public boolean isOpen() {
            return open.get();
        }

        @Override
        public void close() {
            open.set(false);
            closed.set(true);
        }

        @Override
        public void close(CloseStatus status) throws IOException {
            open.set(false);
            closed.set(true);
        }
    }
}

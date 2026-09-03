package cn.zhenxinjian.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.websocket.DemoWebSocketHandler;
import cn.zhenxinjian.websocket.DemoWebSocketInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.List;

/**
 * WebSocket 配置（Demo 板子）
 * 路径 / 开关 / Origin / 限流均由 zhenxinjian.websocket.* 管控
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zhenxinjian.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig implements WebSocketConfigurer {

    private final DemoWebSocketHandler demoWebSocketHandler;
    private final DemoWebSocketInterceptor demoWebSocketInterceptor;
    private final ZhenxinjianProperties luoteProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        ZhenxinjianProperties.Websocket ws = luoteProperties.getWebsocket();
        String path = StrUtil.blankToDefault(ws.getPath(), "/ws/demo");
        if (!StrUtil.startWith(path, "/")) {
            path = "/" + path;
        }
        registry.addHandler(demoWebSocketHandler, path)
                .addInterceptors(demoWebSocketInterceptor)
                .setAllowedOrigins(resolveOrigins());
        log.info("[ws] 已注册端点 path={}, maxSessions={}, maxPerUser={}, maxMessageBytes={}",
                path, ws.getMaxSessions(), ws.getMaxSessionsPerUser(), ws.getMaxMessageBytes());
    }

    /**
     * 限制单帧 / 会话缓冲，防止大包打满内存
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        int maxBytes = Math.max(1024, luoteProperties.getWebsocket().getMaxMessageBytes());
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(maxBytes);
        container.setMaxBinaryMessageBufferSize(maxBytes);
        return container;
    }

    /**
     * Origin：优先 zhenxinjian.websocket.allowed-origins，否则复用 CORS 白名单（禁止 *）
     */
    private String[] resolveOrigins() {
        List<String> origins = luoteProperties.getWebsocket().getAllowedOrigins();
        if (CollUtil.isEmpty(origins)) {
            origins = luoteProperties.getSecurity().getCorsAllowedOrigins();
        }
        List<String> resolved;
        if (CollUtil.isEmpty(origins)) {
            resolved = List.of(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://localhost:5174",
                    "http://127.0.0.1:5174");
        } else if (origins.size() == 1 && StrUtil.contains(origins.get(0), ',')) {
            resolved = StrUtil.splitTrim(origins.get(0), ',');
        } else {
            resolved = origins;
        }
        String[] allowed = resolved.stream()
                .filter(o -> StrUtil.isNotBlank(o) && !"*".equals(o))
                .toArray(String[]::new);
        if (allowed.length == 0) {
            return new String[]{
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://localhost:5174",
                    "http://127.0.0.1:5174"
            };
        }
        return allowed;
    }
}

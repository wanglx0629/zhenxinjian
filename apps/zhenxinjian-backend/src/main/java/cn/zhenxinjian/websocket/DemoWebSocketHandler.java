package cn.zhenxinjian.websocket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket Demo Handler：广播聊天板子（含消息长度限制与会话注册）
 * 协议（JSON 文本）：
 * 客户端发送 {"type":"chat","content":"你好"} 或 {"type":"ping"}
 * 服务端推送 {"type":"chat|system|pong","from":"...","content":"...","time":"..."}
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoWebSocketHandler extends TextWebSocketHandler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final WebSocketSessionRegistry sessionRegistry;
    private final ZhenxinjianProperties zhenxinjianProperties;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userIdAttr = session.getAttributes().get(DemoWebSocketInterceptor.ATTR_USER_ID);
        Long userId = userIdAttr instanceof Long id ? id : null;
        sessionRegistry.register(userId, session);
        String username = resolveUsername(session);
        log.info("[ws] 连接建立: sessionId={}, user={}", session.getId(), username);
        broadcast(buildMessage("system", "系统", username + " 加入了房间（在线 " + sessionRegistry.size() + " 人）"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = StrUtil.trim(message.getPayload());
        if (StrUtil.isBlank(payload)) {
            return;
        }
        int maxBytes = zhenxinjianProperties.getWebsocket().getMaxMessageBytes();
        if (maxBytes > 0 && utf8Length(payload) > maxBytes) {
            session.sendMessage(new TextMessage(buildMessage("system", "系统", "消息过长，已拒绝")));
            return;
        }
        String username = resolveUsername(session);
        JSONObject req;
        try {
            req = JSONUtil.parseObj(payload);
        } catch (JSONException e) {
            // 兼容纯文本：当成 chat，并做内容清洗
            broadcast(buildMessage("chat", username, sanitizeContent(payload)));
            return;
        }
        String type = StrUtil.blankToDefault(req.getStr("type"), "chat");
        if ("ping".equalsIgnoreCase(type)) {
            session.sendMessage(new TextMessage(buildMessage("pong", "系统", "pong")));
            return;
        }
        String content = sanitizeContent(StrUtil.blankToDefault(req.getStr("content"), ""));
        if (StrUtil.isBlank(content)) {
            return;
        }
        broadcast(buildMessage("chat", username, content));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionRegistry.unregister(session);
        String username = resolveUsername(session);
        log.info("[ws] 连接关闭: sessionId={}, user={}, status={}", session.getId(), username, status);
        // 会话失效踢下线不广播，避免刷屏
        if (status != null && StrUtil.contains(status.getReason(), "会话已失效")) {
            return;
        }
        broadcast(buildMessage("system", "系统", username + " 离开了房间（在线 " + sessionRegistry.size() + " 人）"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("[ws] 传输异常: sessionId={}, err={}", session.getId(), exception.getMessage());
        sessionRegistry.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 广播给所有在线会话
     */
    private void broadcast(String json) {
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessionRegistry.all()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("[ws] 发送失败: sessionId={}, err={}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 组装统一消息 JSON
     */
    private String buildMessage(String type, String from, String content) {
        JSONObject json = new JSONObject();
        json.set("type", type);
        json.set("from", from);
        json.set("content", content);
        json.set("time", LocalDateTime.now().format(TIME_FMT));
        json.set("online", sessionRegistry.size());
        return json.toString();
    }

    /**
     * 清洗聊天内容，降低 XSS / 控制字符风险（板子级防护）
     */
    private String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }
        String cleaned = content.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        int maxBytes = zhenxinjianProperties.getWebsocket().getMaxMessageBytes();
        if (maxBytes > 0) {
            cleaned = truncateUtf8(cleaned, maxBytes);
        }
        return cleaned.trim();
    }

    /**
     * 计算 UTF-8 字节长度
     */
    private int utf8Length(String text) {
        return text == null ? 0 : text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    /**
     * 按 UTF-8 字节上限截断，避免多字节字符被拦腰切断
     */
    private String truncateUtf8(String text, int maxBytes) {
        if (utf8Length(text) <= maxBytes) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int len = utf8Length(ch);
            if (used + len > maxBytes) {
                break;
            }
            sb.append(ch);
            used += len;
        }
        return sb.toString();
    }

    /**
     * 从握手属性读取用户名
     */
    private String resolveUsername(WebSocketSession session) {
        Object username = session.getAttributes().get(DemoWebSocketInterceptor.ATTR_USERNAME);
        return username == null ? "匿名" : String.valueOf(username);
    }
}

package cn.zhenxinjian.websocket;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手拦截：优先 Sec-WebSocket-Protocol，回退 Authorization Bearer
 * 禁止 query token（避免日志/Referer 泄露）
 * H5：new WebSocket(url, [protocol, jwt])
 * 微信小程序等：header Authorization: Bearer jwt
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoWebSocketInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";
    private static final String HEADER_SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    private static final String DEFAULT_PROTOCOL = "bearer";

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final ZhenxinjianProperties luoteProperties;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        String token = resolveToken(request, response);
        if (StrUtil.isBlank(token)) {
            log.warn("[ws] 握手失败：缺少 Token（Protocol 或 Authorization）");
            return false;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String cached = redisUtils.getToken(userId);
            if (cached == null || !StrUtil.equals(cached, token)) {
                log.warn("[ws] 握手失败：Token 无效或已登出, userId={}", userId);
                return false;
            }
            int maxSessions = luoteProperties.getWebsocket().getMaxSessions();
            if (maxSessions > 0 && sessionRegistry.size() >= maxSessions) {
                log.warn("[ws] 握手失败：全局限流已满, max={}", maxSessions);
                return false;
            }
            int maxPerUser = luoteProperties.getWebsocket().getMaxSessionsPerUser();
            if (maxPerUser > 0 && sessionRegistry.countByUser(userId) >= maxPerUser) {
                log.warn("[ws] 握手失败：用户连接数已满, userId={}, max={}", userId, maxPerUser);
                return false;
            }
            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_USERNAME, username);
            return true;
        } catch (BusinessException e) {
            log.warn("[ws] 握手失败：业务拒绝, code={}, msg={}", e.getCode(), e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            log.warn("[ws] 握手失败：Token subject 非法");
            return false;
        } catch (RuntimeException e) {
            log.warn("[ws] 握手失败：type={}, msg={}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成，无需额外处理
    }

    /**
     * 解析 Token：优先子协议，其次 Authorization 头
     */
    private String resolveToken(ServerHttpRequest request, ServerHttpResponse response) {
        String fromProtocol = resolveTokenFromProtocol(request, response);
        if (StrUtil.isNotBlank(fromProtocol)) {
            return fromProtocol;
        }
        return resolveTokenFromAuthorization(request);
    }

    /**
     * 从 Sec-WebSocket-Protocol 解析 Token，并回写接受的子协议
     */
    private String resolveTokenFromProtocol(ServerHttpRequest request, ServerHttpResponse response) {
        List<String> protocols = request.getHeaders().get(HEADER_SEC_WEBSOCKET_PROTOCOL);
        if (protocols == null || protocols.isEmpty()) {
            return null;
        }
        String expected = StrUtil.blankToDefault(
                luoteProperties.getWebsocket().getProtocol(), DEFAULT_PROTOCOL);
        String matchedProtocol = null;
        String token = null;
        for (String raw : protocols) {
            for (String part : StrUtil.splitTrim(raw, ',')) {
                if (expected.equalsIgnoreCase(part)) {
                    matchedProtocol = expected;
                } else if (StrUtil.isNotBlank(part) && !expected.equalsIgnoreCase(part)) {
                    token = part;
                }
            }
        }
        if (matchedProtocol == null || StrUtil.isBlank(token)) {
            return null;
        }
        response.getHeaders().set(HEADER_SEC_WEBSOCKET_PROTOCOL, matchedProtocol);
        return token;
    }

    /**
     * 从 Authorization: Bearer &lt;jwt&gt; 解析（微信小程序等无子协议场景）
     */
    private String resolveTokenFromAuthorization(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(header) || !StrUtil.startWithIgnoreCase(header, CommonConstant.TOKEN_PREFIX)) {
            return null;
        }
        return StrUtil.trim(StrUtil.removePrefixIgnoreCase(header, CommonConstant.TOKEN_PREFIX));
    }
}

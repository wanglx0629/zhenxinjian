package cn.zhenxinjian.security;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.JwtUtils;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截 Token，写入 ThreadLocal，有互动则续期
 * 白名单接口上无效 Token 不阻断（便于登录页带过期 Token 仍可拉验证码）
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final SecurityJsonWriter securityJsonWriter;
    private final ZhenxinjianProperties luoteProperties;
    private final UserCacheService userCacheService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StrUtil.isNotBlank(token)) {
                try {
                    authenticateToken(token, response);
                } catch (BusinessException | IllegalArgumentException e) {
                    // 白名单接口忽略无效 Token，继续匿名访问
                    if (isPermitUrl(request.getRequestURI(), request.getContextPath())) {
                        SecurityContextHolder.clearContext();
                        UserContext.clear();
                        log.debug("白名单接口忽略无效Token: uri={}", request.getRequestURI());
                    } else {
                        int code = e instanceof BusinessException be
                                ? be.getCode()
                                : CommonConstant.UNAUTHORIZED_CODE;
                        String message = e instanceof BusinessException be
                                ? be.getMessage()
                                : ExceptionConstant.TOKEN_INVALID;
                        securityJsonWriter.write(response, code, message);
                        return;
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            securityJsonWriter.write(response, e.getCode(), e.getMessage());
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 校验 Token 并写入安全上下文
     */
    private void authenticateToken(String token, HttpServletResponse response) {
        Claims claims = jwtUtils.parseToken(token);
        Long userId = Long.valueOf(claims.getSubject());

        String cachedToken = redisUtils.getToken(userId);
        if (cachedToken == null || !StrUtil.equals(cachedToken, token)) {
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.TOKEN_EXPIRED);
        }

        // 续期与鉴权均以库中最新身份为准，避免角色/禁用状态滞后
        User user = userCacheService.getById(userId);
        if (user == null) {
            redisUtils.removeToken(userId);
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.TOKEN_EXPIRED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            redisUtils.removeToken(userId);
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.ACCOUNT_DISABLED);
        }
        String username = user.getUsername();
        String role = StrUtil.blankToDefault(user.getRole(), CommonConstant.ROLE_USER);
        if (!CommonConstant.ROLE_ADMIN.equals(role)) {
            role = CommonConstant.ROLE_USER;
        }

        if (jwtUtils.isNearExpire(claims)) {
            String newToken = jwtUtils.generateToken(userId, username, role);
            redisUtils.saveToken(userId, newToken);
            response.setHeader("X-Refresh-Token", newToken);
            response.setHeader("Access-Control-Expose-Headers", "X-Refresh-Token");
            log.debug("Token续期: userId={}", userId);
        } else {
            redisUtils.refreshTokenExpire(userId);
        }

        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setId(userId);
        loginUser.setUsername(username);
        loginUser.setRole(role);
        UserContext.set(loginUser);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 判断是否安全白名单（兼容 context-path）
     */
    private boolean isPermitUrl(String requestUri, String contextPath) {
        String path = requestUri;
        if (StrUtil.isNotBlank(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (StrUtil.isBlank(path)) {
            path = "/";
        }
        List<String> permitUrls = luoteProperties.getSecurity().getPermitUrls();
        if (permitUrls == null || permitUrls.isEmpty()) {
            return false;
        }
        for (String pattern : permitUrls) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求头解析 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstant.HEADER_AUTH);
        if (StrUtil.isNotBlank(header) && StrUtil.startWith(header, CommonConstant.TOKEN_PREFIX)) {
            return StrUtil.removePrefix(header, CommonConstant.TOKEN_PREFIX);
        }
        return null;
    }
}

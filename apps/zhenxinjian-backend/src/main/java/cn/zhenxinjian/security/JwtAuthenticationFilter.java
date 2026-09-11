package cn.zhenxinjian.security;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.UserStatusEnum;
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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截 Token，写入 ThreadLocal，有互动则续期
 * 白名单接口上无效 Token 不阻断（便于登录页带过期 Token 仍可拉验证码）
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final SecurityJsonWriter securityJsonWriter;
    private final ZhenxinjianProperties zhenxinjianProperties;
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
        boolean guest = CommonConstant.USER_TYPE_GUEST.equals(jwtUtils.getUserType(claims));

        // 游客到期 claim 早判：gexp < now 直接拒绝（无须查库）
        Date gexp = jwtUtils.getGuestExpireAt(claims);
        if (guest && gexp != null && gexp.before(new Date())) {
            throw new BusinessException(CommonConstant.GUEST_EXPIRED_CODE, ExceptionConstant.GUEST_EXPIRED);
        }

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
        // 非正常状态（冻结/注销）账号立即作废会话
        if (user.getStatus() == null || UserStatusEnum.of(user.getStatus()) != UserStatusEnum.NORMAL) {
            redisUtils.removeToken(userId);
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.ACCOUNT_DISABLED);
        }
        // 游客到期 DB 兜底：guest_expire_at 为最终真源（支持管理侧改库提前作废）
        if (guest && user.getGuestExpireAt() != null
                && user.getGuestExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(CommonConstant.GUEST_EXPIRED_CODE, ExceptionConstant.GUEST_EXPIRED);
        }
        String username = user.getUsername();
        String role = StrUtil.blankToDefault(user.getRole(), CommonConstant.ROLE_USER);
        if (!CommonConstant.ROLE_ADMIN.equals(role)) {
            role = CommonConstant.ROLE_USER;
        }

        if (jwtUtils.isNearExpire(claims)) {
            // 续期保留身份 claim（游客 userType/gexp 随新 token 延续）
            String newToken = jwtUtils.generateToken(userId, username, role,
                    jwtUtils.getUserType(claims), jwtUtils.getGuestExpireAt(claims));
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
        loginUser.setUserType(user.getUserType());
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
        List<String> permitUrls = zhenxinjianProperties.getSecurity().getPermitUrls();
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

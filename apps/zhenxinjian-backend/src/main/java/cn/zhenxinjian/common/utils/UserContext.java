package cn.zhenxinjian.common.utils;

import cn.zhenxinjian.domain.vo.LoginUserVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 当前登录用户 ThreadLocal 上下文
 * 作者: wanglx
 */
@Slf4j
public final class UserContext {

    private static final ThreadLocal<LoginUserVO> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前用户
     */
    public static void set(LoginUserVO user) {
        CONTEXT.set(user);
    }

    /**
     * 获取当前用户
     */
    public static LoginUserVO get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        LoginUserVO user = CONTEXT.get();
        return user != null ? user.getId() : null;
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }
}

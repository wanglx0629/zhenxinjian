package cn.zhenxinjian.common.ai;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI Alibaba 验证上下文（ThreadLocal）
 * 拦截验证结果存入 ThreadLocal，供业务层后续调用
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
public final class AiValidationContext {

    private static final ThreadLocal<AiValidationResult> CONTEXT = new ThreadLocal<>();

    private AiValidationContext() {
    }

    /**
     * 设置验证结果
     */
    public static void set(AiValidationResult result) {
        CONTEXT.set(result);
    }

    /**
     * 获取验证结果
     */
    public static AiValidationResult get() {
        return CONTEXT.get();
    }

    /**
     * 是否通过验证
     */
    public static boolean isPassed() {
        AiValidationResult result = CONTEXT.get();
        return result != null && result.isPassed();
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }
}

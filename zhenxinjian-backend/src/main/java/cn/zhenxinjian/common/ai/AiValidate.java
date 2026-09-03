package cn.zhenxinjian.common.ai;

import java.lang.annotation.*;

import cn.zhenxinjian.common.constant.ExceptionConstant;

/**
 * Spring AI Alibaba 内容验证注解
 * 标注在方法或参数上，拦截验证结果写入 ThreadLocal
 * 作者: luote (luote) - https://luote996.cn
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiValidate {

    /** 待验证内容的 SpEL 表达式或参数名 */
    String value() default "";

    /** 验证失败时的提示信息 */
    String message() default ExceptionConstant.AI_VALIDATE_FAIL;
}

package cn.zhenxinjian.common.ai;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Spring AI Alibaba 验证切面
 * 基于 DashScope 通义千问，拦截 @AiValidate 标注的方法，验证结果写入 ThreadLocal
 * 作者: luote (luote) - https://luote996.cn
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ChatClient.Builder.class)
public class AiValidationAspect {

    private final ChatClient.Builder chatClientBuilder;

    @Around("@annotation(aiValidate)")
    public Object around(ProceedingJoinPoint joinPoint, AiValidate aiValidate) throws Throwable {
        try {
            String content = extractContent(joinPoint, aiValidate);
            if (StrUtil.isNotBlank(content)) {
                AiValidationResult result = doValidate(content);
                AiValidationContext.set(result);
                if (!result.isPassed()) {
                    throw new BusinessException(aiValidate.message());
                }
            }
            return joinPoint.proceed();
        } finally {
            AiValidationContext.clear();
        }
    }

    /**
     * 调用 Spring AI Alibaba（DashScope）进行内容安全验证
     */
    private AiValidationResult doValidate(String content) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = "请判断以下内容是否包含违规、敏感或不当信息，仅回复 PASS 或 REJECT：" + content;
            String response = chatClient.prompt(prompt).call().content();
            if (StrUtil.containsIgnoreCase(response, "PASS")) {
                return AiValidationResult.pass(content);
            }
            return AiValidationResult.reject(content, ExceptionConstant.AI_VALIDATE_FAIL);
        } catch (Exception e) {
            log.warn("AI验证服务不可用，拒绝请求: {}", e.getMessage());
            return AiValidationResult.reject(content, ExceptionConstant.AI_VALIDATE_FAIL);
        }
    }

    /**
     * 从方法参数中提取待验证内容
     */
    private String extractContent(ProceedingJoinPoint joinPoint, AiValidate aiValidate) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (args[i] instanceof String str && StrUtil.isNotBlank(str)) {
                    return str;
                }
            }
        }
        return null;
    }
}

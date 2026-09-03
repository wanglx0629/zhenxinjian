package cn.zhenxinjian.common.ai;

import lombok.Data;

/**
 * Spring AI Alibaba 验证结果
 * 作者: luote (luote) - https://luote996.cn
 */
@Data
public class AiValidationResult {

    /** 是否通过 */
    private boolean passed;

    /** 验证消息 */
    private String message;

    /** 原始内容 */
    private String content;

    public static AiValidationResult pass(String content) {
        AiValidationResult result = new AiValidationResult();
        result.setPassed(true);
        result.setMessage("验证通过");
        result.setContent(content);
        return result;
    }

    public static AiValidationResult reject(String content, String message) {
        AiValidationResult result = new AiValidationResult();
        result.setPassed(false);
        result.setMessage(message);
        result.setContent(content);
        return result;
    }
}

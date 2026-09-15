package cn.zhenxinjian.common.ai;

import java.util.List;

/**
 * 多模态视觉对话客户端薄封装（隔离具体 SDK，便于单测 mock）
 * 作者: wanglx
 */
public interface AiChatClient {

    /**
     * 发起一次多模态对话（文本提示 + 图片），返回模型文本输出
     *
     * @param model          模型编码（如 glm-4.6v-flash）
     * @param systemPrompt   系统提示词
     * @param images         图片列表（base64 + mimeType）
     * @param timeoutSeconds 调用超时（秒）
     * @return 模型输出的文本内容
     */
    String chat(String model, String systemPrompt, List<AiImage> images, int timeoutSeconds);

    /**
     * 图片入参（base64 数据 + MIME 类型）
     */
    record AiImage(String base64, String mimeType) {
    }
}

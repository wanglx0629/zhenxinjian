package cn.zhenxinjian.common.ai;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.service.ConfigService;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import com.zhipu.oapi.service.v4.model.ModelData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 智谱 BigModel 多模态客户端实现（GLM-4.6V-Flash 视觉理解）
 * 每次调用现读 api-key/model/timeout 懒构建 SDK 客户端，后台改配置下一次请求即生效
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuAiChatClient implements AiChatClient {

    private final ConfigService configService;

    @Override
    public String chat(String model, String systemPrompt, List<AiImage> images, int timeoutSeconds) {
        String apiKey = configService.getValue(ProjectConfigKeyConstant.OCR_API_KEY);
        if (StrUtil.isBlank(apiKey)) {
            throw new AiCallException("zhipu api-key not configured", 0, false);
        }
        // 每次调用懒构建：key 更换/超时调整即时生效；disableTokenCache 直接以 APIKey 作 Bearer
        ClientV4 client = new ClientV4.Builder(apiKey)
                .disableTokenCache()
                .networkConfig(timeoutSeconds, timeoutSeconds, timeoutSeconds, timeoutSeconds, TimeUnit.SECONDS)
                .build();

        List<ChatMessage> messages = new ArrayList<>(2);
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), buildImageContent(images)));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .temperature(0.2f)
                .build();

        ModelApiResponse response;
        try {
            response = client.invokeModelApi(request);
        } catch (RuntimeException e) {
            // 网络/IO/超时等 SDK 运行时异常：可能是临时抖动，按可转移错误处理
            log.warn("智谱视觉识别网络/SDK 异常: {}", e.getMessage());
            throw new AiCallException("zhipu chat invoke error: " + e.getMessage(), e, 0, true);
        }
        if (response == null) {
            log.warn("智谱视觉识别调用失败: null response");
            throw new AiCallException("zhipu chat failed: null response", 0, true);
        }
        if (!response.isSuccess()) {
            // SDK 将上游 HTTP 状态码写入 code（如 429/401/500），业务码仅作日志
            int httpStatus = response.getCode();
            String detail = response.getError() != null && response.getError().getMessage() != null
                    ? response.getError().getMessage() : response.getMsg();
            log.warn("智谱视觉识别调用失败: httpStatus={}, msg={}", httpStatus, detail);
            throw new AiCallException("zhipu chat failed: " + detail, httpStatus,
                    AiCallException.isTransferableStatus(httpStatus));
        }
        if (response.getData() == null) {
            log.warn("智谱视觉识别调用失败: data 缺失");
            throw new AiCallException("zhipu chat failed: data missing", 0, true);
        }
        return extractContent(response.getData());
    }

    /**
     * 组装多模态 user content：image_url 数据块（data:{mime};base64,{b64}）
     */
    private List<Map<String, Object>> buildImageContent(List<AiImage> images) {
        List<Map<String, Object>> content = new ArrayList<>(images.size());
        for (AiImage image : images) {
            Map<String, Object> urlBlock = new HashMap<>(2);
            urlBlock.put("url", "data:" + image.mimeType() + ";base64," + image.base64());
            Map<String, Object> imageBlock = new HashMap<>(2);
            imageBlock.put("type", "image_url");
            imageBlock.put("image_url", urlBlock);
            content.add(imageBlock);
        }
        return content;
    }

    /**
     * 取首个候选 choice 的文本内容
     */
    private String extractContent(ModelData data) {
        if (data.getChoices() == null || data.getChoices().isEmpty()
                || data.getChoices().get(0).getMessage() == null) {
            // 响应体结构性异常：按可转移错误处理（换模型可能恢复）
            throw new AiCallException("zhipu chat response has no choices", 0, true);
        }
        Object text = data.getChoices().get(0).getMessage().getContent();
        if (text == null) {
            throw new AiCallException("zhipu chat response content is null", 0, true);
        }
        return String.valueOf(text);
    }
}

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
            throw new IllegalStateException("zhipu api-key not configured");
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

        ModelApiResponse response = client.invokeModelApi(request);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String msg = response == null ? "null response" : response.getMsg();
            log.warn("智谱视觉识别调用失败: {}", msg);
            throw new IllegalStateException("zhipu chat failed: " + msg);
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
            throw new IllegalStateException("zhipu chat response has no choices");
        }
        Object text = data.getChoices().get(0).getMessage().getContent();
        if (text == null) {
            throw new IllegalStateException("zhipu chat response content is null");
        }
        return String.valueOf(text);
    }
}

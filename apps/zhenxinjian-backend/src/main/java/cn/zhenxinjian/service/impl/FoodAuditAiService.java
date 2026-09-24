package cn.zhenxinjian.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.ai.AiChatClient;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.enums.FoodAiVerdictEnum;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 共建食物 AI 文本营养校验服务（纯文本模型）
 * 仅判断「名称/品类与营养是否离谱」，输出供人工审核参考，不直接改用户数据；
 * 任何失败（未配置/超时/限流/坏 JSON）一律降级 verdict=none，不阻塞用户提交
 * 作者: wanglx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodAuditAiService {

    /** 建议文本最大长度（对应 foods.ai_suggestion 列宽 500） */
    private static final int SUGGESTION_MAX_LENGTH = 500;

    /** 默认调用超时（秒），复用 OCR 超时配置口径 */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /** 严格 JSON 输出系统提示词 */
    private static final String SYSTEM_PROMPT = """
            你是食物营养数据审核助手。用户将提交一条共建食物的名称、分类与每100g营养值，\
            你判断「名称/品类与营养值是否匹配合理」。常见食物营养常识作参考，如：\
            纯水类热量≈0、蔬菜类每100g通常<100kcal、油脂类每100g约884kcal、精制糖约387kcal。
            只输出严格 JSON，不要输出任何其他文字或 Markdown 代码围栏，格式：
            {"verdict":"pass|suspect|reject","suggestion":"结论说明或修正建议(≤200字)"}
            verdict 取值：pass=名称与营养匹配合理；suspect=存在可疑点需人工复核；reject=名称与营养明显不符。
            """;

    private final AiChatClient aiChatClient;
    private final ConfigService configService;

    /**
     * AI 校验结果（verdict + 建议；verdict=none 表示未校验或降级）
     */
    public record AiCheckResult(FoodAiVerdictEnum verdict, String suggestion) {

        /** 未校验/降级结果（不阻塞提交） */
        static AiCheckResult none() {
            return new AiCheckResult(FoodAiVerdictEnum.NONE, null);
        }
    }

    /**
     * 对一条共建食物做营养合理性校验
     *
     * @param food 已落库的共建食物（含名称/分类/kcal/kj/三宏）
     * @return 校验结论；任何异常降级为 none，永不抛错
     */
    public AiCheckResult audit(Food food) {
        String model = configService.getValue(ProjectConfigKeyConstant.AI_FOOD_AUDIT_MODEL);
        if (StrUtil.isBlank(model)) {
            // 未配置文本模型 = 关闭 AI 校验，不阻塞
            return AiCheckResult.none();
        }
        try {
            int timeout = configService.getInt(ProjectConfigKeyConstant.OCR_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
            String output = aiChatClient.chatText(model.trim(), SYSTEM_PROMPT, buildUserText(food), timeout);
            return parse(output);
        } catch (Exception e) {
            // 超时/限流/网络等一切异常：降级 none，不阻塞用户提交
            log.warn("AI 营养校验降级（不影响提交）: {}", e.getMessage());
            return AiCheckResult.none();
        }
    }

    /**
     * 组装用户侧文本（名称/分类/kcal/kj/三宏）
     */
    private String buildUserText(Food food) {
        return "食物名称：" + food.getName()
                + "\n分类：" + StrUtil.nullToEmpty(food.getCategoryName())
                + "\n每100g营养值：碳水 " + food.getCarb() + "g，蛋白质 " + food.getProtein()
                + "g，脂肪 " + food.getFat() + "g，能量 " + food.getKcal() + "kcal"
                + (food.getKj() != null ? "（" + food.getKj() + "kJ）" : "");
    }

    /**
     * 解析模型输出：剥离围栏 → JSON → verdict/suggestion；未知 verdict 或坏 JSON 降级 none
     */
    private AiCheckResult parse(String raw) {
        try {
            String json = stripCodeFence(raw);
            JSONObject root = JSONUtil.parseObj(json);
            FoodAiVerdictEnum verdict = FoodAiVerdictEnum.of(root.getStr("verdict"));
            if (verdict == null || verdict == FoodAiVerdictEnum.NONE) {
                return AiCheckResult.none();
            }
            String suggestion = StrUtil.trimToNull(root.getStr("suggestion"));
            if (suggestion != null && suggestion.length() > SUGGESTION_MAX_LENGTH) {
                suggestion = suggestion.substring(0, SUGGESTION_MAX_LENGTH);
            }
            return new AiCheckResult(verdict, suggestion);
        } catch (Exception e) {
            log.warn("AI 营养校验输出无法解析（不影响提交）: {}", e.getMessage());
            return AiCheckResult.none();
        }
    }

    /**
     * 剥离 ```json ... ``` 代码围栏，并截取首尾花括号间内容（与 OCR 解析同口径）
     */
    private String stripCodeFence(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline >= 0) {
                text = text.substring(firstNewline + 1);
            }
            int fenceEnd = text.lastIndexOf("```");
            if (fenceEnd >= 0) {
                text = text.substring(0, fenceEnd);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text.trim();
    }
}

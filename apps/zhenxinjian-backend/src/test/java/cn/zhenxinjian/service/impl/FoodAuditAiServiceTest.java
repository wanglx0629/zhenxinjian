package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.ai.AiCallException;
import cn.zhenxinjian.common.ai.AiChatClient;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.enums.FoodAiVerdictEnum;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FoodAuditAiService 单元测试（结论解析 / Markdown 围栏 / 坏 JSON 与异常降级 none 不阻塞 / 未配置跳过调用）
 * 作者: wanglx
 */
class FoodAuditAiServiceTest {

    private AiChatClient aiChatClient;
    private ConfigService configService;
    private FoodAuditAiService service;

    @BeforeEach
    void setUp() {
        aiChatClient = mock(AiChatClient.class);
        configService = mock(ConfigService.class);
        service = new FoodAuditAiService(aiChatClient, configService);

        when(configService.getValue(ProjectConfigKeyConstant.AI_FOOD_AUDIT_MODEL)).thenReturn("glm-4-flash");
        when(configService.getInt(eq(ProjectConfigKeyConstant.OCR_TIMEOUT_SECONDS), eq(30))).thenReturn(30);
    }

    private Food food() {
        Food food = new Food();
        food.setName("自制燕麦碗");
        food.setCategoryName("谷薯杂豆·主食");
        food.setCarb(new java.math.BigDecimal("55.0"));
        food.setProtein(new java.math.BigDecimal("12.0"));
        food.setFat(new java.math.BigDecimal("8.0"));
        food.setKcal(348);
        food.setKj(1456);
        return food;
    }

    /** 场景：模型回 pass → 结论 PASS，不抛错 */
    @Test
    void audit_pass_parsed() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("{\"verdict\":\"pass\",\"suggestion\":\"\"}");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.PASS, result.verdict());
        assertNull(result.suggestion());
    }

    /** 场景：模型回 suspect + 建议 → 结论 SUSPECT 且保留建议文本 */
    @Test
    void audit_suggestion_preserved() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("{\"verdict\":\"suspect\",\"suggestion\":\"脂肪偏高，建议核对为 5g\"}");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.SUSPECT, result.verdict());
        assertEquals("脂肪偏高，建议核对为 5g", result.suggestion());
    }

    /** 场景：模型回 reject → 结论 REJECT */
    @Test
    void audit_reject_parsed() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("{\"verdict\":\"reject\",\"suggestion\":\"名称与营养明显不符\"}");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.REJECT, result.verdict());
    }

    /** 场景：模型输出带 Markdown 围栏 → 剥离后正常解析 */
    @Test
    void audit_markdownFence_stripped() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("```json\n{\"verdict\":\"pass\",\"suggestion\":\"\"}\n```");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.PASS, result.verdict());
    }

    /** 场景：坏 JSON → 降级 NONE，不抛错不阻塞 */
    @Test
    void audit_badJson_degradesToNone() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("这不是JSON");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.NONE, result.verdict());
        assertNull(result.suggestion());
    }

    /** 场景：AI 调用异常（限流/超时等）→ 降级 NONE，不抛错不阻塞 */
    @Test
    void audit_aiException_degradesToNone() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new AiCallException("zhipu chat failed: 429", 429, true));

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.NONE, result.verdict());
    }

    /** 场景：未知 verdict 值 → 降级 NONE */
    @Test
    void audit_unknownVerdict_degradesToNone() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("{\"verdict\":\"maybe\",\"suggestion\":\"x\"}");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.NONE, result.verdict());
    }

    /** 场景：模型未配置（空值=关闭 AI 校验）→ 不调模型直接 NONE */
    @Test
    void audit_modelNotConfigured_skipsCall() {
        when(configService.getValue(ProjectConfigKeyConstant.AI_FOOD_AUDIT_MODEL)).thenReturn(" ");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.NONE, result.verdict());
        verify(aiChatClient, never()).chatText(anyString(), anyString(), anyString(), anyInt());
    }

    /** 场景：建议超长 → 截断到 500 字符以内（列宽口径） */
    @Test
    void audit_longSuggestion_truncated() {
        when(aiChatClient.chatText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("{\"verdict\":\"suspect\",\"suggestion\":\"" + "长".repeat(600) + "\"}");

        FoodAuditAiService.AiCheckResult result = service.audit(food());

        assertEquals(FoodAiVerdictEnum.SUSPECT, result.verdict());
        assertTrue(result.suggestion().length() <= 500);
    }
}

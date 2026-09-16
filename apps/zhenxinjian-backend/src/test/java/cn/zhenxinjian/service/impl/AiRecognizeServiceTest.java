package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.ai.AiCallException;
import cn.zhenxinjian.common.ai.AiChatClient;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.domain.vo.FoodRecognizeVO;
import cn.zhenxinjian.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiRecognizeService 单元测试（开关/校验/指纹缓存/限流/key 缺失/解析重试/守恒重算/脏值丢弃）
 * 作者: wanglx
 */
class AiRecognizeServiceTest {

    private AiChatClient aiChatClient;
    private ConfigService configService;
    private RedisUtils redisUtils;
    private AiRecognizeService service;

    @BeforeEach
    void setUp() {
        aiChatClient = mock(AiChatClient.class);
        configService = mock(ConfigService.class);
        redisUtils = mock(RedisUtils.class);
        service = new AiRecognizeService(aiChatClient, configService, redisUtils);

        when(configService.getBoolean(eq(ProjectConfigKeyConstant.OCR_ENABLED), eq(true))).thenReturn(true);
        when(configService.getInt(eq(ProjectConfigKeyConstant.OCR_DAILY_LIMIT), eq(20))).thenReturn(20);
        when(configService.getInt(eq(ProjectConfigKeyConstant.OCR_TIMEOUT_SECONDS), eq(30))).thenReturn(30);
        when(configService.getInt(eq(ProjectConfigKeyConstant.OCR_CACHE_TTL_HOURS), eq(24))).thenReturn(24);
        when(configService.getValue(ProjectConfigKeyConstant.OCR_API_KEY)).thenReturn("kid.secret");
        when(configService.getValue(ProjectConfigKeyConstant.OCR_MODEL)).thenReturn("glm-4.6v-flash");
        when(configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS)).thenReturn(List.of());
        when(configService.getInt(eq(ProjectConfigKeyConstant.OCR_CIRCUIT_FAIL_THRESHOLD), eq(3))).thenReturn(3);
        when(configService.getInt(eq(ProjectConfigKeyConstant.OCR_CIRCUIT_OPEN_SECONDS), eq(300))).thenReturn(300);
        // 默认：指纹缓存未命中、当日未计次、无模型熔断
        when(redisUtils.get(startsWith("zhenxinjian:ocr:img:"))).thenReturn(null);
        when(redisUtils.get(startsWith("zhenxinjian:ocr:daily:"))).thenReturn(null);
        when(redisUtils.get(startsWith("zhenxinjian:ocr:circuit:"))).thenReturn(null);
    }

    private MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", "fake-image-bytes".getBytes());
    }

    /** 场景：功能关闭 → fail-closed 拒识，不调模型不计次 */
    @Test
    void recognize_disabled_throwsDisabled() {
        when(configService.getBoolean(eq(ProjectConfigKeyConstant.OCR_ENABLED), eq(true))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_SERVICE_DISABLED_CODE, ex.getCode());
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
    }

    /** 场景：非法扩展名 → 参数校验失败，不调模型不计次 */
    @Test
    void recognize_gif_rejected() {
        MockMultipartFile gif = new MockMultipartFile("file", "a.gif", "image/gif", "x".getBytes());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, gif));
        assertEquals(CommonConstant.FAIL_CODE, ex.getCode());
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
        verify(redisUtils, never()).incrementWithTtl(anyString(), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：超过 10MB → 拒绝 */
    @Test
    void recognize_oversized_rejected() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", big);

        assertThrows(BusinessException.class, () -> service.recognize(1L, file));
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
    }

    /** 场景：同图指纹命中 → 直返缓存，不调模型不计次不查限流 */
    @Test
    void recognize_fingerprintHit_returnsCachedWithoutModelCall() {
        when(redisUtils.get(startsWith("zhenxinjian:ocr:img:")))
                .thenReturn("[{\"name\":\"白米饭\",\"carb\":25.9,\"protein\":2.6,\"fat\":0.3,\"kcal\":116.7}]");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("a.jpg"));

        assertEquals(1, result.size());
        assertEquals("白米饭", result.get(0).getName());
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
        verify(redisUtils, never()).incrementWithTtl(anyString(), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：当日次数用完 → 限流文案，不调模型 */
    @Test
    void recognize_dailyLimitExceeded_throwsLimit() {
        when(redisUtils.get(startsWith("zhenxinjian:ocr:daily:"))).thenReturn("20");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_DAILY_LIMIT_CODE, ex.getCode());
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
    }

    /** 场景：api-key 缺失 → 配置缺失文案 */
    @Test
    void recognize_apiKeyMissing_throwsConfigMissing() {
        when(configService.getValue(ProjectConfigKeyConstant.OCR_API_KEY)).thenReturn("  ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_CONFIG_MISSING_CODE, ex.getCode());
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
    }

    /** 场景：正常识别 → kcal 按 4/4/9 重算覆盖模型值，写缓存并计次 */
    @Test
    void recognize_success_kcalRecalculatedAndCounted() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt())).thenReturn(
                "```json\n"
                + "{\"items\":[{\"name\":\"白米饭\",\"carb\":25.9,\"protein\":2.6,\"fat\":0.3,\"kcal\":999}]}"
                + "\n```");

        List<FoodRecognizeVO> result = service.recognize(7L, jpeg("rice.jpg"));

        assertEquals(1, result.size());
        FoodRecognizeVO vo = result.get(0);
        assertEquals("白米饭", vo.getName());
        assertEquals(116.7, vo.getKcal(), 0.01);
        verify(redisUtils).setEx(startsWith("zhenxinjian:ocr:img:"), anyString(), eq(24L), eq(TimeUnit.HOURS));
        verify(redisUtils).incrementWithTtl(startsWith("zhenxinjian:ocr:daily:7:"), anyLong(), eq(TimeUnit.SECONDS));
    }

    /** 场景：首次输出无法解析，重试后成功 → 返回结果 */
    @Test
    void recognize_firstCallUnparseable_retrySucceeds() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("抱歉，我无法识别")
                .thenReturn("{\"items\":[{\"name\":\"鸡蛋\",\"carb\":1.1,\"protein\":13.0,\"fat\":9.0}]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("egg.jpg"));

        assertEquals(1, result.size());
        assertEquals("鸡蛋", result.get(0).getName());
        verify(aiChatClient, org.mockito.Mockito.times(2)).chat(anyString(), anyString(), anyList(), anyInt());
        verify(redisUtils).incrementWithTtl(anyString(), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：两次输出均无法解析 → 识别失败（不转移），不计次 */
    @Test
    void recognize_bothOutputsUnparseable_throwsRecognizeFailAndNotCounted() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("抱歉，我无法识别")
                .thenReturn("还是无法识别");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_RECOGNIZE_FAIL_CODE, ex.getCode());
        verify(aiChatClient, org.mockito.Mockito.times(2)).chat(anyString(), anyString(), anyList(), anyInt());
        verify(redisUtils, never()).incrementWithTtl(anyString(), anyLong(), ArgumentMatchers.any());
        verify(redisUtils, never()).setEx(anyString(), anyString(), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：主模型可转移错误（超时）且无备用 → 繁忙文案，失败计数 +1，不计次不写指纹 */
    @Test
    void recognize_transferableErrorNoFallback_throwsBusyAndCountedFailureOnce() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt()))
                .thenThrow(new AiCallException("timeout", 0, true));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_BUSY_CODE, ex.getCode());
        verify(aiChatClient, org.mockito.Mockito.times(1)).chat(anyString(), anyString(), anyList(), anyInt());
        verify(redisUtils).incrementWithTtl(startsWith("zhenxinjian:ocr:modelfail:"), eq(300L), eq(TimeUnit.SECONDS));
        verify(redisUtils, never()).incrementWithTtl(startsWith("zhenxinjian:ocr:daily:"), anyLong(), ArgumentMatchers.any());
        verify(redisUtils, never()).setEx(startsWith("zhenxinjian:ocr:img:"), anyString(), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：非食物图返回空 items → 业务成功空列表，不写缓存不计次 */
    @Test
    void recognize_nonFoodImage_emptyResultNotCachedOrCounted() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("{\"items\":[]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("scene.jpg"));

        assertTrue(result.isEmpty());
        verify(redisUtils, never()).setEx(anyString(), anyString(), anyLong(), ArgumentMatchers.any());
        verify(redisUtils, never()).incrementWithTtl(anyString(), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：脏宏量（负数/超界/非数字）置 0；候选项截断至 10 项 */
    @Test
    void recognize_dirtyMacrosSanitizedAndItemsCappedAt10() {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 12; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"name\":\"食物").append(i).append("\",\"carb\":-5,\"protein\":200,\"fat\":\"x\"}");
        }
        json.append("]}");
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt())).thenReturn(json.toString());

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("many.jpg"));

        assertEquals(10, result.size());
        FoodRecognizeVO first = result.get(0);
        assertEquals(0d, first.getCarb(), 0.01);
        assertEquals(0d, first.getProtein(), 0.01);
        assertEquals(0d, first.getFat(), 0.01);
        assertEquals(0d, first.getKcal(), 0.01);
    }

    /** 场景：一图多食 → 多项返回，各项独立重算 */
    @Test
    void recognize_multipleFoods_allReturned() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt())).thenReturn(
                "{\"items\":["
                + "{\"name\":\"鸡胸肉\",\"carb\":0,\"protein\":23,\"fat\":5},"
                + "{\"name\":\"西兰花\",\"carb\":4,\"protein\":3,\"fat\":0.4}"
                + "]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("meal.jpg"));

        assertEquals(2, result.size());
        assertEquals(137d, result.get(0).getKcal(), 0.01);
        assertEquals(31.6, result.get(1).getKcal(), 0.01);
    }

    // ==================== 故障转移与熔断（change13） ====================

    private static final String PRIMARY = "glm-4.6v-flash";
    private static final String FALLBACK = "GLM-4V-Flash";

    /** 场景：主模型 429 → 自动切备用模型成功；主模型失败计数 +1，成功模型清熔断 */
    @Test
    void recognize_primary429_fallbackSucceeds() {
        when(configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS))
                .thenReturn(List.of("GLM-4.1V-Thinking-Flash", FALLBACK));
        when(aiChatClient.chat(eq(PRIMARY), anyString(), anyList(), anyInt()))
                .thenThrow(new AiCallException("rate limited", 429, true));
        when(aiChatClient.chat(eq("GLM-4.1V-Thinking-Flash"), anyString(), anyList(), anyInt()))
                .thenThrow(new AiCallException("rate limited", 429, true));
        when(aiChatClient.chat(eq(FALLBACK), anyString(), anyList(), anyInt()))
                .thenReturn("{\"items\":[{\"name\":\"白米饭\",\"carb\":25.9,\"protein\":2.6,\"fat\":0.3}]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("rice.jpg"));

        assertEquals(1, result.size());
        assertEquals("白米饭", result.get(0).getName());
        verify(redisUtils).incrementWithTtl(startsWith("zhenxinjian:ocr:modelfail:" + PRIMARY), eq(300L), eq(TimeUnit.SECONDS));
        verify(redisUtils).incrementWithTtl(startsWith("zhenxinjian:ocr:modelfail:" + "GLM-4.1V-Thinking-Flash"), eq(300L), eq(TimeUnit.SECONDS));
        verify(redisUtils).delete(startsWith("zhenxinjian:ocr:modelfail:" + FALLBACK));
        verify(redisUtils).delete(startsWith("zhenxinjian:ocr:circuit:" + FALLBACK));
        // 成功仍计当日次数
        verify(redisUtils).incrementWithTtl(startsWith("zhenxinjian:ocr:daily:"), anyLong(), eq(TimeUnit.SECONDS));
    }

    /** 场景：主模型熔断中 → 直接跳过主模型，仅调备用模型 */
    @Test
    void recognize_primaryCircuitOpen_skipsToFallback() {
        when(configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS)).thenReturn(List.of(FALLBACK));
        when(redisUtils.get(startsWith("zhenxinjian:ocr:circuit:" + PRIMARY))).thenReturn("1");
        when(aiChatClient.chat(eq(FALLBACK), anyString(), anyList(), anyInt()))
                .thenReturn("{\"items\":[{\"name\":\"鸡蛋\",\"carb\":1.1,\"protein\":13,\"fat\":9}]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("egg.jpg"));

        assertEquals(1, result.size());
        assertEquals("鸡蛋", result.get(0).getName());
        verify(aiChatClient, never()).chat(eq(PRIMARY), anyString(), anyList(), anyInt());
        verify(aiChatClient).chat(eq(FALLBACK), anyString(), anyList(), anyInt());
    }

    /** 场景：主模型 401（密钥错误）→ 立即失败，不调备用，不写熔断，不计次 */
    @Test
    void recognize_primary401_failsFastWithoutFallbackOrCircuit() {
        when(configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS)).thenReturn(List.of(FALLBACK));
        when(aiChatClient.chat(eq(PRIMARY), anyString(), anyList(), anyInt()))
                .thenThrow(new AiCallException("invalid api key", 401, false));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_RECOGNIZE_FAIL_CODE, ex.getCode());
        verify(aiChatClient, never()).chat(eq(FALLBACK), anyString(), anyList(), anyInt());
        verify(redisUtils, never()).incrementWithTtl(startsWith("zhenxinjian:ocr:modelfail:"), anyLong(), ArgumentMatchers.any());
        verify(redisUtils, never()).setEx(startsWith("zhenxinjian:ocr:circuit:"), anyString(), anyLong(), ArgumentMatchers.any());
        verify(redisUtils, never()).incrementWithTtl(startsWith("zhenxinjian:ocr:daily:"), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：主备全部熔断 → 无任何模型调用，快速返回繁忙，不计次 */
    @Test
    void recognize_allCircuitsOpen_fastBusyWithoutModelCall() {
        when(configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS)).thenReturn(List.of(FALLBACK));
        when(redisUtils.get(startsWith("zhenxinjian:ocr:circuit:"))).thenReturn("1");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
        assertEquals(CommonConstant.AI_BUSY_CODE, ex.getCode());
        verify(aiChatClient, never()).chat(anyString(), anyString(), anyList(), anyInt());
        verify(redisUtils, never()).incrementWithTtl(startsWith("zhenxinjian:ocr:daily:"), anyLong(), ArgumentMatchers.any());
    }

    /** 场景：连续可转移失败达到阈值（3）→ 第 3 次写入熔断标记 */
    @Test
    void recognize_failuresReachThreshold_setsCircuitFlag() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt()))
                .thenThrow(new AiCallException("server error", 500, true));
        when(redisUtils.incrementWithTtl(startsWith("zhenxinjian:ocr:modelfail:"), eq(300L), eq(TimeUnit.SECONDS)))
                .thenReturn(1L, 2L, 3L);

        for (int i = 0; i < 3; i++) {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(1L, jpeg("a.jpg")));
            assertEquals(CommonConstant.AI_BUSY_CODE, ex.getCode());
        }
        // 仅在第 3 次（计数达阈值）写熔断标记
        verify(redisUtils, org.mockito.Mockito.times(1))
                .setEx(startsWith("zhenxinjian:ocr:circuit:"), eq("1"), eq(300L), eq(TimeUnit.SECONDS));
    }

    /** 场景：成功调用清除历史失败计数（未熔断模型恢复） */
    @Test
    void recognize_successAfterFailures_clearsFailureCount() {
        when(aiChatClient.chat(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("{\"items\":[{\"name\":\"白米饭\",\"carb\":25.9,\"protein\":2.6,\"fat\":0.3}]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("rice.jpg"));

        assertEquals(1, result.size());
        verify(redisUtils).delete(startsWith("zhenxinjian:ocr:modelfail:" + PRIMARY));
        verify(redisUtils).delete(startsWith("zhenxinjian:ocr:circuit:" + PRIMARY));
    }

    /** 场景：备用列表去空去重保序（主在前），同一模型一次请求最多调一次 */
    @Test
    void recognize_candidatesDedupedAndTrimmed() {
        when(configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS))
                .thenReturn(List.of("  ", PRIMARY, FALLBACK, FALLBACK));
        when(aiChatClient.chat(eq(PRIMARY), anyString(), anyList(), anyInt()))
                .thenThrow(new AiCallException("429", 429, true));
        when(aiChatClient.chat(eq(FALLBACK), anyString(), anyList(), anyInt()))
                .thenReturn("{\"items\":[{\"name\":\"鸡蛋\",\"carb\":1.1,\"protein\":13,\"fat\":9}]}");

        List<FoodRecognizeVO> result = service.recognize(1L, jpeg("egg.jpg"));

        assertEquals(1, result.size());
        // 主模型仅 1 次（去重后备用里的同名项被合并），备用 1 次
        verify(aiChatClient, org.mockito.Mockito.times(1)).chat(eq(PRIMARY), anyString(), anyList(), anyInt());
        verify(aiChatClient, org.mockito.Mockito.times(1)).chat(eq(FALLBACK), anyString(), anyList(), anyInt());
    }
}

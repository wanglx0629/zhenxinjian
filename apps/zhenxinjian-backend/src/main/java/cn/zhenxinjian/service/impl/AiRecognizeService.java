package cn.zhenxinjian.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.ai.AiCallException;
import cn.zhenxinjian.common.ai.AiChatClient;
import cn.zhenxinjian.common.ai.AiChatClient.AiImage;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.Numbers;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.domain.vo.FoodRecognizeVO;
import cn.zhenxinjian.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * AI 食物识别服务（智谱 GLM 视觉模型）
 * 流程：开关 → 校验 → 同图指纹缓存 → 每日限流 → key 检查 → 调模型（失败重试 1 次）
 *      → JSON 解析 → 4/4/9 守恒重算 kcal → 脏值丢弃/10 项截断 → 写缓存与计次
 * 作者: wanglx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecognizeService {

    /** 允许的图片扩展名（口径与存储白名单中的图片类型一致，此处硬编码） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** 图片大小上限 10MB */
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    /** 候选项上限 */
    private static final int MAX_ITEMS = 10;

    /** 食物名最大长度 */
    private static final int MAX_NAME_LENGTH = 20;

    /** 每 100g 宏量上限（超过即脏值，丢弃该项宏量字段置 0） */
    private static final double MAX_MACRO_PER_100G = 100d;

    private static final String DEFAULT_MODEL = "glm-4.6v-flash";

    private static final int DEFAULT_CIRCUIT_FAIL_THRESHOLD = 3;

    private static final int DEFAULT_CIRCUIT_OPEN_SECONDS = 300;

    /** 熔断阈值/窗口秒数下限 */
    private static final int MIN_CIRCUIT_VALUE = 1;

    private static final String IMAGE_CACHE_KEY_PREFIX = "zhenxinjian:ocr:img:";

    private static final String DAILY_COUNT_KEY_PREFIX = "zhenxinjian:ocr:daily:";

    /** 模型连续可转移失败计数 key 前缀（TTL 与熔断窗口对齐） */
    private static final String MODEL_FAIL_KEY_PREFIX = "zhenxinjian:ocr:modelfail:";

    /** 模型熔断标记 key 前缀（存在即熔断中，TTL 为窗口秒数） */
    private static final String CIRCUIT_KEY_PREFIX = "zhenxinjian:ocr:circuit:";

    /** 熔断标记值（仅判存在性） */
    private static final String CIRCUIT_FLAG = "1";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 严格 JSON 输出系统提示词（只识别食物，一图多食，无食物返回空数组） */
    private static final String SYSTEM_PROMPT = """
            你是食物营养识别助手。仅识别图片中可食用的食物，忽略人物、餐具、包装、文字、背景等非食物内容。
            识别图中所有食物（一图多食须逐一列出），给出每 100g 的碳水化合物、蛋白质、脂肪克数。
            只输出严格 JSON，不要输出任何其他文字或 Markdown 代码围栏，格式：
            {"items":[{"name":"食物中文名(≤20字)","carb":每100g碳水克数,"protein":每100g蛋白质克数,"fat":每100g脂肪克数}]}
            数量 1-10 项；拿不准时给出最可能的一项；图中没有任何食物时输出 {"items":[]}。
            """;

    private final AiChatClient aiChatClient;
    private final ConfigService configService;
    private final RedisUtils redisUtils;

    /**
     * 识别图片中的食物
     *
     * @param userId 用户 ID（含游客）
     * @param file   上传的图片
     * @return 候选食物列表（每 100g 基准；非食物图返回空列表）
     */
    public List<FoodRecognizeVO> recognize(Long userId, MultipartFile file) {
        // 1. 开关 fail-closed
        if (!configService.getBoolean(ProjectConfigKeyConstant.OCR_ENABLED, true)) {
            throw new BusinessException(CommonConstant.AI_SERVICE_DISABLED_CODE, ExceptionConstant.AI_SERVICE_DISABLED);
        }

        // 2. 参数校验
        byte[] imageBytes = validateAndRead(file);

        // 3. 同图指纹缓存（与用户无关，全站共享；命中不计次不查限流）
        String fingerprint = DigestUtil.sha256Hex(imageBytes);
        String cacheKey = IMAGE_CACHE_KEY_PREFIX + fingerprint;
        String cached = redisUtils.get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return JSONUtil.toList(cached, FoodRecognizeVO.class);
        }

        // 4. 每日限流（仅在已成功次数达到阈值时拒绝；成功后才计次）
        int dailyLimit = configService.getInt(ProjectConfigKeyConstant.OCR_DAILY_LIMIT, 20);
        String dayKey = DAILY_COUNT_KEY_PREFIX + userId + ":" + LocalDate.now().format(DAY_FORMATTER);
        long used = parseCount(redisUtils.get(dayKey));
        if (used >= dailyLimit) {
            throw new BusinessException(CommonConstant.AI_DAILY_LIMIT_CODE, ExceptionConstant.AI_DAILY_LIMIT);
        }

        // 5. key 检查
        String apiKey = configService.getValue(ProjectConfigKeyConstant.OCR_API_KEY);
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(CommonConstant.AI_CONFIG_MISSING_CODE, ExceptionConstant.AI_CONFIG_MISSING);
        }

        // 6-8. 候选链（主 + 备）逐模型调用：跳过熔断 → 成功清熔断 / 可转移失败计数熔断 / 不可转移立即失败
        String primaryModel = StrUtil.blankToDefault(configService.getValue(ProjectConfigKeyConstant.OCR_MODEL), DEFAULT_MODEL);
        int timeout = configService.getInt(ProjectConfigKeyConstant.OCR_TIMEOUT_SECONDS, 30);
        String mimeType = resolveMimeType(FileUtil.extName(file.getOriginalFilename()));
        AiImage image = new AiImage(Base64.getEncoder().encodeToString(imageBytes), mimeType);

        int failThreshold = Math.max(MIN_CIRCUIT_VALUE,
                configService.getInt(ProjectConfigKeyConstant.OCR_CIRCUIT_FAIL_THRESHOLD, DEFAULT_CIRCUIT_FAIL_THRESHOLD));
        int openSeconds = Math.max(MIN_CIRCUIT_VALUE,
                configService.getInt(ProjectConfigKeyConstant.OCR_CIRCUIT_OPEN_SECONDS, DEFAULT_CIRCUIT_OPEN_SECONDS));
        List<String> candidates = buildCandidates(primaryModel, configService.getStringList(ProjectConfigKeyConstant.OCR_FALLBACK_MODELS));

        List<FoodRecognizeVO> items = invokeWithFallback(candidates, List.of(image), timeout, failThreshold, openSeconds);

        // 非食物图：返回空，不写缓存、不计次（当日剩余次数不变）
        if (items.isEmpty()) {
            return items;
        }

        // 9. 写指纹缓存 + 计次
        int ttlHours = configService.getInt(ProjectConfigKeyConstant.OCR_CACHE_TTL_HOURS, 24);
        redisUtils.setEx(cacheKey, JSONUtil.toJsonStr(items), ttlHours, TimeUnit.HOURS);
        redisUtils.incrementWithTtl(dayKey, secondsUntilMidnight(), TimeUnit.SECONDS);
        return items;
    }

    /**
     * 校验文件并读取字节
     */
    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ExceptionConstant.PARAM_VALID_FAIL);
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ExceptionConstant.PARAM_VALID_FAIL);
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (StrUtil.isBlank(ext) || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ExceptionConstant.PARAM_VALID_FAIL);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.warn("读取识别图片失败: {}", e.getMessage());
            throw new BusinessException(ExceptionConstant.PARAM_VALID_FAIL);
        }
    }

    /**
     * 候选链逐模型编排：熔断中跳过 → 调用（JSON 解析失败同模型加强提示词重试 1 次）
     * → 成功清熔断；可转移失败计数并按阈值置熔断后继续下一模型；不可转移立即失败；全耗尽抛繁忙
     */
    private List<FoodRecognizeVO> invokeWithFallback(List<String> candidates, List<AiImage> image,
                                                     int timeout, int failThreshold, int openSeconds) {
        for (String model : candidates) {
            if (isCircuitOpen(model)) {
                log.info("模型 {} 熔断中，本次跳过", model);
                continue;
            }
            try {
                List<FoodRecognizeVO> items = invokeSingleModel(model, image, timeout);
                // 任一模型成功（含非食物空结果）：清除其失败计数与熔断标记
                clearCircuit(model);
                return items;
            } catch (BusinessException e) {
                // 同模型两次输出均无法解析：内容质量问题，不转移、不计熔断，沿用识别失败文案
                throw e;
            } catch (AiCallException e) {
                if (!e.isTransferable()) {
                    // 密钥/参数类错误：立即失败，不切换、不熔断，避免掩盖配置错误
                    log.warn("模型 {} 返回不可转移错误(httpStatus={})，立即失败: {}", model, e.getHttpStatus(), e.getMessage());
                    throw new BusinessException(CommonConstant.AI_RECOGNIZE_FAIL_CODE, ExceptionConstant.AI_RECOGNIZE_FAIL);
                }
                log.warn("模型 {} 可转移失败(httpStatus={})，准备切换下一候选: {}", model, e.getHttpStatus(), e.getMessage());
                recordFailure(model, failThreshold, openSeconds);
            }
        }
        throw new BusinessException(CommonConstant.AI_BUSY_CODE, ExceptionConstant.AI_BUSY);
    }

    /**
     * 单模型调用并解析：仅模型输出 JSON 解析失败时同模型加强提示词重试 1 次；
     * 服务端/网络错误（AiCallException）由 chat 直接上抛交由故障转移，不在同模型上浪费配额
     */
    private List<FoodRecognizeVO> invokeSingleModel(String model, List<AiImage> image, int timeout) {
        String output = aiChatClient.chat(model, SYSTEM_PROMPT, image, timeout);
        try {
            return parseAndNormalize(output);
        } catch (IllegalArgumentException first) {
            log.warn("模型 {} 首次输出无法解析，准备加强提示词重试: {}", model, first.getMessage());
            String retried = aiChatClient.chat(model,
                    SYSTEM_PROMPT + "\n再次强调：只输出 JSON，不要任何解释或 Markdown 围栏。", image, timeout);
            try {
                return parseAndNormalize(retried);
            } catch (IllegalArgumentException second) {
                log.warn("模型 {} 重试输出仍无法解析: {}", model, second.getMessage());
                throw new BusinessException(CommonConstant.AI_RECOGNIZE_FAIL_CODE, ExceptionConstant.AI_RECOGNIZE_FAIL);
            }
        }
    }

    /**
     * 构建候选链：主模型在前、备用保序，去空白去重（同一模型一次请求最多调用一次）
     */
    private List<String> buildCandidates(String primaryModel, List<String> fallbackModels) {
        LinkedHashSet<String> chain = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(primaryModel)) {
            chain.add(primaryModel.trim());
        }
        if (fallbackModels != null) {
            for (String fallback : fallbackModels) {
                if (StrUtil.isNotBlank(fallback)) {
                    chain.add(fallback.trim());
                }
            }
        }
        return new ArrayList<>(chain);
    }

    /**
     * 熔断标记是否存在（存在即熔断中，跳过该模型）
     */
    private boolean isCircuitOpen(String model) {
        return StrUtil.isNotBlank(redisUtils.get(CIRCUIT_KEY_PREFIX + model));
    }

    /**
     * 记录一次可转移失败：计数 +1（计数 TTL 与熔断窗口对齐）；达阈值写熔断标记
     */
    private void recordFailure(String model, int failThreshold, int openSeconds) {
        long failures = redisUtils.incrementWithTtl(MODEL_FAIL_KEY_PREFIX + model, openSeconds, TimeUnit.SECONDS);
        if (failures >= failThreshold) {
            redisUtils.setEx(CIRCUIT_KEY_PREFIX + model, CIRCUIT_FLAG, openSeconds, TimeUnit.SECONDS);
            log.warn("模型 {} 连续失败 {} 次达到阈值，熔断 {} 秒", model, failures, openSeconds);
        }
    }

    /**
     * 模型调用成功：立即清除失败计数与熔断标记
     */
    private void clearCircuit(String model) {
        redisUtils.delete(MODEL_FAIL_KEY_PREFIX + model);
        redisUtils.delete(CIRCUIT_KEY_PREFIX + model);
    }

    /**
     * 剥离 Markdown 围栏后解析 JSON 并做守恒重算/脏值丢弃/截断
     */
    private List<FoodRecognizeVO> parseAndNormalize(String raw) {
        String json = stripCodeFence(raw);
        List<FoodRecognizeVO> result = new ArrayList<>();
        try {
            Map<String, Object> root = JSONUtil.toBean(json, Map.class);
            Object rawItems = root == null ? null : root.get("items");
            if (!(rawItems instanceof List<?> list)) {
                throw new IllegalArgumentException("items missing");
            }
            for (Object element : list) {
                if (result.size() >= MAX_ITEMS) {
                    break;
                }
                if (!(element instanceof Map<?, ?> map)) {
                    continue;
                }
                FoodRecognizeVO vo = normalizeItem(map);
                if (vo != null) {
                    result.add(vo);
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("unparseable model output: " + e.getMessage(), e);
        }
    }

    /**
     * 单项规范化：名称 trim 校验；脏宏量（负数/超界）置 0；kcal 按 4/4/9 重算覆盖
     */
    private FoodRecognizeVO normalizeItem(Map<?, ?> map) {
        Object nameValue = map.get("name");
        if (nameValue == null) {
            return null;
        }
        String name = String.valueOf(nameValue).trim();
        if (name.isEmpty()) {
            return null;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        double carb = sanitizeMacro(map.get("carb"));
        double protein = sanitizeMacro(map.get("protein"));
        double fat = sanitizeMacro(map.get("fat"));
        double kcal = Numbers.round1(BigDecimal.valueOf(carb * 4 + protein * 4 + fat * 9));

        FoodRecognizeVO vo = new FoodRecognizeVO();
        vo.setName(name);
        vo.setCarb(Numbers.round1(carb));
        vo.setProtein(Numbers.round1(protein));
        vo.setFat(Numbers.round1(fat));
        vo.setKcal(kcal);
        return vo;
    }

    /**
     * 宏量脏值清洗：无法解析/负数/超 100g 一律置 0
     */
    private double sanitizeMacro(Object value) {
        if (value == null) {
            return 0d;
        }
        try {
            double parsed = Double.parseDouble(String.valueOf(value).trim());
            if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed < 0 || parsed > MAX_MACRO_PER_100G) {
                return 0d;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    /**
     * 剥离 ```json ... ``` 或 ``` ... ``` 代码围栏
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
        // 容错：模型在 JSON 外附带文字时截取首个 { 到末个 }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text.trim();
    }

    private String resolveMimeType(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private long parseCount(String value) {
        if (StrUtil.isBlank(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 当前时刻到当日 24:00 的秒数（限流 key 跨天自动失效）
     */
    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        return Duration.between(now, LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)).getSeconds();
    }
}

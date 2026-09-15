package cn.zhenxinjian.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
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

    private static final String IMAGE_CACHE_KEY_PREFIX = "zhenxinjian:ocr:img:";

    private static final String DAILY_COUNT_KEY_PREFIX = "zhenxinjian:ocr:daily:";

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

        // 6-8. 调模型 → 解析 → 守恒重算（失败重试 1 次）
        String model = StrUtil.blankToDefault(configService.getValue(ProjectConfigKeyConstant.OCR_MODEL), DEFAULT_MODEL);
        int timeout = configService.getInt(ProjectConfigKeyConstant.OCR_TIMEOUT_SECONDS, 30);
        String mimeType = resolveMimeType(FileUtil.extName(file.getOriginalFilename()));
        AiImage image = new AiImage(Base64.getEncoder().encodeToString(imageBytes), mimeType);

        List<FoodRecognizeVO> items = invokeWithRetry(model, List.of(image), timeout);

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
     * 调用模型并解析，失败重试 1 次（第二次在提示词中强调只输出 JSON）
     */
    private List<FoodRecognizeVO> invokeWithRetry(String model, List<AiImage> image, int timeout) {
        try {
            String output = aiChatClient.chat(model, SYSTEM_PROMPT, image, timeout);
            return parseAndNormalize(output);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception first) {
            log.warn("视觉识别首次调用/解析失败，准备重试: {}", first.getMessage());
            try {
                String output = aiChatClient.chat(model, SYSTEM_PROMPT + "\n再次强调：只输出 JSON，不要任何解释或 Markdown 围栏。",
                        image, timeout);
                return parseAndNormalize(output);
            } catch (Exception second) {
                log.warn("视觉识别重试仍失败: {}", second.getMessage());
                throw new BusinessException(CommonConstant.AI_RECOGNIZE_FAIL_CODE, ExceptionConstant.AI_RECOGNIZE_FAIL);
            }
        }
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

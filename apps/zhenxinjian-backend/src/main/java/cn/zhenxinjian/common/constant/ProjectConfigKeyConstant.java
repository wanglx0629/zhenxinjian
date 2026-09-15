package cn.zhenxinjian.common.constant;

import java.util.regex.Pattern;

/**
 * 项目配置键常量（project_config.config_key 已知取值与格式约定）
 * 新 key 需在此登记常量供代码引用；组件凭据类（SECRET）以 change SQL 种子或后台新增落库
 * 作者: wanglx
 */
public final class ProjectConfigKeyConstant {

    private ProjectConfigKeyConstant() {
    }

    /** 配置键格式正则字符串（供 Bean Validation @Pattern 常量引用）：小写点分，至少两段（如 sensitive.filter.enabled） */
    public static final String KEY_REGEX = "^[a-z0-9]+(\\.[a-z0-9-]+)+$";

    /** 配置键格式：小写点分，至少两段（如 sensitive.filter.enabled） */
    public static final Pattern KEY_PATTERN = Pattern.compile(KEY_REGEX);

    /** 敏感词过滤总开关（布尔） */
    public static final String SENSITIVE_FILTER_ENABLED = "sensitive.filter.enabled";

    /** 敏感词追加词（JSON 数组） */
    public static final String SENSITIVE_FILTER_EXTRA_WORDS = "sensitive.filter.extra-words";

    /** 敏感词排除词——内置词库豁免（JSON 数组） */
    public static final String SENSITIVE_FILTER_EXCLUDE_WORDS = "sensitive.filter.exclude-words";

    /** 敏感词相关配置键前缀（修改后需触发词库重建） */
    public static final String SENSITIVE_FILTER_PREFIX = "sensitive.filter";

    /** OCR 视觉模型名（智谱） */
    public static final String OCR_MODEL = "ocr.model";

    /** OCR 识别总开关（布尔，false 拒识） */
    public static final String OCR_ENABLED = "ocr.enabled";

    /** OCR 每用户每日识别次数上限 */
    public static final String OCR_DAILY_LIMIT = "ocr.daily-limit";

    /** OCR 识别调用超时（秒） */
    public static final String OCR_TIMEOUT_SECONDS = "ocr.timeout-seconds";

    /** OCR 同图识别结果缓存时长（小时） */
    public static final String OCR_CACHE_TTL_HOURS = "ocr.cache-ttl-hours";

    /** OCR API 密钥（SECRET，AES 加密落库；种子在 change11 预置，真实值经 admin 后台录入） */
    public static final String OCR_API_KEY = "ocr.api-key";

    /** OCR 相关配置键前缀（普通配置，改后仅需失效缓存，无词库重建副作用） */
    public static final String OCR_PREFIX = "ocr";
}

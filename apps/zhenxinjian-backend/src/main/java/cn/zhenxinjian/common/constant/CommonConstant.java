package cn.zhenxinjian.common.constant;

/**
 * 通用常量
 * 作者: wanglx
 */
public final class CommonConstant {

    private CommonConstant() {
    }

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 失败状态码 */
    public static final int FAIL_CODE = 500;

    /** 未授权状态码 */
    public static final int UNAUTHORIZED_CODE = 401;

    /** 禁止访问状态码 */
    public static final int FORBIDDEN_CODE = 403;

    /** 请求过于频繁 / 登录锁定 */
    public static final int TOO_MANY_REQUESTS_CODE = 429;

    // ==================== 微信登录错误码（401xx） ====================

    /** 微信登录 code 无效或已使用 */
    public static final int WECHAT_CODE_INVALID_CODE = 40101;

    /** 微信服务不可用（code2session 超时/异常） */
    public static final int WECHAT_UNAVAILABLE_CODE = 40102;

    /** 微信 OpenID 绑定冲突 */
    public static final int WECHAT_OPENID_CONFLICT_CODE = 40103;

    // ==================== 游客模式错误码（402xx） ====================

    /** 游客体验已到期 */
    public static final int GUEST_EXPIRED_CODE = 40201;

    /** 游客标识无效 */
    public static final int GUEST_KEY_INVALID_CODE = 40202;

    // ==================== 身体数据错误码（403xx） ====================

    /** 目标体重须不大于当前体重（容差+0.1kg） */
    public static final int BODY_TARGET_WEIGHT_INVALID_CODE = 40301;

    /** 减脂缺口档位非法（仅200/300/400/500） */
    public static final int BODY_DEFICIT_INVALID_CODE = 40302;

    /** 脂肪系数非法（仅0.8/1.0） */
    public static final int BODY_CFC_INVALID_CODE = 40303;

    // ==================== 食物库错误码（404xx） ====================

    /** 自定义食物名称重复（同用户活跃唯一） */
    public static final int FOOD_NAME_DUPLICATE_CODE = 40401;

    /** 自定义食物宏量数值非法 */
    public static final int FOOD_MACRO_INVALID_CODE = 40402;

    /** 自定义食物能量与宏量不守恒（±10%） */
    public static final int FOOD_KCAL_MISMATCH_CODE = 40403;

    /** 食物不存在或不可操作（含内置食物写请求拒绝） */
    public static final int FOOD_NOT_FOUND_CODE = 40404;

    /** 无权限操作他人食物 */
    public static final int FOOD_NOT_OWNER_CODE = 40405;

    /** 试算克数超区间（1-10000） */
    public static final int FOOD_GRAMS_INVALID_CODE = 40406;

    // ==================== 饮食记录错误码（405xx） ====================

    /** 份量克数非法（>0 且 ≤5000） */
    public static final int DIET_AMOUNT_INVALID_CODE = 40501;

    /** 宏量数值非法（非负且在上限内） */
    public static final int DIET_MACRO_INVALID_CODE = 40502;

    /** 能量与宏量不守恒（±10%） */
    public static final int DIET_KCAL_MISMATCH_CODE = 40503;

    /** 记录不存在或不可操作（越权同码不泄露存在性） */
    public static final int DIET_RECORD_NOT_FOUND_CODE = 40504;

    /** 不允许未来日期 */
    public static final int DIET_FUTURE_DATE_CODE = 40505;

    /** 食物不存在或已删除（含他人自定义食物） */
    public static final int DIET_FOOD_INVALID_CODE = 40506;

    /** 餐别或来源非法 */
    public static final int DIET_MEAL_TYPE_INVALID_CODE = 40507;

    // ==================== 碳循环错误码（406xx） ====================

    /** 未建档（碳循环周期创建须先完成身体档案） */
    public static final int CYCLE_NOT_PROFILED_CODE = 40601;

    /** 模式或周期参数非法 */
    public static final int CYCLE_PARAM_INVALID_CODE = 40602;

    /** 周期不存在 */
    public static final int CYCLE_PLAN_NOT_FOUND_CODE = 40603;

    /** 运动日越界或过多 */
    public static final int CYCLE_SPORT_DAY_INVALID_CODE = 40604;

    /** 越权或不可操作（不泄露存在性） */
    public static final int CYCLE_NOT_OWNER_CODE = 40605;

    // ==================== 饮食提醒错误码（407xx） ====================

    /** 提醒时间格式非法（须 24 小时制 HH:mm） */
    public static final int REMINDER_TIME_INVALID_CODE = 40701;

    /** 订阅消息模板未配置（预留，运行时任务空转不报错） */
    public static final int REMINDER_TEMPLATE_MISSING_CODE = 40702;

    // ==================== 经期管理/体重记录错误码（408xx） ====================

    /** 经期设置非法（L/D 越界或起始日非法/未来） */
    public static final int MENSTRUAL_SETTING_INVALID_CODE = 40801;

    /** 体重记录非法（越界/未来日期） */
    public static final int WEIGHT_INVALID_CODE = 40802;

    // ==================== 管理后台/埋点错误码（409xx） ====================

    /** 统计参数非法（日期/天数越界） */
    public static final int STATS_PARAM_INVALID_CODE = 40902;

    /** 食物维护冲突（同名/非内置写操作） */
    public static final int ADMIN_FOOD_CONFLICT_CODE = 40903;

    /** 项目配置键已存在（活跃唯一） */
    public static final int CONFIG_KEY_DUPLICATE_CODE = 40904;

    /** 项目配置键格式非法（小写点分，至少两段） */
    public static final int CONFIG_KEY_INVALID_CODE = 40905;

    /** 项目配置不存在或不可操作 */
    public static final int CONFIG_NOT_FOUND_CODE = 40906;

    /** SECRET 值加密密钥未配置或长度非法 */
    public static final int CONFIG_SECRET_KEY_MISSING_CODE = 40907;

    /** SECRET 值解密失败（密钥不匹配/密文损坏） */
    public static final int CONFIG_SECRET_DECRYPT_FAIL_CODE = 40908;

    /** 内容包含敏感词（统一拦截文案） */
    public static final int CONTAINS_SENSITIVE_WORD_CODE = 40909;

    /** 埋点单批上报上限 */
    public static final int TRACK_BATCH_MAX_SIZE = 50;

    /** 埋点上报限流窗口（秒，固定窗口计数） */
    public static final long TRACK_RATE_WINDOW_SECONDS = 60L;

    /** 埋点扩展字段最大长度 */
    public static final int TRACK_EXTRA_MAX_LENGTH = 512;

    /** 食物搜索单页上限 */
    public static final long FOOD_SEARCH_MAX_SIZE = 50L;

    /** 试算克数下限 */
    public static final int FOOD_CALC_MIN_GRAMS = 1;

    /** 试算克数上限 */
    public static final int FOOD_CALC_MAX_GRAMS = 10000;

    /** 健康计算免责声明（不可移除，业务不变量 I3） */
    public static final String HEALTH_DISCLAIMER = "以上结果基于通用公式估算，仅作生活化减脂参考，不构成医疗建议；如有特殊健康状况请咨询医生";

    /** 用户类型：微信正式用户 */
    public static final String USER_TYPE_WECHAT = "WECHAT";

    /** 用户类型：游客 */
    public static final String USER_TYPE_GUEST = "GUEST";

    /** 游客体验天数 */
    public static final int GUEST_TRIAL_DAYS = 3;

    /** 游客到期后数据保留天数（窗口期内登录仍可迁移） */
    public static final int GUEST_GRACE_DAYS = 7;

    /** 默认页码 */
    public static final long DEFAULT_PAGE = 1L;

    /** 默认每页条数 */
    public static final long DEFAULT_SIZE = 10L;

    /** Authorization 请求头 */
    public static final String HEADER_AUTH = "Authorization";

    /** Bearer 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 普通用户角色 */
    public static final String ROLE_USER = "USER";

    // ==================== 用户账号通用常量 ====================
    // 账号状态取值统一走 UserStatusEnum 字典枚举（0冻结 1正常 2注销），不再此处定义散常量

    /** 微信绑定状态：已绑定 */
    public static final int WECHAT_BIND_YES = 1;

    // ==================== 微信侧内部识别常量（仅后端分类用，严禁透传前端） ====================

    /** 微信 errcode：code 无效或已使用（后端归类为系统码 40101，不外泄） */
    public static final int WECHAT_ERR_CODE_INVALID = 40029;

    /** 微信登录用户名前缀（后接 UUID） */
    public static final String USERNAME_PREFIX_WECHAT = "wx_";

    /** 游客用户名前缀（后接 UUID） */
    public static final String USERNAME_PREFIX_GUEST = "guest_";

    /** 微信新用户默认昵称 */
    public static final String NICKNAME_WECHAT_DEFAULT = "微信用户";

    /** 游客默认昵称 */
    public static final String NICKNAME_GUEST_DEFAULT = "游客";

    /** createBy 来源：微信登录建号 */
    public static final String CREATE_BY_WECHAT_LOGIN = "wechat-login";

    /** createBy 来源：游客签发 */
    public static final String CREATE_BY_GUEST_LOGIN = "guest-login";

    /** createBy 来源：系统操作 */
    public static final String CREATE_BY_SYSTEM = "system";

    /** 分页最大条数 */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 我的自定义食物列表上限（有界查询防无限积累） */
    public static final int CUSTOM_FOOD_MINE_LIMIT = 200;

    /** 搜索关键词最大长度 */
    public static final int MAX_KEYWORD_LENGTH = 64;

    /** 验证码UUID格式（32位十六进制） */
    public static final String CAPTCHA_UUID_PATTERN = "^[a-f0-9]{32}$";
}

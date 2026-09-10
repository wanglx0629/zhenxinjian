package cn.zhenxinjian.common.constant;

/**
 * 异常常量（错误码 + 错误信息）
 * 作者: wanglx
 */
public final class ExceptionConstant {

    private ExceptionConstant() {
    }

    // ==================== 认证授权 ====================

    /** 未登录 */
    public static final String NOT_LOGIN = "未登录";

    /** Token 无效或已过期 */
    public static final String TOKEN_INVALID = "Token无效或已过期";

    /** Token 已失效，请重新登录 */
    public static final String TOKEN_EXPIRED = "Token已失效，请重新登录";

    /** 用户名或密码错误 */
    public static final String BAD_CREDENTIALS = "用户名或密码错误";

    /** 权限不足 */
    public static final String ACCESS_DENIED = "权限不足";

    /** 账号已被禁用 */
    public static final String ACCOUNT_DISABLED = "账号已被禁用";

    // ==================== 微信登录（401xx） ====================

    /** 40101 微信登录 code 无效或已使用 */
    public static final String WECHAT_CODE_INVALID = "登录凭证无效，请重新登录";

    /** 40102 微信服务暂不可用 */
    public static final String WECHAT_UNAVAILABLE = "微信服务暂不可用，请稍后重试";

    /** 40103 微信 OpenID 绑定冲突 */
    public static final String WECHAT_OPENID_CONFLICT = "微信账号绑定异常，请重试";

    // ==================== 游客模式（402xx） ====================

    /** 40201 游客体验已到期 */
    public static final String GUEST_EXPIRED = "游客体验已到期，请授权微信后继续使用";

    /** 40202 游客标识无效 */
    public static final String GUEST_KEY_INVALID = "游客身份无效，请重新进入";

    // ==================== 身体数据（403xx） ====================

    /** 40301 目标体重须不大于当前体重 */
    public static final String BODY_TARGET_WEIGHT_INVALID = "目标体重须不大于当前体重";

    /** 40302 减脂缺口档位非法 */
    public static final String BODY_DEFICIT_INVALID = "减脂缺口仅支持200/300/400/500";

    /** 40303 脂肪系数非法 */
    public static final String BODY_CFC_INVALID = "脂肪系数仅支持0.8或1.0";

    // ==================== 食物库（404xx） ====================

    /** 40401 食物名称重复 */
    public static final String FOOD_NAME_DUPLICATE = "该名称的食物已存在，请换个名称";

    /** 40402 宏量数值非法 */
    public static final String FOOD_MACRO_INVALID = "碳水/蛋白/脂肪须在0-100之间";

    /** 40403 能量与宏量不守恒 */
    public static final String FOOD_KCAL_MISMATCH = "能量须约等于碳水×4+蛋白×4+脂肪×9（偏差10%以内）";

    /** 40404 食物不存在或不可操作 */
    public static final String FOOD_NOT_FOUND = "食物不存在或不可操作";

    /** 40405 无权限操作他人食物 */
    public static final String FOOD_NOT_OWNER = "无权操作他人的食物";

    /** 40406 试算克数超区间 */
    public static final String FOOD_GRAMS_INVALID = "试算克数须在1-10000之间";

    // ==================== 饮食记录（405xx） ====================

    /** 40501 份量克数非法 */
    public static final String DIET_AMOUNT_INVALID = "份量克数须大于0且不超过5000";

    /** 40502 宏量数值非法 */
    public static final String DIET_MACRO_INVALID = "名称与宏量数值非法，请检查输入";

    /** 40503 能量与宏量不守恒 */
    public static final String DIET_KCAL_MISMATCH = "能量须约等于碳水×4+蛋白×4+脂肪×9（偏差10%以内）";

    /** 40504 记录不存在或不可操作 */
    public static final String DIET_RECORD_NOT_FOUND = "记录不存在或不可操作";

    /** 40505 不允许未来日期 */
    public static final String DIET_FUTURE_DATE = "不能记录或查看未来日期";

    /** 40506 食物不存在或已删除 */
    public static final String DIET_FOOD_INVALID = "食物不存在或已删除，请重新选择";

    /** 40507 餐别或来源非法 */
    public static final String DIET_MEAL_TYPE_INVALID = "餐别或来源非法";

    // ==================== 碳循环（406xx） ====================

    /** 40601 未建档 */
    public static final String CYCLE_NOT_PROFILED = "请先完成身体档案录入";

    /** 40602 模式或周期参数非法 */
    public static final String CYCLE_PARAM_INVALID = "模式或周期参数非法";

    /** 40603 周期不存在 */
    public static final String CYCLE_PLAN_NOT_FOUND = "周期不存在";

    /** 40604 运动日越界或过多 */
    public static final String CYCLE_SPORT_DAY_INVALID = "运动日须落在周期内";

    /** 40605 越权或不可操作 */
    public static final String CYCLE_NOT_OWNER = "周期不存在或不可操作";

    // ==================== 饮食提醒（407xx） ====================

    /** 40701 提醒时间格式非法 */
    public static final String REMINDER_TIME_INVALID = "提醒时间格式非法";

    /** 40702 订阅消息模板未配置 */
    public static final String REMINDER_TEMPLATE_MISSING = "订阅消息模板未配置";

    // ==================== 用户业务 ====================

    /** 用户名已存在 */
    public static final String USERNAME_EXISTS = "用户名已存在";

    /** 用户不存在 */
    public static final String USER_NOT_FOUND = "用户不存在";

    /** 用户ID不能为空 */
    public static final String USER_ID_REQUIRED = "用户ID不能为空";

    /** 密码不能为空 */
    public static final String PASSWORD_REQUIRED = "密码不能为空";

    // ==================== 验证码 ====================

    /** 验证码错误或已过期 */
    public static final String CAPTCHA_INVALID = "验证码错误或已过期";

    /** 验证码生成失败 */
    public static final String CAPTCHA_GENERATE_FAIL = "验证码生成失败";

    // ==================== 参数校验 ====================

    /** 页码最小为1 */
    public static final String PAGE_MIN = "页码最小为1";

    /** 每页条数最小为1 */
    public static final String SIZE_MIN = "每页条数最小为1";

    /** 每页条数超出上限 */
    public static final String SIZE_MAX = "每页条数最大为100";

    /** 搜索关键词过长 */
    public static final String KEYWORD_TOO_LONG = "搜索关键词过长";

    /** 用户名不能为空 */
    public static final String USERNAME_REQUIRED = "用户名不能为空";

    /** 用户名长度3-32位 */
    public static final String USERNAME_LENGTH = "用户名长度3-32位";

    /** 密码长度8-32位 */
    public static final String PASSWORD_LENGTH = "密码长度8-32位";

    /** 验证码不能为空 */
    public static final String CAPTCHA_REQUIRED = "验证码不能为空";

    /** 验证码标识不能为空 */
    public static final String CAPTCHA_UUID_REQUIRED = "验证码标识不能为空";

    /** 邮箱格式不正确 */
    public static final String EMAIL_FORMAT_INVALID = "邮箱格式不正确";

    /** 验证码标识格式不正确 */
    public static final String CAPTCHA_UUID_INVALID = "验证码标识格式不正确";

    /** 登录失败次数过多 */
    public static final String LOGIN_LOCKED = "登录失败次数过多，请稍后再试";

    /** 角色不合法 */
    public static final String ROLE_INVALID = "角色不合法";

    // ==================== 文件存储 ====================

    /** 上传文件不能为空 */
    public static final String FILE_EMPTY = "上传文件不能为空";

    /** 文件体积超出限制 */
    public static final String FILE_TOO_LARGE = "文件体积超出限制";

    /** 文件类型不允许 */
    public static final String FILE_TYPE_NOT_ALLOWED = "文件类型不允许";

    /** 文件上传失败 */
    public static final String FILE_UPLOAD_FAIL = "文件上传失败，请稍后重试";

    /** 文件删除失败 */
    public static final String FILE_DELETE_FAIL = "文件删除失败";

    /** 对象Key不能为空 */
    public static final String FILE_KEY_REQUIRED = "文件标识不能为空";

    // ==================== 缓存 ====================

    /** 缓存值过大，拒绝写入 */
    public static final String CACHE_VALUE_TOO_LARGE = "缓存数据过大，请缩小查询范围";

    // ==================== 通用 ====================

    /** 参数校验失败 */
    public static final String PARAM_VALID_FAIL = "参数校验失败";

    /** 请求方法不支持 */
    public static final String METHOD_NOT_ALLOWED = "请求方法不支持";

    /** 请求体无法解析 */
    public static final String BAD_REQUEST_BODY = "请求体格式错误";

    /** 缺少必要参数 */
    public static final String MISSING_PARAM = "缺少必要参数";

    /** 系统繁忙，请稍后重试 */
    public static final String SYSTEM_BUSY = "系统繁忙，请稍后重试";

}

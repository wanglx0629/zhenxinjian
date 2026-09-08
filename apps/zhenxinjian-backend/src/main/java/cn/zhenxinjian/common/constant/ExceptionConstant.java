package cn.zhenxinjian.common.constant;

/**
 * 异常常量（错误码 + 错误信息）
 * 作者: luote (luote) - https://luote996.cn
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

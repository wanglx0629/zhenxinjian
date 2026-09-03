package cn.zhenxinjian.common.constant;

/**
 * 通用常量
 * 作者: luote (luote) - https://luote996.cn
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

    /** 分页最大条数 */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 搜索关键词最大长度 */
    public static final int MAX_KEYWORD_LENGTH = 64;

    /** 验证码UUID格式（32位十六进制） */
    public static final String CAPTCHA_UUID_PATTERN = "^[a-f0-9]{32}$";

    /**
     * 登录失败锁定阈值默认值（实际以 zhenxinjian.redis.login-fail-max 为准，默认 10）
     */
    public static final int LOGIN_FAIL_MAX = 10;
}

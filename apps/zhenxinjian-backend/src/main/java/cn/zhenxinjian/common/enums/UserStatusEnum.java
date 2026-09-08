package cn.zhenxinjian.common.enums;

/**
 * 用户账号状态字典枚举
 * 作者: wanglx
 */
public enum UserStatusEnum {

    /** 冻结：禁止登录（管理员处置 / 风控锁定） */
    FROZEN(0, "冻结"),

    /** 正常：允许登录 */
    NORMAL(1, "正常"),

    /** 注销：用户主动注销（逻辑等价冻结，保留数据供审计） */
    DEACTIVATED(2, "注销");

    /** 状态码（对应 users.status 列） */
    private final Integer code;

    /** 状态描述 */
    private final String desc;

    UserStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按 code 查询枚举
     *
     * @param code 状态码
     * @return 对应枚举；code 无效时返回 null
     */
    public static UserStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

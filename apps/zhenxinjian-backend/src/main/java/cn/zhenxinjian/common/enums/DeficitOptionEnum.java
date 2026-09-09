package cn.zhenxinjian.common.enums;

/**
 * 减脂缺口档位字典枚举（对应 user_body.deficit 列，基准热量 = TDEE − 缺口）
 * 作者: wanglx
 */
public enum DeficitOptionEnum {

    /** 温和：200 kcal（默认） */
    GENTLE(200, "温和"),

    /** 标准：300 kcal */
    STANDARD(300, "标准"),

    /** 积极：400 kcal */
    ACTIVE(400, "积极"),

    /** 激进：500 kcal */
    AGGRESSIVE(500, "激进");

    /** 缺口值 kcal（对应 user_body.deficit 列） */
    private final Integer code;

    /** 档位描述 */
    private final String desc;

    DeficitOptionEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 默认档位：温和 200 kcal */
    public static DeficitOptionEnum defaultOption() {
        return GENTLE;
    }

    /**
     * 按缺口值查询枚举
     *
     * @param code 缺口值 kcal
     * @return 对应枚举；code 无效时返回 null
     */
    public static DeficitOptionEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeficitOptionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

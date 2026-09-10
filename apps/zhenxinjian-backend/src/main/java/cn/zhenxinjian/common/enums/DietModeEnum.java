package cn.zhenxinjian.common.enums;

/**
 * 减脂模式字典枚举（对应 user_body.mode 列）
 * 作者: wanglx
 */
public enum DietModeEnum {

    /** 532 碳水渐降 */
    TAPER_532(1, "532"),

    /** 碳循环 */
    CARB_CYCLE(2, "碳循环");

    /** 模式编码（对应 user_body.mode 列） */
    private final Integer code;

    /** 模式描述 */
    private final String desc;

    DietModeEnum(Integer code, String desc) {
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
     * 按编码查询枚举
     *
     * @param code 模式编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static DietModeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (DietModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

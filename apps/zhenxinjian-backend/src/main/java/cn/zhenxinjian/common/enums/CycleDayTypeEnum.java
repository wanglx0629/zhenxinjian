package cn.zhenxinjian.common.enums;

/**
 * 碳循环日型字典枚举（对应 carb_cycle_day.day_type 列）
 * 作者: wanglx
 */
public enum CycleDayTypeEnum {

    /** 高碳日 */
    HIGH(1, "高碳日"),

    /** 中碳日 */
    MEDIUM(2, "中碳日"),

    /** 低碳日 */
    LOW(3, "低碳日");

    /** 日型编码（对应 carb_cycle_day.day_type 列） */
    private final Integer code;

    /** 日型描述 */
    private final String desc;

    CycleDayTypeEnum(Integer code, String desc) {
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
     * @param code 日型编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static CycleDayTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CycleDayTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

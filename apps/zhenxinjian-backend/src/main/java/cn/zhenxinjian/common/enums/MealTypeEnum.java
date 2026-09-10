package cn.zhenxinjian.common.enums;

/**
 * 餐别字典枚举（对应 diet_records.meal_type 列）
 * 作者: wanglx
 */
public enum MealTypeEnum {

    /** 早餐 */
    BREAKFAST(1, "早餐"),

    /** 午餐 */
    LUNCH(2, "午餐"),

    /** 晚餐 */
    DINNER(3, "晚餐"),

    /** 加餐 */
    SNACK(4, "加餐");

    /** 餐别编码（对应 diet_records.meal_type 列） */
    private final Integer code;

    /** 餐别描述 */
    private final String desc;

    MealTypeEnum(Integer code, String desc) {
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
     * @param code 餐别编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static MealTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (MealTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

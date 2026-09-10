package cn.zhenxinjian.common.enums;

/**
 * 饮食记录来源字典枚举（对应 diet_records.source 列）
 * 作者: wanglx
 */
public enum DietRecordSourceEnum {

    /** 内置食物（200 条基础库） */
    BUILT_IN_FOOD(1, "内置食物"),

    /** 自定义食物（用户创建） */
    CUSTOM_FOOD(2, "自定义食物"),

    /** 手动输入（保底，不选食物直接填三宏热量） */
    MANUAL(3, "手动输入");

    /** 来源编码（对应 diet_records.source 列） */
    private final Integer code;

    /** 来源描述 */
    private final String desc;

    DietRecordSourceEnum(Integer code, String desc) {
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
     * @param code 来源编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static DietRecordSourceEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (DietRecordSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

package cn.zhenxinjian.common.enums;

/**
 * 碳循环周期状态字典枚举（对应 carb_cycle_plan.status 列）
 * 作者: wanglx
 */
public enum CyclePlanStatusEnum {

    /** 进行中 */
    ACTIVE(1, "进行中"),

    /** 已完成 */
    FINISHED(2, "已完成"),

    /** 已终止 */
    TERMINATED(3, "已终止");

    /** 状态编码（对应 carb_cycle_plan.status 列） */
    private final Integer code;

    /** 状态描述 */
    private final String desc;

    CyclePlanStatusEnum(Integer code, String desc) {
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
     * @param code 状态编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static CyclePlanStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CyclePlanStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

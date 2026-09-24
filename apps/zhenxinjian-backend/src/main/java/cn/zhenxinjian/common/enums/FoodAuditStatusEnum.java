package cn.zhenxinjian.common.enums;

/**
 * 食物审核状态字典枚举（对应 foods.audit_status 列）
 * 作者: wanglx
 */
public enum FoodAuditStatusEnum {

    /** 待审核（用户提交/重提后进入，私有可编辑可记录） */
    PENDING(0, "待审核"),

    /** 已通过（公共食物，全员可见，原作者只读） */
    APPROVED(1, "已通过"),

    /** 已驳回（私有可编辑，含驳回原因，可改后重提） */
    REJECTED(2, "已驳回"),

    /** 无需审核（内置/TFDA 基础食物，恒不进审核流） */
    NOT_REQUIRED(3, "无需审核");

    /** 审核状态编码（对应 foods.audit_status 列） */
    private final Integer code;

    /** 审核状态描述 */
    private final String desc;

    FoodAuditStatusEnum(Integer code, String desc) {
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
     * @param code 审核状态编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static FoodAuditStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FoodAuditStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

package cn.zhenxinjian.common.enums;

/**
 * 食物审核动作字典枚举（对应 food_audit_log.action 列，VARCHAR 存字符串编码）
 * 作者: wanglx
 */
public enum FoodAuditActionEnum {

    /** 用户提交新食物（进入待审核） */
    SUBMIT("SUBMIT", "提交"),

    /** 系统触发 AI 营养合理性校验（operator 为系统 0） */
    AI_CHECK("AI_CHECK", "AI校验"),

    /** 管理员审核通过（转公共食物） */
    APPROVE("APPROVE", "通过"),

    /** 管理员驳回（必填原因，退回私有可改） */
    REJECT("REJECT", "驳回"),

    /** 用户修改已驳回食物后重新提交（状态回待审核） */
    RESUBMIT("RESUBMIT", "重新提交"),

    /** 管理员修正数据后通过（按修正值落库并转公共） */
    ADMIN_FIX("ADMIN_FIX", "管理员修正");

    /** 动作编码（对应 food_audit_log.action 列） */
    private final String code;

    /** 动作描述 */
    private final String desc;

    FoodAuditActionEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码查询枚举
     *
     * @param code 动作编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static FoodAuditActionEnum of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (FoodAuditActionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

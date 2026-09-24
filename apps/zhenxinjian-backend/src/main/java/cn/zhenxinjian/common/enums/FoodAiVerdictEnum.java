package cn.zhenxinjian.common.enums;

/**
 * 食物 AI 校验结论字典枚举（对应 foods.ai_verdict / food_audit_log.ai_verdict 列，VARCHAR 存字符串编码）
 * 作者: wanglx
 */
public enum FoodAiVerdictEnum {

    /** AI 认为名称/品类与营养值匹配合理 */
    PASS("pass", "合理"),

    /** AI 认为存在可疑点（供人工重点复核） */
    SUSPECT("suspect", "存疑"),

    /** AI 认为名称/品类与营养值明显不符 */
    REJECT("reject", "明显不符"),

    /** AI 未校验或校验降级（未配置/超时/限流/坏 JSON，不阻塞提交） */
    NONE("none", "未校验");

    /** 结论编码（对应 ai_verdict 列） */
    private final String code;

    /** 结论描述 */
    private final String desc;

    FoodAiVerdictEnum(String code, String desc) {
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
     * @param code 结论编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static FoodAiVerdictEnum of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (FoodAiVerdictEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

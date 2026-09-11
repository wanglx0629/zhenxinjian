package cn.zhenxinjian.common.enums;

/**
 * 调碳动作字典枚举（对应 adjust_log.action 列）
 * 作者: wanglx
 */
public enum AdjustActionEnum {

    /** 下调 */
    DOWN(1, "下调"),

    /** 恢复 */
    RESTORE(2, "恢复");

    /** 动作编码（对应 adjust_log.action 列） */
    private final Integer code;

    /** 动作描述 */
    private final String desc;

    AdjustActionEnum(Integer code, String desc) {
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
     * @param code 动作编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static AdjustActionEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AdjustActionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
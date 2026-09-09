package cn.zhenxinjian.common.enums;

/**
 * 活动系数档位字典枚举（对应 user_body.activity_level 列，系数见 factor）
 * 作者: wanglx
 */
public enum ActivityLevelEnum {

    /** 久坐：几乎不运动 */
    SEDENTARY(1, 1.2, "久坐（几乎不运动）"),

    /** 轻度：每周运动1-3次 */
    LIGHT(2, 1.375, "轻度（每周运动1-3次）"),

    /** 中度：每周运动3-5次 */
    MODERATE(3, 1.55, "中度（每周运动3-5次）"),

    /** 高度：每周运动6-7次 */
    HIGH(4, 1.725, "高度（每周运动6-7次）");

    /** 档位码（对应 user_body.activity_level 列） */
    private final Integer code;

    /** 活动系数（TDEE = BMR × factor） */
    private final Double factor;

    /** 档位描述 */
    private final String desc;

    ActivityLevelEnum(Integer code, Double factor, String desc) {
        this.code = code;
        this.factor = factor;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public Double getFactor() {
        return factor;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按档位码查询枚举
     *
     * @param code 档位码
     * @return 对应枚举；code 无效时返回 null
     */
    public static ActivityLevelEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ActivityLevelEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

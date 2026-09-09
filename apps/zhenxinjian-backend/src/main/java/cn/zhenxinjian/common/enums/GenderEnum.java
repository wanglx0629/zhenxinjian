package cn.zhenxinjian.common.enums;

/**
 * 性别字典枚举（业务口径，对应 user_body.gender 列）
 * 作者: wanglx
 */
public enum GenderEnum {

    /** 男 */
    MALE(1, "男"),

    /** 女 */
    FEMALE(2, "女");

    /** 性别码（对应 user_body.gender 列） */
    private final Integer code;

    /** 性别描述 */
    private final String desc;

    GenderEnum(Integer code, String desc) {
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
     * 按 code 查询枚举
     *
     * @param code 性别码
     * @return 对应枚举；code 无效时返回 null
     */
    public static GenderEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (GenderEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

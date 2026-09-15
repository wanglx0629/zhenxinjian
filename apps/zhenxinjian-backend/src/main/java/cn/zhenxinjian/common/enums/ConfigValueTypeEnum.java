package cn.zhenxinjian.common.enums;

/**
 * 项目配置值类型字典枚举（对应 project_config.value_type 列）
 * 作者: wanglx
 */
public enum ConfigValueTypeEnum {

    /** 字符串（原样存取） */
    STRING(1, "字符串"),

    /** 数字（整数） */
    NUMBER(2, "数字"),

    /** 布尔（true/false） */
    BOOLEAN(3, "布尔"),

    /** JSON（数组或对象，如敏感词追加/排除词列表） */
    JSON(4, "JSON"),

    /** 密文（组件凭据类，AES 加密落库带 enc: 前缀，下发脱敏） */
    SECRET(5, "密文");

    /** 类型编码（对应 project_config.value_type 列） */
    private final Integer code;

    /** 类型描述 */
    private final String desc;

    ConfigValueTypeEnum(Integer code, String desc) {
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
     * @param code 类型编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static ConfigValueTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConfigValueTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

package cn.zhenxinjian.common.enums;

/**
 * 食物图片来源字典枚举（对应 food_images.source 列）
 * 作者: wanglx
 */
public enum FoodImageSourceEnum {

    /** 启动预热（FoodImageInitializer 从 classpath food-images/ 上传对象存储） */
    PREHEAT(1, "启动预热"),

    /** 人工上传（后台维护） */
    MANUAL(2, "人工上传");

    /** 来源编码（对应 food_images.source 列） */
    private final Integer code;

    /** 来源描述 */
    private final String desc;

    FoodImageSourceEnum(Integer code, String desc) {
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
    public static FoodImageSourceEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FoodImageSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

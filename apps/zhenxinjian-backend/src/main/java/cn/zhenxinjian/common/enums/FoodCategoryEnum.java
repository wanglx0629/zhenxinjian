package cn.zhenxinjian.common.enums;

/**
 * 食物分类字典枚举（对应 foods.category_code/category_name 列，10 大分类，顺序固定 01–10）
 * 作者: wanglx
 */
public enum FoodCategoryEnum {

    /** 01 谷薯杂豆·主食 */
    STAPLE("01", "谷薯杂豆·主食"),

    /** 02 畜禽肉及制品 */
    MEAT("02", "畜禽肉及制品"),

    /** 03 蛋奶及制品 */
    EGG_DAIRY("03", "蛋奶及制品"),

    /** 04 水产及制品 */
    SEAFOOD("04", "水产及制品"),

    /** 05 大豆及制品 */
    BEAN("05", "大豆及制品"),

    /** 06 蔬菜 */
    VEGETABLE("06", "蔬菜"),

    /** 07 菌藻 */
    FUNGUS_ALGAE("07", "菌藻"),

    /** 08 水果 */
    FRUIT("08", "水果"),

    /** 09 坚果·种子 */
    NUT_SEED("09", "坚果·种子"),

    /** 10 油脂·调味·饮品 */
    OIL_CONDIMENT_DRINK("10", "油脂·调味·饮品");

    /** 分类编号（对应 foods.category_code 列） */
    private final String code;

    /** 分类名称（对应 foods.category_name 列） */
    private final String desc;

    FoodCategoryEnum(String code, String desc) {
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
     * 按分类编号查询枚举
     *
     * @param code 分类编号（01–10）
     * @return 对应枚举；code 无效时返回 null
     */
    public static FoodCategoryEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (FoodCategoryEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

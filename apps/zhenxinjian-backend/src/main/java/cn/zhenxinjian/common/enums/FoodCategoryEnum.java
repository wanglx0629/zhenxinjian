package cn.zhenxinjian.common.enums;

/**
 * 食物分类字典枚举（对应 foods.category_code/category_name 列，TFDA 口径 18 大类，顺序固定 01–18）
 * 作者: wanglx
 *
 * 口径: TFDA 食品營養成分資料庫原始 18 大类（类名繁转简，官方口径原样保留）；
 * 顺序按饮食记录产品常用习惯排列（主食→蛋白→蔬果→加工），见 openspec/changes/food-category-tfda-18 design D1
 */
public enum FoodCategoryEnum {

    /** 01 淀粉类 */
    STARCH("01", "淀粉类"),

    /** 02 谷物类 */
    GRAIN("02", "谷物类"),

    /** 03 肉类 */
    MEAT("03", "肉类"),

    /** 04 鱼贝类 */
    SEAFOOD("04", "鱼贝类"),

    /** 05 蛋类 */
    EGG("05", "蛋类"),

    /** 06 乳品类 */
    DAIRY("06", "乳品类"),

    /** 07 豆类 */
    BEAN("07", "豆类"),

    /** 08 蔬菜类 */
    VEGETABLE("08", "蔬菜类"),

    /** 09 菇类 */
    MUSHROOM("09", "菇类"),

    /** 10 藻类 */
    ALGA("10", "藻类"),

    /** 11 水果类 */
    FRUIT("11", "水果类"),

    /** 12 坚果及种子类 */
    NUT_SEED("12", "坚果及种子类"),

    /** 13 油脂类 */
    OIL("13", "油脂类"),

    /** 14 糖类 */
    SUGAR("14", "糖类"),

    /** 15 糕饼点心类 */
    PASTRY("15", "糕饼点心类"),

    /** 16 调味料及香辛料类 */
    CONDIMENT("16", "调味料及香辛料类"),

    /** 17 饮料类 */
    BEVERAGE("17", "饮料类"),

    /** 18 加工调理食品及其他类 */
    PROCESSED("18", "加工调理食品及其他类");

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
     * @param code 分类编号（01–18）
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

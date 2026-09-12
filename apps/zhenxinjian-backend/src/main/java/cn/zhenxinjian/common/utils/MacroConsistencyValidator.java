package cn.zhenxinjian.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 宏量-能量守恒校验器（业务不变量 I11 单一真源）
 * 口径：|碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%
 * 消费方：DietRecordService / CustomFoodService / AdminFoodService
 * 作者: wanglx
 */
public final class MacroConsistencyValidator {

    /** 能量守恒允许偏差比例（10%） */
    public static final BigDecimal TOLERANCE_RATIO = new BigDecimal("0.10");

    /** 守恒计算系数：碳水/蛋白 4 kcal/g */
    public static final BigDecimal CARB_PROTEIN_FACTOR = new BigDecimal("4");

    /** 守恒计算系数：脂肪 9 kcal/g */
    public static final BigDecimal FAT_FACTOR = new BigDecimal("9");

    /** 每 100g 单项宏量上限 g（食物库口径） */
    public static final BigDecimal PER_100G_MACRO_MAX = new BigDecimal("100");

    /** 每 100g 能量上限 kcal（食物库口径，与建表一致） */
    public static final int PER_100G_KCAL_MAX = 900;

    private MacroConsistencyValidator() {
    }

    /**
     * 宏量理论产能量：碳水×4 + 蛋白×4 + 脂肪×9
     */
    public static BigDecimal expectedKcal(BigDecimal carb, BigDecimal protein, BigDecimal fat) {
        return carb.multiply(CARB_PROTEIN_FACTOR)
                .add(protein.multiply(CARB_PROTEIN_FACTOR))
                .add(fat.multiply(FAT_FACTOR));
    }

    /**
     * 守恒判定：宏量理论产能量与标注能量的相对偏差 ≤ 10%
     *
     * @param carb    碳水 g
     * @param protein 蛋白 g
     * @param fat     脂肪 g
     * @param kcal    标注能量 kcal
     * @return true 在容差内
     */
    public static boolean withinTolerance(BigDecimal carb, BigDecimal protein, BigDecimal fat, int kcal) {
        BigDecimal diff = expectedKcal(carb, protein, fat).subtract(BigDecimal.valueOf(kcal)).abs();
        BigDecimal base = BigDecimal.valueOf(Math.max(kcal, 1));
        return diff.divide(base, 4, RoundingMode.HALF_UP).compareTo(TOLERANCE_RATIO) <= 0;
    }
}

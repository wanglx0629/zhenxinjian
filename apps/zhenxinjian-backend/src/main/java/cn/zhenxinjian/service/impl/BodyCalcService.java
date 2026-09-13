package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.DeficitOptionEnum;
import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.utils.Numbers;
import org.springframework.stereotype.Component;

/**
 * 身体核心计算服务（全产品唯一计算真源）
 * 口径：BMR（Mifflin-St Jeor 男+5/女-161）→ TDEE（BMR精确值×活动系数）→ 基准热量（TDEE取整−缺口）
 *       → 532 目标宏量（碳50%/蛋30%/脂20%，产能系数 4/4/9）
 * 取整：热量类四舍五入取整 kcal；克数保留 1 位小数（HALF_UP）
 * 作者: wanglx
 */
@Component
public class BodyCalcService {

    /** 低热量安全下限 kcal：女 */
    private static final int LOW_KCAL_FEMALE = 1200;

    /** 低热量安全下限 kcal：男 */
    private static final int LOW_KCAL_MALE = 1500;

    /** 532 宏量占比（固定写死，见业务规则速查） */
    private static final double MACRO_CARB_RATIO = 0.5;
    private static final double MACRO_PROTEIN_RATIO = 0.3;
    private static final double MACRO_FAT_RATIO = 0.2;

    /** 产能系数 kcal/g（4/4/9） */
    private static final double KCAL_PER_G_CARB = 4d;
    private static final double KCAL_PER_G_PROTEIN = 4d;
    private static final double KCAL_PER_G_FAT = 9d;

    /**
     * 计算结果快照
     *
     * @param bmr          基础代谢 kcal（取整）
     * @param tdee         每日总消耗 kcal（取整）
     * @param targetKcal   基准热量 kcal（TDEE − 缺口，取整）
     * @param carb         目标碳水 g（1 位小数）
     * @param protein      目标蛋白 g（1 位小数）
     * @param fat          目标脂肪 g（1 位小数）
     * @param lowKcalRisk  低热量风险标记（女 <1200 / 男 <1500，仅提示不阻断）
     */
    public record CalcResult(int bmr, int tdee, int targetKcal,
                             double carb, double protein, double fat,
                             boolean lowKcalRisk) {
    }

    /**
     * 全量计算：BMR → TDEE → 基准热量 → 532 宏量 + 风险标记
     *
     * @param gender         性别枚举
     * @param age            年龄
     * @param height         身高 cm
     * @param weight         体重 kg
     * @param activityFactor 活动系数（1.2/1.375/1.55/1.725）
     * @param deficit        减脂缺口 kcal（200/300/400/500）
     * @return 计算结果快照
     */
    public CalcResult compute(GenderEnum gender, int age, double height, double weight,
                              double activityFactor, int deficit) {
        double bmrRaw = bmrRaw(gender, weight, height, age);
        int bmr = roundKcal(bmrRaw);
        int tdee = roundKcal(bmrRaw * activityFactor);
        int targetKcal = tdee - deficit;
        double carb = Numbers.round1(targetKcal * MACRO_CARB_RATIO / KCAL_PER_G_CARB);
        double protein = Numbers.round1(targetKcal * MACRO_PROTEIN_RATIO / KCAL_PER_G_PROTEIN);
        double fat = Numbers.round1(targetKcal * MACRO_FAT_RATIO / KCAL_PER_G_FAT);
        return new CalcResult(bmr, tdee, targetKcal, carb, protein, fat,
                isLowKcalRisk(gender, targetKcal));
    }

    /**
     * 低热量风险判定：基准热量低于安全下限（女 1200 / 男 1500）
     *
     * @param gender     性别枚举
     * @param targetKcal 基准热量 kcal
     * @return true=存在风险（仅提示，不阻断）
     */
    public boolean isLowKcalRisk(GenderEnum gender, int targetKcal) {
        return gender == GenderEnum.FEMALE ? targetKcal < LOW_KCAL_FEMALE : targetKcal < LOW_KCAL_MALE;
    }

    /** BMR 精确值 · Mifflin-St Jeor（男 +5 / 女 −161，不取整，供 TDEE 连算防口径漂移） */
    private double bmrRaw(GenderEnum gender, double weight, double height, int age) {
        double base = 10 * weight + 6.25 * height - 5d * age;
        return gender == GenderEnum.MALE ? base + 5 : base - 161;
    }

    /** 热量取整 kcal（四舍五入） */
    private int roundKcal(double v) {
        return (int) Math.round(v);
    }

    /**
     * 缺口缺省值（未选择时默认温和 200）
     *
     * @param deficit 入参缺口（可空）
     * @return 有效缺口枚举；非法值返回 null（由调用方拒绝）
     */
    public DeficitOptionEnum resolveDeficit(Integer deficit) {
        return deficit == null ? DeficitOptionEnum.defaultOption() : DeficitOptionEnum.of(deficit);
    }
}

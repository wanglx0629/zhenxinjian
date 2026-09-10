package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.CycleDayTypeEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 碳循环计算引擎（图片公式唯一真源）
 * 作者: wanglx
 *
 * 口径（design §3）：
 * 碳水池 = 目标体重 × 2.5 × N；脂肪池 = 目标体重 × cfc × N；每日蛋白 = 当前体重 × 1.5
 * 日型分配除数 = 权重 × N/7：高(碳50%/脂15%, w2.0) 中(碳35%/脂35%, w2.2) 低(碳15%/脂50%, w2.0)
 * —— 同一日型单日目标与 N 无关；N=7 退化 PRD 原式（金标 240.6/153.1/72.2 逐位命中）
 * 日型排布：模板 [高·中·低·低·中·高·低] 循环；运动日按日序升序抢占高碳位→中碳位
 * 合计 = 池 × K（K碳≈1.043 / K脂≈1.218），逐日四舍五入漂移 ≤0.7g 天然落在 ±1g（D4 免调尾）
 */
@Service
public class CycleCalcService {

    /** 碳水池系数：目标体重 × 2.5 × N */
    private static final BigDecimal CARB_POOL_FACTOR = new BigDecimal("2.5");

    /** 每日蛋白系数：当前体重 × 1.5 */
    private static final BigDecimal PROTEIN_FACTOR = new BigDecimal("1.5");

    /** 模板周期基准天数（除数归一基准） */
    private static final BigDecimal TEMPLATE_BASE_DAYS = new BigDecimal("7");

    /** 产能系数：碳水/蛋白 4、脂肪 9 */
    private static final BigDecimal CARB_PROTEIN_KCAL = new BigDecimal("4");
    private static final BigDecimal FAT_KCAL = new BigDecimal("9");

    /** 默认日型轮播模板（7 天一轮） */
    private static final CycleDayTypeEnum[] TEMPLATE = {
            CycleDayTypeEnum.HIGH, CycleDayTypeEnum.MEDIUM, CycleDayTypeEnum.LOW, CycleDayTypeEnum.LOW,
            CycleDayTypeEnum.MEDIUM, CycleDayTypeEnum.HIGH, CycleDayTypeEnum.LOW};

    /** 日型参数：碳水占比 / 脂肪占比 / 天数权重 */
    private record DayTypeParam(BigDecimal carbPct, BigDecimal fatPct, BigDecimal weight) {
    }

    private static DayTypeParam paramOf(CycleDayTypeEnum type) {
        return switch (type) {
            case HIGH -> new DayTypeParam(new BigDecimal("0.50"), new BigDecimal("0.15"), new BigDecimal("2.0"));
            case MEDIUM -> new DayTypeParam(new BigDecimal("0.35"), new BigDecimal("0.35"), new BigDecimal("2.2"));
            case LOW -> new DayTypeParam(new BigDecimal("0.15"), new BigDecimal("0.50"), new BigDecimal("2.0"));
        };
    }

    /** 单日计算结果 */
    public record DayResult(int dayIndex, CycleDayTypeEnum dayType, boolean sport,
                            BigDecimal carbG, BigDecimal proteinG, BigDecimal fatG, int kcal) {
    }

    /** 周期计算结果：三大池 + 逐日列表 */
    public record CycleResult(BigDecimal carbPool, BigDecimal fatPool, BigDecimal dailyProtein,
                              List<DayResult> days) {
    }

    /**
     * 计算碳循环周期（图片公式）
     *
     * @param currentWeight 当前体重 kg（每日蛋白口径）
     * @param targetWeight  目标体重 kg（碳/脂池口径）
     * @param cycleDays     周期天数 N（7-14，调用方已校验）
     * @param cfc           脂肪系数 0.8/1.0（调用方已校验）
     * @param sportDays     运动日日序集合（1..N，可空；调用方已校验越界/超量）
     * @return 周期计算结果
     */
    public CycleResult compute(BigDecimal currentWeight, BigDecimal targetWeight,
                               int cycleDays, BigDecimal cfc, Set<Integer> sportDays) {
        BigDecimal n = BigDecimal.valueOf(cycleDays);
        BigDecimal carbPool = targetWeight.multiply(CARB_POOL_FACTOR).multiply(n);
        BigDecimal fatPool = targetWeight.multiply(cfc).multiply(n);
        BigDecimal dailyProtein = currentWeight.multiply(PROTEIN_FACTOR).setScale(1, RoundingMode.HALF_UP);

        List<CycleDayTypeEnum> layout = layout(cycleDays, sportDays);
        BigDecimal divisorScale = n.divide(TEMPLATE_BASE_DAYS, 10, RoundingMode.HALF_UP);
        List<DayResult> days = new ArrayList<>(cycleDays);
        Set<Integer> sports = sportDays == null ? Set.of() : new TreeSet<>(sportDays);
        for (int i = 1; i <= cycleDays; i++) {
            CycleDayTypeEnum type = layout.get(i - 1);
            DayTypeParam p = paramOf(type);
            BigDecimal divisor = p.weight().multiply(divisorScale);
            BigDecimal carb = carbPool.multiply(p.carbPct())
                    .divide(divisor, 10, RoundingMode.HALF_UP).setScale(1, RoundingMode.HALF_UP);
            BigDecimal fat = fatPool.multiply(p.fatPct())
                    .divide(divisor, 10, RoundingMode.HALF_UP).setScale(1, RoundingMode.HALF_UP);
            int kcal = carb.multiply(CARB_PROTEIN_KCAL)
                    .add(dailyProtein.multiply(CARB_PROTEIN_KCAL))
                    .add(fat.multiply(FAT_KCAL))
                    .setScale(0, RoundingMode.HALF_UP).intValue();
            days.add(new DayResult(i, type, sports.contains(i), carb, dailyProtein, fat, kcal));
        }
        return new CycleResult(carbPool.setScale(1, RoundingMode.HALF_UP),
                fatPool.setScale(1, RoundingMode.HALF_UP), dailyProtein, days);
    }

    /**
     * 日型排布：模板循环 + 运动日抢占（高碳位用尽降级中碳位，非运动日保持模板相对顺序）
     *
     * @param cycleDays 周期天数 N
     * @param sportDays 运动日日序（可空）
     * @return 长度 N 的日型序列（索引 0 对应日序 1）
     */
    List<CycleDayTypeEnum> layout(int cycleDays, Set<Integer> sportDays) {
        List<CycleDayTypeEnum> base = new ArrayList<>(cycleDays);
        for (int i = 0; i < cycleDays; i++) {
            base.add(TEMPLATE[i % TEMPLATE.length]);
        }
        Set<Integer> sports = sportDays == null ? Set.of() : new TreeSet<>(sportDays);
        List<CycleDayTypeEnum> result = new ArrayList<>(base);
        // 运动日按日序升序抢占：先高后中（占的是模板中的高/中碳位额度）
        int highQuota = count(base, CycleDayTypeEnum.HIGH);
        int mediumQuota = count(base, CycleDayTypeEnum.MEDIUM);
        for (int dayIndex : sports) {
            if (highQuota > 0) {
                result.set(dayIndex - 1, CycleDayTypeEnum.HIGH);
                highQuota--;
            } else if (mediumQuota > 0) {
                result.set(dayIndex - 1, CycleDayTypeEnum.MEDIUM);
                mediumQuota--;
            }
            // 高+中碳位均用尽时运动日保持原日型（不再降级低碳）
        }
        // 非运动日按模板相对顺序填入剩余日型
        List<CycleDayTypeEnum> remaining = new ArrayList<>();
        for (CycleDayTypeEnum t : base) {
            if (t == CycleDayTypeEnum.HIGH && highQuota > 0) {
                highQuota--;
                remaining.add(t);
            } else if (t == CycleDayTypeEnum.MEDIUM && mediumQuota > 0) {
                mediumQuota--;
                remaining.add(t);
            } else if (t == CycleDayTypeEnum.LOW) {
                remaining.add(t);
            }
        }
        int cursor = 0;
        for (int i = 1; i <= cycleDays; i++) {
            if (!sports.contains(i)) {
                result.set(i - 1, remaining.get(cursor++));
            }
        }
        return result;
    }

    /** 统计序列中指定日型数量 */
    private int count(List<CycleDayTypeEnum> layout, CycleDayTypeEnum type) {
        int c = 0;
        for (CycleDayTypeEnum t : layout) {
            if (t == type) {
                c++;
            }
        }
        return c;
    }
}

package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.CycleDayTypeEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 碳循环计算引擎单元测试
 * （金标对拍 57/55/0.8/7 / 14 天与 cfc=1.0 变体 / 合计守恒 ±1g / 运动日抢占与降级 / N≠7 模板循环）
 * 作者: wanglx
 */
class CycleCalcServiceTest {

    private final CycleCalcService service = new CycleCalcService();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** 金标：57/55/0.8/7 无运动日 → 池 962.5/308/85.5；H 240.6/23.1、M 153.1/49.0、L 72.2/77.0 */
    @Test
    void compute_golden_57_55_08_7() {
        CycleCalcService.CycleResult r = service.compute(bd("57"), bd("55"), 7, bd("0.8"), Set.of());

        assertEquals(0, bd("962.5").compareTo(r.carbPool()));
        assertEquals(0, bd("308.0").compareTo(r.fatPool()));
        assertEquals(0, bd("85.5").compareTo(r.dailyProtein()));
        assertEquals(7, r.days().size());

        CycleCalcService.DayResult d1 = r.days().get(0);
        assertEquals(CycleDayTypeEnum.HIGH, d1.dayType());
        assertEquals(0, bd("240.6").compareTo(d1.carbG()));
        assertEquals(0, bd("23.1").compareTo(d1.fatG()));
        assertEquals(0, bd("85.5").compareTo(d1.proteinG()));

        CycleCalcService.DayResult d2 = r.days().get(1);
        assertEquals(CycleDayTypeEnum.MEDIUM, d2.dayType());
        assertEquals(0, bd("153.1").compareTo(d2.carbG()));
        assertEquals(0, bd("49.0").compareTo(d2.fatG()));

        CycleCalcService.DayResult d3 = r.days().get(2);
        assertEquals(CycleDayTypeEnum.LOW, d3.dayType());
        assertEquals(0, bd("72.2").compareTo(d3.carbG()));
        assertEquals(0, bd("77.0").compareTo(d3.fatG()));

        // 模板轮播：高·中·低·低·中·高·低
        List<CycleDayTypeEnum> expected = List.of(
                CycleDayTypeEnum.HIGH, CycleDayTypeEnum.MEDIUM, CycleDayTypeEnum.LOW, CycleDayTypeEnum.LOW,
                CycleDayTypeEnum.MEDIUM, CycleDayTypeEnum.HIGH, CycleDayTypeEnum.LOW);
        for (int i = 0; i < 7; i++) {
            assertEquals(expected.get(i), r.days().get(i).dayType(), "day " + (i + 1));
        }
    }

    /** 金标：7 日碳水合计 = 池 × K ≈ 1004.1g（±1g 守恒） */
    @Test
    void compute_golden_totalConservation_7days() {
        CycleCalcService.CycleResult r = service.compute(bd("57"), bd("55"), 7, bd("0.8"), Set.of());
        BigDecimal sum = r.days().stream().map(CycleCalcService.DayResult::carbG)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 2×240.6 + 2×153.1 + 3×72.2 = 1004.1（每日四舍五入累计漂移 ≤0.7g，规格容许 ±1g）
        assertTrue(sum.subtract(bd("1004.1")).abs().compareTo(BigDecimal.ONE) <= 0,
                "7 日合计应 ≈ 1004.1g，实际 " + sum);
    }

    /** 14 天：池翻倍、日数翻倍、同一日型单日目标与 7 天完全一致；合计 = 池×K（±1g） */
    @Test
    void compute_14days_sameDailyValues() {
        CycleCalcService.CycleResult r7 = service.compute(bd("57"), bd("55"), 7, bd("0.8"), Set.of());
        CycleCalcService.CycleResult r14 = service.compute(bd("57"), bd("55"), 14, bd("0.8"), Set.of());

        assertEquals(0, bd("1925.0").compareTo(r14.carbPool()));
        assertEquals(14, r14.days().size());
        // 同型单日一致
        for (CycleDayTypeEnum type : CycleDayTypeEnum.values()) {
            BigDecimal v7 = r7.days().stream().filter(d -> d.dayType() == type).findFirst().orElseThrow().carbG();
            BigDecimal v14 = r14.days().stream().filter(d -> d.dayType() == type).findFirst().orElseThrow().carbG();
            assertEquals(0, v7.compareTo(v14), type + " 单日碳水应不随 N 变化");
        }
        // 合计 = 2 × 7 日合计（±1g）
        BigDecimal sum14 = r14.days().stream().map(CycleCalcService.DayResult::carbG)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(sum14.subtract(bd("2008.2")).abs().compareTo(BigDecimal.ONE) <= 0,
                "14 日合计应 ≈ 2008.2，实际 " + sum14);
    }

    /** cfc=1.0 变体：脂肪池 = 55 × 1.0 × 7 = 385，H 28.9 / M 61.3 / L 96.3（±0.1） */
    @Test
    void compute_cfc10_fatPool() {
        CycleCalcService.CycleResult r = service.compute(bd("57"), bd("55"), 7, bd("1.0"), Set.of());
        assertEquals(0, bd("385.0").compareTo(r.fatPool()));
        assertEquals(0, bd("28.9").compareTo(r.days().get(0).fatG()));
        assertEquals(0, bd("61.3").compareTo(r.days().get(1).fatG()));
        assertEquals(0, bd("96.3").compareTo(r.days().get(2).fatG()));
    }

    /** 运动日抢占：第 3、5 天运动 → 均重排为高碳（占用模板两个高碳位），其余保持模板相对顺序 */
    @Test
    void layout_sportDays_takeHighSlots() {
        List<CycleDayTypeEnum> layout = service.layout(7, Set.of(3, 5));
        assertEquals(CycleDayTypeEnum.HIGH, layout.get(2));
        assertEquals(CycleDayTypeEnum.HIGH, layout.get(4));
        // 非运动日填剩余：模板去掉 2 高 → 中·低·低·中·低
        List<CycleDayTypeEnum> expected = List.of(
                CycleDayTypeEnum.MEDIUM, CycleDayTypeEnum.LOW, CycleDayTypeEnum.HIGH,
                CycleDayTypeEnum.LOW, CycleDayTypeEnum.HIGH, CycleDayTypeEnum.MEDIUM, CycleDayTypeEnum.LOW);
        for (int i = 0; i < 7; i++) {
            assertEquals(expected.get(i), layout.get(i), "day " + (i + 1));
        }
    }

    /** 运动日降级：3 个运动日 → 高碳位（2）用完后第 3 个占中碳位 */
    @Test
    void layout_sportDays_overflowToMedium() {
        List<CycleDayTypeEnum> layout = service.layout(7, Set.of(1, 2, 3));
        assertEquals(CycleDayTypeEnum.HIGH, layout.get(0));
        assertEquals(CycleDayTypeEnum.HIGH, layout.get(1));
        assertEquals(CycleDayTypeEnum.MEDIUM, layout.get(2));
    }

    /** N≠7 模板循环：8 天 → 第 8 天回到模板第 1 位（高碳） */
    @Test
    void layout_8days_templateCycles() {
        List<CycleDayTypeEnum> layout = service.layout(8, Set.of());
        assertEquals(8, layout.size());
        assertEquals(CycleDayTypeEnum.HIGH, layout.get(7));
        // 8 天各日型计数：高3 中2 低3
        assertEquals(3, layout.stream().filter(t -> t == CycleDayTypeEnum.HIGH).count());
        assertEquals(2, layout.stream().filter(t -> t == CycleDayTypeEnum.MEDIUM).count());
        assertEquals(3, layout.stream().filter(t -> t == CycleDayTypeEnum.LOW).count());
    }

    /** 热量按当日克数 4/4/9 取整：H 日 = 240.6×4 + 85.5×4 + 23.1×9 = 1512.3 → 1512 */
    @Test
    void compute_kcalFromRoundedGrams() {
        CycleCalcService.CycleResult r = service.compute(bd("57"), bd("55"), 7, bd("0.8"), Set.of());
        assertEquals(1512, r.days().get(0).kcal());
    }
}

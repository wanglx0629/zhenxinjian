package cn.zhenxinjian.common.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 宏量-能量守恒校验器单元测试（边界 ±10% 临界 / kcal=0 兜底 / 超差拒绝）
 * 作者: wanglx
 */
class MacroConsistencyValidatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void expectedKcalShouldUse449Factors() {
        // 10g 碳水 + 5g 蛋白 + 2g 脂肪 = 40+20+18 = 78
        assertEquals(0, bd("78").compareTo(
                MacroConsistencyValidator.expectedKcal(bd("10"), bd("5"), bd("2"))));
    }

    @Test
    void exactMatchShouldPass() {
        assertTrue(MacroConsistencyValidator.withinTolerance(bd("10"), bd("5"), bd("2"), 78));
    }

    @Test
    void boundaryExactly10PercentShouldPass() {
        // 理论 110（15×4 + 12.5×4），标注 100 → 偏差 10/100 = 10.0% 恰在容差边界，通过
        assertEquals(0, bd("110").compareTo(
                MacroConsistencyValidator.expectedKcal(bd("15"), bd("12.5"), bd("0"))));
        assertTrue(MacroConsistencyValidator.withinTolerance(bd("15"), bd("12.5"), bd("0"), 100));
    }

    @Test
    void justAbove10PercentShouldFail() {
        // 理论 110（15×4+12.5×4），标注 99 → 11/99 = 11.11% > 10% 拒绝
        assertFalse(MacroConsistencyValidator.withinTolerance(bd("15"), bd("12.5"), bd("0"), 99));
    }

    @Test
    void zeroKcalShouldUseBaseOne() {
        // kcal=0 时分母兜底为 1：全零宏量通过，任何宏量 >0.1 拒绝
        assertTrue(MacroConsistencyValidator.withinTolerance(bd("0"), bd("0"), bd("0"), 0));
        assertFalse(MacroConsistencyValidator.withinTolerance(bd("1"), bd("0"), bd("0"), 0));
    }

    @Test
    void decimalMacrosShouldWork() {
        // 小数宏量：12.3×4 + 5.6×4 + 7.8×9 = 49.2+22.4+70.2 = 141.8，标注 142 → 偏差 0.2/142 通过
        assertTrue(MacroConsistencyValidator.withinTolerance(bd("12.3"), bd("5.6"), bd("7.8"), 142));
    }
}

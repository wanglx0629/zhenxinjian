package cn.zhenxinjian.common.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Numbers 数值舍入工具单元测试
 * 覆盖：double/BigDecimal 两重载的 1 位小数 HALF_UP、负数、零、整数、进位边界
 * 作者: wanglx
 */
class NumbersTest {

    @Test
    void round1DoubleShouldRoundHalfUpToOneDecimal() {
        assertEquals(1.2d, Numbers.round1(1.15d), 0.0000001d);
    }

    @Test
    void round1DoubleShouldRoundDownWhenBelowHalf() {
        assertEquals(1.1d, Numbers.round1(1.14d), 0.0000001d);
    }

    @Test
    void round1DoubleShouldHandleNegativeValue() {
        assertEquals(-1.2d, Numbers.round1(-1.15d), 0.0000001d);
    }

    @Test
    void round1DoubleShouldKeepZeroAndInteger() {
        assertEquals(0.0d, Numbers.round1(0.0d), 0.0d);
        assertEquals(3.0d, Numbers.round1(3.0d), 0.0d);
    }

    @Test
    void round1BigDecimalShouldRoundHalfUpToOneDecimal() {
        assertEquals(1.2d, Numbers.round1(new BigDecimal("1.15")), 0.0d);
    }

    @Test
    void round1BigDecimalShouldRoundDownWhenBelowHalf() {
        assertEquals(1.1d, Numbers.round1(new BigDecimal("1.14")), 0.0d);
    }

    @Test
    void round1BigDecimalShouldHandleNegativeAndZero() {
        assertEquals(-1.2d, Numbers.round1(new BigDecimal("-1.15")), 0.0d);
        assertEquals(0.0d, Numbers.round1(BigDecimal.ZERO), 0.0d);
    }
}

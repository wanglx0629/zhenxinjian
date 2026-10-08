package cn.zhenxinjian.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LIMIT 片段安全构建单元测试
 * 作者: wanglx
 */
class SqlLimitTest {

    /** 固定条数：正常返回 LIMIT n */
    @Test
    void fixed_positive_returnsLimitClause() {
        assertEquals("LIMIT 100", SqlLimit.fixed(100));
    }

    /** 固定条数：非正返回空串（不追加 LIMIT） */
    @Test
    void fixed_nonPositive_returnsEmpty() {
        assertEquals("", SqlLimit.fixed(0));
        assertEquals("", SqlLimit.fixed(-1));
    }

    /** 有界：请求值超 cap 封顶（防 Integer.MAX_VALUE 拖垮全表） */
    @Test
    void bounded_aboveCap_clamps() {
        assertEquals("LIMIT 200", SqlLimit.bounded(Integer.MAX_VALUE, 200));
    }

    /** 有界：正常值原样返回 */
    @Test
    void bounded_withinCap_unchanged() {
        assertEquals("LIMIT 50", SqlLimit.bounded(50, 200));
    }

    /** 有界：空值或非正返回空串（不追加 LIMIT） */
    @Test
    void bounded_nullOrNonPositive_returnsEmpty() {
        assertEquals("", SqlLimit.bounded(null, 200));
        assertEquals("", SqlLimit.bounded(0, 200));
        assertEquals("", SqlLimit.bounded(-3, 200));
    }
}
package cn.zhenxinjian.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数值舍入公共工具（三宏/热量口径统一 1 位小数 HALF_UP）
 * 作者: wanglx
 */
public final class Numbers {

    private Numbers() {
    }

    /** 1 位小数四舍五入（HALF_UP） */
    public static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** 1 位小数四舍五入（HALF_UP） */
    public static double round1(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

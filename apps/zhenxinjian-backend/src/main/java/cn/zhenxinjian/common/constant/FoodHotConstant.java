package cn.zhenxinjian.common.constant;

import java.util.List;

/**
 * 热门食物常量（静态清单，覆盖高频减脂场景；饮食记录上线后可升级为按记录次数统计，接口不变）
 * 作者: wanglx
 */
public final class FoodHotConstant {

    private FoodHotConstant() {
    }

    /** 热门食物编号清单（≤12 条，均为内置库真实编号） */
    public static final List<String> HOT_FOOD_CODES = List.of(
            "F002", // 米饭（蒸，粳米）
            "F007", // 燕麦片
            "F016", // 馒头（蒸，标准粉）
            "F022", // 红薯（甘薯）
            "F056", // 鸡蛋（均值）
            "F063", // 牛奶（均值）
            "F046", // 鸡胸脯肉
            "F039", // 牛肉（瘦）
            "F075", // 三文鱼
            "F082", // 虾仁
            "F092", // 豆腐（北）
            "F113"  // 西兰花
    );
}

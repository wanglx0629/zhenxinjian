package cn.zhenxinjian.common.enums;

/**
 * 经期四阶段字典枚举（判定结果，不落库；上浮值跨模式通用）
 * 作者: wanglx
 */
public enum MenstrualPhaseEnum {

    /** 经期：碳水 +15g / 热量 +60kcal */
    MENSTRUAL("menstrual", "经期", 15, 60),

    /** 卵泡期：基线无上浮 */
    FOLLICULAR("follicular", "卵泡期", 0, 0),

    /** 排卵期：碳水 +5g / 热量 +20kcal */
    OVULATION("ovulation", "排卵期", 5, 20),

    /** 黄体期：碳水 +10g / 热量 +120kcal */
    LUTEAL("luteal", "黄体期", 10, 120);

    /** 阶段键（经期 VO 下发键，端上按键匹配展示） */
    private final String key;

    /** 阶段名称 */
    private final String desc;

    /** 碳水上浮 g */
    private final int carbUplift;

    /** 热量上浮 kcal */
    private final int kcalUplift;

    MenstrualPhaseEnum(String key, String desc, int carbUplift, int kcalUplift) {
        this.key = key;
        this.desc = desc;
        this.carbUplift = carbUplift;
        this.kcalUplift = kcalUplift;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }

    public int getCarbUplift() {
        return carbUplift;
    }

    public int getKcalUplift() {
        return kcalUplift;
    }
}
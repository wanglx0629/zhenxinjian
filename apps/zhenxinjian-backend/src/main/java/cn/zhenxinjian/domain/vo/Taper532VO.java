package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 532 四阶段计划卡 + 今日目标视图
 * 作者: wanglx
 */
@Data
@Schema(description = "532 四阶段计划卡 + 今日目标")
public class Taper532VO implements Serializable {

    @Schema(description = "四阶段计划卡（阶段 4 平台触发式 −20/−80）")
    private List<StageItem> stages;

    @Schema(description = "今日 532 目标（基线 + 平台下调 + 经期上浮）")
    private TodayTarget today;

    @Data
    @Schema(description = "532 阶段计划项")
    public static class StageItem implements Serializable {

        @Schema(description = "阶段序号 1-4")
        private Integer n;

        @Schema(description = "阶段名称")
        private String name;

        @Schema(description = "阶段说明")
        private String desc;

        @Schema(description = "碳水调整 g（阶段 4 为 -20）")
        private Integer carbDelta;

        @Schema(description = "热量调整 kcal（阶段 4 为 -80）")
        private Integer kcalDelta;

        @Schema(description = "是否平台触发式（仅阶段 4 为 true）")
        private Boolean plateauTriggered;
    }

    @Data
    @Schema(description = "今日 532 目标")
    public static class TodayTarget implements Serializable {

        @Schema(description = "目标碳水 g（1 位小数）")
        private Double carb;

        @Schema(description = "目标蛋白 g（1 位小数，保持不变）")
        private Double protein;

        @Schema(description = "目标脂肪 g（1 位小数，保持不变）")
        private Double fat;

        @Schema(description = "目标热量 kcal（取整）")
        private Integer kcal;

        @Schema(description = "是否平台下调态")
        private Boolean adjusted;

        @Schema(description = "当前经期阶段键（未叠加为空）")
        private String phaseKey;

        @Schema(description = "当前经期阶段名称（未叠加为空）")
        private String phaseName;
    }
}
package cn.zhenxinjian.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 按日饮食记录视图（餐别四组 + 各组小计 + 当日合计）
 * 作者: wanglx
 */
@Data
@Schema(description = "按日饮食记录")
public class DietDayVO implements Serializable {

    @Schema(description = "查询日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "餐别分组（恒4组，无记录为空列表）")
    private List<MealGroup> meals;

    @Schema(description = "当日合计碳水 g")
    private Double totalCarb;

    @Schema(description = "当日合计蛋白 g")
    private Double totalProtein;

    @Schema(description = "当日合计脂肪 g")
    private Double totalFat;

    @Schema(description = "当日合计能量 kcal")
    private Integer totalKcal;

    /**
     * 餐别分组
     * 作者: wanglx
     */
    @Data
    @Schema(description = "餐别分组")
    public static class MealGroup implements Serializable {

        @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐")
        private Integer mealType;

        @Schema(description = "餐别名称")
        private String mealName;

        @Schema(description = "该餐别记录列表")
        private List<DietRecordVO> records;

        @Schema(description = "小计碳水 g")
        private Double carb;

        @Schema(description = "小计蛋白 g")
        private Double protein;

        @Schema(description = "小计脂肪 g")
        private Double fat;

        @Schema(description = "小计能量 kcal")
        private Integer kcal;
    }
}

package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 份量试算响应 VO（按「每 100g 值 × 克数 ÷ 100」换算）
 * 作者: wanglx
 */
@Data
@Schema(description = "份量试算响应")
public class FoodCalcVO {

    @Schema(description = "食物ID")
    private Long foodId;

    @Schema(description = "试算克数")
    private BigDecimal grams;

    @Schema(description = "碳水g（克数对应，2位小数）")
    private BigDecimal carb;

    @Schema(description = "蛋白g（克数对应，2位小数）")
    private BigDecimal protein;

    @Schema(description = "脂肪g（克数对应，2位小数）")
    private BigDecimal fat;

    @Schema(description = "能量kcal（克数对应，取整）")
    private Integer kcal;
}

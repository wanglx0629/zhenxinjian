package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 食物响应 VO（搜索列表/详情/热门/自定义列表共用；营养值为每 100g 口径）
 * 作者: wanglx
 */
@Data
@Schema(description = "食物响应")
public class FoodVO {

    @Schema(description = "食物ID")
    private Long id;

    @Schema(description = "食物编号（内置 F001–F200；自定义为空）")
    private String code;

    @Schema(description = "分类编号：01-10")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "食物名称")
    private String name;

    @Schema(description = "别名/俗称")
    private String alias;

    @Schema(description = "碳水化合物 g/100g")
    private BigDecimal carb;

    @Schema(description = "蛋白质 g/100g")
    private BigDecimal protein;

    @Schema(description = "脂肪 g/100g")
    private BigDecimal fat;

    @Schema(description = "能量 kcal/100g")
    private Integer kcal;

    @Schema(description = "常用单份克数")
    private BigDecimal serving;

    @Schema(description = "来源：1内置 2自定义")
    private Integer source;

    @Schema(description = "状态:0停用 1有效")
    private Integer status;
}

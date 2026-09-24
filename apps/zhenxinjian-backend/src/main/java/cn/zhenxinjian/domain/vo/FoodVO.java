package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Schema(description = "能量千焦 kJ/100g（可空）")
    private Integer kj;

    @Schema(description = "常用单份克数")
    private BigDecimal serving;

    @Schema(description = "填报单位（份/个/杯/包…，可空）")
    private String unit;

    @Schema(description = "来源：1基础 2用户共建")
    private Integer source;

    @Schema(description = "审核状态：0待审核 1已通过 2已驳回 3无需审核")
    private Integer auditStatus;

    @Schema(description = "最近一次驳回原因（已驳回时展示）")
    private String auditRemark;

    @Schema(description = "AI校验结论：pass/suspect/reject/none")
    private String aiVerdict;

    @Schema(description = "AI校验建议")
    private String aiSuggestion;

    @Schema(description = "最近一次提交/重提时间")
    private LocalDateTime submitTime;

    @Schema(description = "状态:0停用 1有效")
    private Integer status;

    @Schema(description = "图片 URL（foods.image 优先；内置食物为 food_images 预热图；无图为空走分类占位）")
    private String image;

    @Schema(description = "提交人昵称（仅管理端分页填充；C 端为空）")
    private String submitterName;
}

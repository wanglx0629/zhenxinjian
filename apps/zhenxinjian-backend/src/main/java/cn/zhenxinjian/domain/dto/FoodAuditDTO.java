package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端共建食物审核入参（APPROVE/REJECT/ADMIN_FIX；仅 source=2 共建食物可审）
 * 作者: wanglx
 */
@Data
@Schema(description = "管理端共建食物审核入参")
public class FoodAuditDTO {

    @NotBlank(message = "必填参数缺失")
    @Pattern(regexp = "APPROVE|REJECT|ADMIN_FIX", message = "审核动作非法")
    @Schema(description = "审核动作：APPROVE 通过 / REJECT 驳回 / ADMIN_FIX 修正后通过", requiredMode = Schema.RequiredMode.REQUIRED)
    private String action;

    @Size(max = 255, message = "驳回原因最长255字符")
    @Schema(description = "驳回原因（REJECT 必填；APPROVE/ADMIN_FIX 可作为备注）")
    private String remark;

    @Schema(description = "修正值（仅 ADMIN_FIX 必填；字段可空，空字段沿用原值，落库前整体校验）")
    private Fix fix;

    /**
     * ADMIN_FIX 修正值（可空字段沿用原值）
     */
    @Data
    @Schema(description = "管理员修正值")
    public static class Fix {

        @Size(max = 100, message = "食物名称过长")
        @Schema(description = "食物名称")
        private String name;

        @Size(max = 100, message = "别名过长")
        @Schema(description = "别名/俗称")
        private String alias;

        @Size(max = 2, message = "分类编号非法")
        @Schema(description = "分类编号：01-10")
        private String categoryCode;

        @DecimalMin(value = "0", message = "碳水不可为负")
        @DecimalMax(value = "100", message = "碳水不可超100")
        @Schema(description = "碳水化合物 g/100g")
        private BigDecimal carb;

        @DecimalMin(value = "0", message = "蛋白质不可为负")
        @DecimalMax(value = "100", message = "蛋白质不可超100")
        @Schema(description = "蛋白质 g/100g")
        private BigDecimal protein;

        @DecimalMin(value = "0", message = "脂肪不可为负")
        @DecimalMax(value = "100", message = "脂肪不可超100")
        @Schema(description = "脂肪 g/100g")
        private BigDecimal fat;

        @Min(value = 0, message = "能量不可为负")
        @Max(value = 900, message = "能量不可超900")
        @Schema(description = "能量 kcal/100g")
        private Integer kcal;

        @Min(value = 0, message = "千焦不可为负")
        @Max(value = 3800, message = "千焦不可超3800")
        @Schema(description = "能量千焦 kJ/100g")
        private Integer kj;

        @Size(max = 16, message = "单位过长")
        @Schema(description = "填报单位")
        private String unit;

        @DecimalMin(value = "5", message = "单份克数不可小于5")
        @DecimalMax(value = "1000", message = "单份克数不可超1000")
        @Schema(description = "常用单份克数")
        private BigDecimal serving;
    }
}

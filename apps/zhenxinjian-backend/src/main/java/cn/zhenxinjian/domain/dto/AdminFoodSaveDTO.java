package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端食物保存入参（仅内置食物；自定义食物只读）
 * 作者: wanglx
 */
@Data
@Schema(description = "管理端食物保存入参")
public class AdminFoodSaveDTO {

    @NotBlank(message = "必填参数缺失")
    @Size(max = 64, message = "食物名称最长64字符")
    @Schema(description = "食物名称")
    private String name;

    @NotBlank(message = "必填参数缺失")
    @Schema(description = "分类编号：01-10")
    private String categoryCode;

    @NotBlank(message = "必填参数缺失")
    @Schema(description = "分类名称")
    private String categoryName;

    @Size(max = 128, message = "别名最长128字符")
    @Schema(description = "别名/俗称（可空）")
    private String alias;

    @NotNull(message = "必填参数缺失")
    @DecimalMin(value = "0", message = "碳水不可为负")
    @DecimalMax(value = "100", message = "碳水不可超100")
    @Schema(description = "碳水化合物 g/100g")
    private BigDecimal carb;

    @NotNull(message = "必填参数缺失")
    @DecimalMin(value = "0", message = "蛋白质不可为负")
    @DecimalMax(value = "100", message = "蛋白质不可超100")
    @Schema(description = "蛋白质 g/100g")
    private BigDecimal protein;

    @NotNull(message = "必填参数缺失")
    @DecimalMin(value = "0", message = "脂肪不可为负")
    @DecimalMax(value = "100", message = "脂肪不可超100")
    @Schema(description = "脂肪 g/100g")
    private BigDecimal fat;

    @NotNull(message = "必填参数缺失")
    @Min(value = 0, message = "能量不可为负")
    @Max(value = 5000, message = "能量不可超5000")
    @Schema(description = "能量 kcal/100g")
    private Integer kcal;

    @DecimalMin(value = "1", message = "单份克数不可小于1")
    @Schema(description = "常用单份克数（默认100）")
    private BigDecimal serving;
}

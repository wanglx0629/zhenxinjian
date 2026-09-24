package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 自定义食物新增/编辑请求（新增/编辑一体：id 为空新增，非空编辑）
 * 数值区间与能量守恒由 CustomFoodService 业务校验（40402/40403），此处仅做结构校验
 * 作者: wanglx
 */
@Data
@Schema(description = "自定义食物新增/编辑请求")
public class CustomFoodSaveDTO {

    @Schema(description = "食物ID（编辑时必填，新增为空）")
    private Long id;

    @Schema(description = "食物名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "食物名称不能为空")
    @Size(max = 100, message = "食物名称过长")
    private String name;

    @Schema(description = "别名/俗称（可空）")
    @Size(max = 100, message = "别名过长")
    private String alias;

    @Schema(description = "分类编号：01-18，缺省默认18（加工调理食品及其他类）")
    @Size(max = 2, message = "分类编号非法")
    private String categoryCode;

    @Schema(description = "碳水化合物 g/100g（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "碳水化合物不能为空")
    private BigDecimal carb;

    @Schema(description = "蛋白质 g/100g（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "蛋白质不能为空")
    private BigDecimal protein;

    @Schema(description = "脂肪 g/100g（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "脂肪不能为空")
    private BigDecimal fat;

    @Schema(description = "能量 kcal/100g（0-900；与 kj 至少一项非空，缺省按 kj÷4.184 换算，kcal 为入库基准）")
    private Integer kcal;

    @Schema(description = "能量千焦 kJ/100g（0-3800；与 kcal 至少一项非空，后端以 kcal 为入库基准）")
    private Integer kj;

    @Schema(description = "能量录入单位：KJ 或 KCAL（缺省 KCAL；KJ 时前端按 4.184 联动换算出 kcal 后提交）")
    private String energyUnit;

    @Schema(description = "填报单位（份/个/杯/包…，可空）")
    @Size(max = 16, message = "单位过长")
    private String unit;

    @Schema(description = "食物图片相对路径（可选，经统一文件上传返回的 key）")
    @Size(max = 512, message = "图片路径过长")
    private String image;

    @Schema(description = "常用单份克数（5-1000，缺省100）")
    private BigDecimal serving;
}

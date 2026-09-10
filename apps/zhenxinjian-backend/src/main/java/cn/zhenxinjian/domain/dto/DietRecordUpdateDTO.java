package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 饮食记录编辑请求（id 走路径参数）
 * 食物来源仅可改餐别/份量/备注（按快照重算摄入，不回查食物表）；手动输入可改名称与三宏热量
 * 作者: wanglx
 */
@Data
@Schema(description = "饮食记录编辑请求")
public class DietRecordUpdateDTO {

    @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "餐别不能为空")
    private Integer mealType;

    @Schema(description = "份量克数（食物来源可改，>0 且 ≤5000）")
    private BigDecimal amountG;

    @Schema(description = "食物名称（手动输入可改，≤50字）")
    @Size(max = 50, message = "食物名称过长")
    private String name;

    @Schema(description = "碳水 g（手动输入可改）")
    private BigDecimal carb;

    @Schema(description = "蛋白 g（手动输入可改）")
    private BigDecimal protein;

    @Schema(description = "脂肪 g（手动输入可改）")
    private BigDecimal fat;

    @Schema(description = "能量 kcal（手动输入可改）")
    private Integer kcal;

    @Schema(description = "备注（可空，≤100字）")
    @Size(max = 100, message = "备注过长")
    private String remark;
}

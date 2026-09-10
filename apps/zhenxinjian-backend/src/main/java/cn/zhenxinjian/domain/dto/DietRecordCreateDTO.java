package cn.zhenxinjian.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 饮食记录新增请求
 * 结构校验仅做非空/长度约束；份量区间、宏量区间与能量守恒由 DietRecordService 业务校验（405xx）
 * 作者: wanglx
 */
@Data
@Schema(description = "饮食记录新增请求")
public class DietRecordCreateDTO {

    @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "餐别不能为空")
    private Integer mealType;

    @Schema(description = "来源：1内置食物 2自定义食物 3手动输入", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来源不能为空")
    private Integer source;

    @Schema(description = "食物ID（source=1/2 必填）")
    private Long foodId;

    @Schema(description = "份量克数（source=1/2 必填，>0 且 ≤5000）")
    private BigDecimal amountG;

    @Schema(description = "食物名称（source=3 必填，≤50字）")
    @Size(max = 50, message = "食物名称过长")
    private String name;

    @Schema(description = "碳水 g（source=3 必填，实际吃下总量）")
    private BigDecimal carb;

    @Schema(description = "蛋白 g（source=3 必填）")
    private BigDecimal protein;

    @Schema(description = "脂肪 g（source=3 必填）")
    private BigDecimal fat;

    @Schema(description = "能量 kcal（source=3 必填）")
    private Integer kcal;

    @Schema(description = "记录日期（可空默认当日；不允许未来日期）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @Schema(description = "备注（可空，≤100字）")
    @Size(max = 100, message = "备注过长")
    private String remark;
}

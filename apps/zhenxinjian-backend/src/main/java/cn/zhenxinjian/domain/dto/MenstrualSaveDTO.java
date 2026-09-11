package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 经期设置保存入参（L/D 区间与开启起始日约束由服务层返回 40801）
 * 作者: wanglx
 */
@Data
@Schema(description = "经期设置保存入参")
public class MenstrualSaveDTO {

    @Schema(description = "是否开启经期管理：0关 1开", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开启状态不能为空")
    @Min(value = 0, message = "开启状态取值仅支持0/1")
    @Max(value = 1, message = "开启状态取值仅支持0/1")
    private Integer enabled;

    @Schema(description = "末次月经起始日（开启时必填，不得为未来日期）")
    private LocalDate periodStartDate;

    @Schema(description = "周期长度 L（21-35，默认28）", example = "28")
    private Integer cycleLen;

    @Schema(description = "经期天数 D（3-10，默认5）", example = "5")
    private Integer periodDays;
}
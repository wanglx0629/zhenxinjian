package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提醒设置保存入参（整体保存：总开关 + 三餐开关与时间；时间非法由服务层返回 40701）
 * 作者: wanglx
 */
@Data
@Schema(description = "提醒设置保存入参")
public class ReminderSaveDTO {

    @NotNull(message = "总开关不能为空")
    @Min(value = 0, message = "开关取值仅支持0/1")
    @Max(value = 1, message = "开关取值仅支持0/1")
    @Schema(description = "总开关：0关 1开", example = "1")
    private Integer masterSwitch;

    @NotNull(message = "早餐开关不能为空")
    @Min(value = 0, message = "开关取值仅支持0/1")
    @Max(value = 1, message = "开关取值仅支持0/1")
    @Schema(description = "早餐提醒开关：0关 1开", example = "1")
    private Integer breakfastSwitch;

    @NotNull(message = "早餐提醒时间不能为空")
    @Schema(description = "早餐提醒时间 HH:mm", example = "08:30")
    private String breakfastTime;

    @NotNull(message = "午餐开关不能为空")
    @Min(value = 0, message = "开关取值仅支持0/1")
    @Max(value = 1, message = "开关取值仅支持0/1")
    @Schema(description = "午餐提醒开关：0关 1开", example = "1")
    private Integer lunchSwitch;

    @NotNull(message = "午餐提醒时间不能为空")
    @Schema(description = "午餐提醒时间 HH:mm", example = "12:00")
    private String lunchTime;

    @NotNull(message = "晚餐开关不能为空")
    @Min(value = 0, message = "开关取值仅支持0/1")
    @Max(value = 1, message = "开关取值仅支持0/1")
    @Schema(description = "晚餐提醒开关：0关 1开", example = "1")
    private Integer dinnerSwitch;

    @NotNull(message = "晚餐提醒时间不能为空")
    @Schema(description = "晚餐提醒时间 HH:mm", example = "18:30")
    private String dinnerTime;
}

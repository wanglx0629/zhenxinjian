package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 身体档案保存请求（录入/修改一体，幂等覆盖）
 * 作者: wanglx
 */
@Data
@Schema(description = "身体档案保存请求")
public class BodyProfileSaveDTO {

    @Schema(description = "性别:1男 2女", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别取值非法")
    @Max(value = 2, message = "性别取值非法")
    private Integer gender;

    @Schema(description = "年龄（12-80）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "年龄不能为空")
    @Min(value = 12, message = "年龄须在12-80岁之间")
    @Max(value = 80, message = "年龄须在12-80岁之间")
    private Integer age;

    @Schema(description = "身高cm（100-250）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "身高不能为空")
    @DecimalMin(value = "100", message = "身高须在100-250cm之间")
    @DecimalMax(value = "250", message = "身高须在100-250cm之间")
    private Double height;

    @Schema(description = "当前体重kg（25-200）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "25", message = "体重须在25-200kg之间")
    @DecimalMax(value = "200", message = "体重须在25-200kg之间")
    private Double weight;

    @Schema(description = "目标体重kg（25-200，须不大于当前体重）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标体重不能为空")
    @DecimalMin(value = "25", message = "目标体重须在25-200kg之间")
    @DecimalMax(value = "200", message = "目标体重须在25-200kg之间")
    private Double targetWeight;

    @Schema(description = "活动系数档位:1久坐 2轻度 3中度 4高度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动系数不能为空")
    @Min(value = 1, message = "活动系数档位非法")
    @Max(value = 4, message = "活动系数档位非法")
    private Integer activityLevel;

    @Schema(description = "减脂缺口kcal:200/300/400/500，缺省默认200")
    private Integer deficit;

    @Schema(description = "脂肪系数（碳循环预留）:0.8/1.0，缺省默认0.8")
    private Double cfc;
}

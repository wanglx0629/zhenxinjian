package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 体重记录保存入参（25-200kg 区间与未来日期由服务层返回 40802）
 * 作者: wanglx
 */
@Data
@Schema(description = "体重记录保存入参")
public class WeightSaveDTO {

    @Schema(description = "记录日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @Schema(description = "体重 kg（25-200）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "体重不能为空")
    private Double weight;
}
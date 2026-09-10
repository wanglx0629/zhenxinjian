package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 减脂模式切换请求
 * 作者: wanglx
 */
@Data
@Schema(description = "减脂模式切换请求")
public class ModeSwitchDTO {

    @Schema(description = "目标模式:1=532 2=碳循环", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模式不能为空")
    @Min(value = 1, message = "模式取值非法")
    @Max(value = 2, message = "模式取值非法")
    private Integer mode;
}

package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 碳循环周期创建请求
 * 作者: wanglx
 */
@Data
@Schema(description = "碳循环周期创建请求")
public class CyclePlanCreateDTO {

    @Schema(description = "周期天数（7-14，默认7）")
    @Min(value = 7, message = "周期天数须在7-14之间")
    @Max(value = 14, message = "周期天数须在7-14之间")
    private Integer cycleDays;

    @Schema(description = "脂肪系数:0.8/1.0（默认0.8）")
    private Double cfc;

    @Schema(description = "运动日日序列表（1..cycleDays，可空）")
    @Size(max = 14, message = "运动日数量不能超过周期天数")
    private List<@NotNull(message = "运动日日序不能为空") @Min(value = 1, message = "运动日日序非法") Integer> sportDays;
}

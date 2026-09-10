package cn.zhenxinjian.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 碳循环周期计划视图（周期 + 逐日计划 + 今日定位；无周期时 plan 为空）
 * 作者: wanglx
 */
@Data
@Schema(description = "碳循环周期计划")
public class CyclePlanVO implements Serializable {

    @Schema(description = "周期ID")
    private Long id;

    @Schema(description = "周期天数（7-14）")
    private Integer cycleDays;

    @Schema(description = "脂肪系数：0.8/1.0")
    private Double cfc;

    @Schema(description = "起始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "碳水池 g")
    private Double carbPool;

    @Schema(description = "脂肪池 g")
    private Double fatPool;

    @Schema(description = "每日蛋白 g（周期内固定）")
    private Double dailyProtein;

    @Schema(description = "状态：1进行中 2已完成 3已终止")
    private Integer status;

    @Schema(description = "逐日计划（按日序升序）")
    private List<CycleDayVO> days;

    @Schema(description = "今日日序（今日在周期内时返回 1..N；否则为 null）")
    private Integer todayIndex;
}

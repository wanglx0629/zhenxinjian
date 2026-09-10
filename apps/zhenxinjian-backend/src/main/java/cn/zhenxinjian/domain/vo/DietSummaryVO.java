package cn.zhenxinjian.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 当日累计与目标进度视图（未建档空态：recorded=false，目标与达成率为空）
 * 作者: wanglx
 */
@Data
@Schema(description = "当日累计与目标进度")
public class DietSummaryVO implements Serializable {

    @Schema(description = "查询日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "是否已录入身体档案（false 时空态，目标/达成率为空）")
    private Boolean recorded;

    @Schema(description = "减脂模式（当前恒 532；碳循环落地后按日型扩展）")
    private String mode;

    @Schema(description = "已摄入碳水 g")
    private Double carbActual;

    @Schema(description = "已摄入蛋白 g")
    private Double proteinActual;

    @Schema(description = "已摄入脂肪 g")
    private Double fatActual;

    @Schema(description = "已摄入能量 kcal")
    private Integer kcalActual;

    @Schema(description = "目标碳水 g（未建档为空）")
    private Double carbTarget;

    @Schema(description = "目标蛋白 g（未建档为空）")
    private Double proteinTarget;

    @Schema(description = "目标脂肪 g（未建档为空）")
    private Double fatTarget;

    @Schema(description = "目标能量 kcal（未建档为空）")
    private Integer kcalTarget;

    @Schema(description = "碳水达成率%（1位小数；未建档为空）")
    private Double carbRate;

    @Schema(description = "蛋白达成率%（1位小数；未建档为空）")
    private Double proteinRate;

    @Schema(description = "脂肪达成率%（1位小数；未建档为空）")
    private Double fatRate;

    @Schema(description = "能量达成率%（1位小数；未建档为空）")
    private Double kcalRate;
}

package cn.zhenxinjian.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 碳循环每日日型计划视图
 * 作者: wanglx
 */
@Data
@Schema(description = "碳循环每日日型计划")
public class CycleDayVO implements Serializable {

    @Schema(description = "日序 1..N")
    private Integer dayIndex;

    @Schema(description = "日历日")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dayDate;

    @Schema(description = "日型：1高碳 2中碳 3低碳")
    private Integer dayType;

    @Schema(description = "日型名称：高碳日/中碳日/低碳日")
    private String dayTypeName;

    @Schema(description = "运动日：0否 1是")
    private Integer isSport;

    @Schema(description = "目标碳水 g")
    private Double carbG;

    @Schema(description = "目标蛋白 g")
    private Double proteinG;

    @Schema(description = "目标脂肪 g")
    private Double fatG;

    @Schema(description = "目标能量 kcal")
    private Integer kcal;
}

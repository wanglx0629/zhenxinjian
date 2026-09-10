package cn.zhenxinjian.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录项视图
 * 作者: wanglx
 */
@Data
@Schema(description = "饮食记录项")
public class DietRecordVO implements Serializable {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "记录日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐")
    private Integer mealType;

    @Schema(description = "餐别名称")
    private String mealName;

    @Schema(description = "来源：1内置食物 2自定义食物 3手动输入")
    private Integer source;

    @Schema(description = "食物ID（手动输入为空）")
    private Long foodId;

    @Schema(description = "食物名称快照")
    private String foodName;

    @Schema(description = "份量克数")
    private Double amountG;

    @Schema(description = "实际摄入碳水 g")
    private Double carbG;

    @Schema(description = "实际摄入蛋白 g")
    private Double proteinG;

    @Schema(description = "实际摄入脂肪 g")
    private Double fatG;

    @Schema(description = "实际摄入能量 kcal")
    private Integer kcal;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

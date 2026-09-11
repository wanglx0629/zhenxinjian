package cn.zhenxinjian.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端饮食记录视图（只读，食物快照冗余列直接透出）
 * 作者: wanglx
 */
@Data
@Schema(description = "管理端饮食记录视图")
public class AdminDietRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "归属用户ID")
    private Long userId;

    @Schema(description = "用户昵称（用户已删显示「用户#id」）")
    private String userNickname;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐")
    private Integer mealType;

    @Schema(description = "食物名称快照")
    private String foodName;

    @Schema(description = "份量克数")
    private Double amount;

    @Schema(description = "实际摄入碳水 g")
    private Double carb;

    @Schema(description = "实际摄入蛋白 g")
    private Double protein;

    @Schema(description = "实际摄入脂肪 g")
    private Double fat;

    @Schema(description = "实际摄入能量 kcal")
    private Integer kcal;

    @Schema(description = "来源：1内置食物 2自定义食物 3手动输入")
    private Integer source;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

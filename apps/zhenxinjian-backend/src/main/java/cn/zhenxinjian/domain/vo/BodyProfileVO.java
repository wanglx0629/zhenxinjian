package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 身体档案响应 VO（档案 + 计算结果快照 + 风险标记 + 免责声明）
 * 作者: wanglx
 */
@Data
@Schema(description = "身体档案响应")
public class BodyProfileVO {

    @Schema(description = "是否已录入档案（false 为空态，其余字段为空）")
    private Boolean recorded;

    @Schema(description = "性别:1男 2女")
    private Integer gender;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "身高cm")
    private Double height;

    @Schema(description = "当前体重kg")
    private Double weight;

    @Schema(description = "目标体重kg")
    private Double targetWeight;

    @Schema(description = "活动系数档位:1久坐 2轻度 3中度 4高度")
    private Integer activityLevel;

    @Schema(description = "活动系数快照")
    private Double activityFactor;

    @Schema(description = "减脂缺口kcal")
    private Integer deficit;

    @Schema(description = "脂肪系数:0.8/1.0")
    private Double cfc;

    @Schema(description = "BMR kcal")
    private Integer bmr;

    @Schema(description = "TDEE kcal")
    private Integer tdee;

    @Schema(description = "基准热量kcal（TDEE-缺口）")
    private Integer targetKcal;

    @Schema(description = "目标碳水g")
    private Double targetCarb;

    @Schema(description = "目标蛋白g")
    private Double targetProtein;

    @Schema(description = "目标脂肪g")
    private Double targetFat;

    @Schema(description = "低热量风险标记（true=基准热量低于安全下限，仅提示不阻断）")
    private Boolean lowKcalRisk;

    @Schema(description = "免责声明（不可移除）")
    private String disclaimer;

    @Schema(description = "档案最近更新时间")
    private LocalDateTime updateTime;
}

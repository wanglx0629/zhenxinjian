package cn.zhenxinjian.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 身体档案历史实体（修改前档案整体留痕，追加写，不参与计算）
 * 作者: wanglx
 */
@Data
@TableName("user_body_history")
@Schema(description = "身体档案历史实体")
public class UserBodyHistory implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "来源档案ID（user_body.id）")
    private Long userBodyId;

    @Schema(description = "用户ID（归档时归属）")
    private Long userId;

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

    @Schema(description = "活动系数档位")
    private Integer activityLevel;

    @Schema(description = "活动系数快照")
    private Double activityFactor;

    @Schema(description = "减脂缺口kcal")
    private Integer deficit;

    @Schema(description = "脂肪系数")
    private Double cfc;

    @Schema(description = "BMR快照kcal")
    private Integer bmr;

    @Schema(description = "TDEE快照kcal")
    private Integer tdee;

    @Schema(description = "基准热量快照kcal")
    private Integer targetKcal;

    @Schema(description = "目标碳水快照g")
    private Double targetCarb;

    @Schema(description = "目标蛋白快照g")
    private Double targetProtein;

    @Schema(description = "目标脂肪快照g")
    private Double targetFat;

    @Schema(description = "归档时刻（被新版本覆盖的时间）")
    private LocalDateTime archivedAt;

    @Schema(description = "状态:0停用 1有效")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "创建时间（归档行写入时间）")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "软删除:0未删 1已删")
    private Integer deleteFlag;
}

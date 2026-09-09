package cn.zhenxinjian.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 身体档案实体（当前档案，每用户活跃唯一；生成列 user_id_active 不映射）
 * 作者: wanglx
 */
@Data
@TableName("user_body")
@Schema(description = "身体档案实体")
public class UserBody implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID（关联 users.id，含游客）")
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

    @Schema(description = "活动系数档位:1久坐 2轻度 3中度 4高度")
    private Integer activityLevel;

    @Schema(description = "活动系数快照")
    private Double activityFactor;

    @Schema(description = "减脂缺口kcal:200/300/400/500")
    private Integer deficit;

    @Schema(description = "脂肪系数（碳循环预留）:0.8/1.0")
    private Double cfc;

    @Schema(description = "BMR快照kcal")
    private Integer bmr;

    @Schema(description = "TDEE快照kcal")
    private Integer tdee;

    @Schema(description = "基准热量快照kcal（TDEE-缺口）")
    private Integer targetKcal;

    @Schema(description = "目标碳水g（1位小数）")
    private Double targetCarb;

    @Schema(description = "目标蛋白g（1位小数）")
    private Double targetProtein;

    @Schema(description = "目标脂肪g（1位小数）")
    private Double targetFat;

    @Schema(description = "状态:0停用 1有效")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "软删除:0未删 1已删")
    private Integer deleteFlag;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}

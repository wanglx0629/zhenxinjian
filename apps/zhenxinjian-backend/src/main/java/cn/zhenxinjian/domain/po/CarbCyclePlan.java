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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 碳循环周期计划实体（图片公式三大池快照，进行中唯一由服务层保证）
 * 作者: wanglx
 */
@Data
@TableName("carb_cycle_plan")
@Schema(description = "碳循环周期计划实体")
public class CarbCyclePlan implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属用户ID（含游客）")
    private Long userId;

    @Schema(description = "周期天数（7-14）")
    private Integer cycleDays;

    @Schema(description = "脂肪系数：0.8/1.0")
    private BigDecimal cfc;

    @Schema(description = "起始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @TableField("weight_snapshot")
    @Schema(description = "创建时当前体重快照 kg")
    private BigDecimal weightSnapshot;

    @TableField("target_weight_snapshot")
    @Schema(description = "创建时目标体重快照 kg")
    private BigDecimal targetWeightSnapshot;

    @TableField("carb_pool")
    @Schema(description = "碳水池 g")
    private BigDecimal carbPool;

    @TableField("fat_pool")
    @Schema(description = "脂肪池 g")
    private BigDecimal fatPool;

    @TableField("daily_protein")
    @Schema(description = "每日蛋白 g（周期内固定）")
    private BigDecimal dailyProtein;

    @Schema(description = "状态：1进行中 2已完成 3已终止")
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

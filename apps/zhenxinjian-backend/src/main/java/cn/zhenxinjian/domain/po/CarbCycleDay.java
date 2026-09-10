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
 * 碳循环每日日型计划实体（日型目标创建时固化，档案变更不回改）
 * 作者: wanglx
 */
@Data
@TableName("carb_cycle_day")
@Schema(description = "碳循环每日日型计划实体")
public class CarbCycleDay implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "所属周期ID")
    private Long planId;

    @Schema(description = "归属用户ID（冗余，迁移/清理直改）")
    private Long userId;

    @Schema(description = "日序 1..N")
    private Integer dayIndex;

    @Schema(description = "日历日")
    private LocalDate dayDate;

    @Schema(description = "日型：1高碳 2中碳 3低碳")
    private Integer dayType;

    @Schema(description = "运动日：0否 1是")
    private Integer isSport;

    @TableField("carb_g")
    @Schema(description = "当日目标碳水 g")
    private BigDecimal carbG;

    @TableField("protein_g")
    @Schema(description = "当日目标蛋白 g")
    private BigDecimal proteinG;

    @TableField("fat_g")
    @Schema(description = "当日目标脂肪 g")
    private BigDecimal fatG;

    @Schema(description = "当日目标能量 kcal")
    private Integer kcal;

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

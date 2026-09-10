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
 * 饮食记录实体（三类来源单表 + 食物快照冗余，软删保历史）
 * 作者: wanglx
 */
@Data
@TableName("diet_records")
@Schema(description = "饮食记录实体")
public class DietRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属用户ID（含游客）")
    private Long userId;

    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐")
    private Integer mealType;

    @Schema(description = "来源：1内置食物 2自定义食物 3手动输入")
    private Integer source;

    @Schema(description = "食物ID（溯源参考，不回查）")
    private Long foodId;

    @Schema(description = "食物名称快照")
    private String foodName;

    @TableField("carb_100g")
    @Schema(description = "快照：碳水 g/100g")
    private BigDecimal carb100g;

    @TableField("protein_100g")
    @Schema(description = "快照：蛋白 g/100g")
    private BigDecimal protein100g;

    @TableField("fat_100g")
    @Schema(description = "快照：脂肪 g/100g")
    private BigDecimal fat100g;

    @TableField("kcal_100g")
    @Schema(description = "快照：能量 kcal/100g")
    private Integer kcal100g;

    @Schema(description = "份量克数（手动输入占位1）")
    private Double amountG;

    @Schema(description = "实际摄入碳水 g（1位小数）")
    private Double carbG;

    @Schema(description = "实际摄入蛋白 g（1位小数）")
    private Double proteinG;

    @Schema(description = "实际摄入脂肪 g（1位小数）")
    private Double fatG;

    @Schema(description = "实际摄入能量 kcal（取整）")
    private Integer kcal;

    @Schema(description = "备注（可空）")
    private String remark;

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

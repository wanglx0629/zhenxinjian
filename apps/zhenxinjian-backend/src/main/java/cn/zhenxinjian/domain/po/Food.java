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
import java.time.LocalDateTime;

/**
 * 食物库实体（内置 200 条 + 用户自定义，单表 + source 隔离；生成列 name_active 不映射）
 * 作者: wanglx
 */
@Data
@TableName("foods")
@Schema(description = "食物库实体")
public class Food implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "食物编号（内置 F001–F200；自定义为空）")
    private String code;

    @Schema(description = "分类编号：01-10")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "食物名称")
    private String name;

    @Schema(description = "别名/俗称（搜索用，可空）")
    private String alias;

    @Schema(description = "碳水化合物 g/100g（1位小数）")
    private BigDecimal carb;

    @Schema(description = "蛋白质 g/100g（1位小数）")
    private BigDecimal protein;

    @Schema(description = "脂肪 g/100g（1位小数）")
    private BigDecimal fat;

    @Schema(description = "能量 kcal/100g（按4/4/9换算参考值）")
    private Integer kcal;

    @Schema(description = "常用单份克数（默认100）")
    private BigDecimal serving;

    @Schema(description = "来源：1内置 2自定义")
    private Integer source;

    @Schema(description = "归属用户ID（仅自定义食物）")
    private Long userId;

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

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
 * 食物图片实体（按 food_code 关联 foods；生成列 code_active 不映射）
 * 作者: wanglx
 */
@Data
@TableName("food_images")
@Schema(description = "食物图片实体")
public class FoodImage implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "食物编号（内置 F001–F200）")
    private String foodCode;

    @Schema(description = "对象存储 ObjectKey（food/{code}.jpg）")
    private String objectKey;

    @Schema(description = "图片完整 URL（冗余存，直接下发）")
    private String url;

    @Schema(description = "来源：1启动预热 2人工上传")
    private Integer source;

    @Schema(description = "状态：0停用 1有效")
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

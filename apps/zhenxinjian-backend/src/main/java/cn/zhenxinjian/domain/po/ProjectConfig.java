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
 * 项目配置实体（组件凭据/运营开关；生成列 key_active 不映射；SECRET 值加密落库）
 * 作者: wanglx
 */
@Data
@TableName("project_config")
@Schema(description = "项目配置实体")
public class ProjectConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "配置键（小写点分）")
    private String configKey;

    @Schema(description = "配置值（SECRET 类型为 AES 密文，带 enc: 前缀）")
    private String configValue;

    @Schema(description = "值类型：1字符串 2数字 3布尔 4JSON 5密文")
    private Integer valueType;

    @Schema(description = "配置说明")
    private String remark;

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

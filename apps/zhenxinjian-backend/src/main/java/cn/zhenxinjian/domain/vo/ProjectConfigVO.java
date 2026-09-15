package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目配置视图对象（SECRET 值脱敏为 ****** 下发）
 * 作者: wanglx
 */
@Data
@Schema(description = "项目配置视图对象")
public class ProjectConfigVO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "配置键（小写点分）")
    private String configKey;

    @Schema(description = "配置值（SECRET 类型脱敏为 ******）")
    private String configValue;

    @Schema(description = "值类型：1字符串 2数字 3布尔 4JSON 5密文")
    private Integer valueType;

    @Schema(description = "配置说明")
    private String remark;

    @Schema(description = "状态：0停用 1有效")
    private Integer status;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

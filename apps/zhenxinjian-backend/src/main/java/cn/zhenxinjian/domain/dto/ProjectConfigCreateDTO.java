package cn.zhenxinjian.domain.dto;

import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目配置新增入参（key 与 valueType 创建后不可改）
 * 作者: wanglx
 */
@Data
@Schema(description = "项目配置新增入参")
public class ProjectConfigCreateDTO {

    @NotBlank(message = "必填参数缺失")
    @Size(max = 100, message = "配置键最长100字符")
    @Pattern(regexp = ProjectConfigKeyConstant.KEY_REGEX, message = "配置键须为小写点分格式（如 a.b.c）")
    @Schema(description = "配置键（小写点分，如 sensitive.filter.enabled）")
    private String configKey;

    @NotNull(message = "必填参数缺失")
    @Schema(description = "值类型：1字符串 2数字 3布尔 4JSON 5密文")
    private Integer valueType;

    @Size(max = 4000, message = "配置值最长4000字符")
    @Schema(description = "配置值（SECRET 类型保存时 AES 加密落库）")
    private String configValue;

    @Size(max = 255, message = "备注最长255字符")
    @Schema(description = "配置说明")
    private String remark;
}

package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目配置修改入参（key 与 valueType 不可改；SECRET 传 ****** 或空 = 保留原值）
 * 作者: wanglx
 */
@Data
@Schema(description = "项目配置修改入参")
public class ProjectConfigUpdateDTO {

    @Size(max = 4000, message = "配置值最长4000字符")
    @Schema(description = "配置值（SECRET 类型传 ****** 或空表示不改，传新值则重新加密落库）")
    private String configValue;

    @Size(max = 255, message = "备注最长255字符")
    @Schema(description = "配置说明")
    private String remark;

    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "状态非法")
    @Schema(description = "状态：0停用 1有效")
    private Integer status;
}

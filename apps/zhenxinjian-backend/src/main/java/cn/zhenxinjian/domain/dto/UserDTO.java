package cn.zhenxinjian.domain.dto;

import cn.zhenxinjian.common.constant.ExceptionConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增/修改 DTO
 * 作者: wanglx
 */
@Data
@Schema(description = "用户请求")
public class UserDTO {

    @Schema(description = "主键ID（修改时必填）")
    private Long id;

    @NotBlank(message = ExceptionConstant.USERNAME_REQUIRED)
    @Size(min = 3, max = 32, message = ExceptionConstant.USERNAME_LENGTH)
    @Schema(description = "用户名")
    private String username;

    @Size(min = 8, max = 32, message = ExceptionConstant.PASSWORD_LENGTH)
    @Schema(description = "密码（新增时必填）")
    private String password;

    @Size(max = 64, message = ExceptionConstant.PARAM_VALID_FAIL)
    @Schema(description = "昵称")
    private String nickname;

    @Email(message = ExceptionConstant.EMAIL_FORMAT_INVALID)
    @Size(max = 128, message = ExceptionConstant.EMAIL_FORMAT_INVALID)
    @Schema(description = "邮箱")
    private String email;

    @Size(max = 20, message = ExceptionConstant.PARAM_VALID_FAIL)
    @Schema(description = "手机号")
    private String phone;

    @Size(max = 512, message = ExceptionConstant.PARAM_VALID_FAIL)
    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别:0未知 1男 2女")
    private Integer gender;

    @Schema(description = "状态:0禁用 1正常")
    private Integer status;

    @Size(max = 32, message = ExceptionConstant.ROLE_INVALID)
    @Schema(description = "角色")
    private String role;

    @Size(max = 512, message = ExceptionConstant.PARAM_VALID_FAIL)
    @Schema(description = "备注")
    private String remark;
}

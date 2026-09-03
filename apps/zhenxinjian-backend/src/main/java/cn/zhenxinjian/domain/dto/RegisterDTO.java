package cn.zhenxinjian.domain.dto;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 * 作者: luote (luote) - https://luote996.cn
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = ExceptionConstant.USERNAME_REQUIRED)
    @Size(min = 3, max = 32, message = ExceptionConstant.USERNAME_LENGTH)
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = ExceptionConstant.PASSWORD_REQUIRED)
    @Size(min = 8, max = 32, message = ExceptionConstant.PASSWORD_LENGTH)
    @Schema(description = "密码")
    private String password;

    @Size(max = 64, message = ExceptionConstant.PARAM_VALID_FAIL)
    @Schema(description = "昵称")
    private String nickname;

    @Email(message = ExceptionConstant.EMAIL_FORMAT_INVALID)
    @Size(max = 128, message = ExceptionConstant.EMAIL_FORMAT_INVALID)
    @Schema(description = "邮箱")
    private String email;

    @NotBlank(message = ExceptionConstant.CAPTCHA_REQUIRED)
    @Size(max = 8, message = ExceptionConstant.CAPTCHA_INVALID)
    @Schema(description = "验证码")
    private String captcha;

    @NotBlank(message = ExceptionConstant.CAPTCHA_UUID_REQUIRED)
    @Pattern(regexp = CommonConstant.CAPTCHA_UUID_PATTERN, message = ExceptionConstant.CAPTCHA_UUID_INVALID)
    @Schema(description = "验证码UUID")
    private String captchaUuid;
}

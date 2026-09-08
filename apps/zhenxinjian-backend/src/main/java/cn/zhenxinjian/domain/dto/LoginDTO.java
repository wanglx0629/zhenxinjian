package cn.zhenxinjian.domain.dto;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求 DTO
 * 作者: wanglx
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = ExceptionConstant.USERNAME_REQUIRED)
    @Size(max = 64, message = ExceptionConstant.USERNAME_LENGTH)
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = ExceptionConstant.PASSWORD_REQUIRED)
    @Size(max = 64, message = ExceptionConstant.PASSWORD_LENGTH)
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = ExceptionConstant.CAPTCHA_REQUIRED)
    @Size(max = 8, message = ExceptionConstant.CAPTCHA_INVALID)
    @Schema(description = "验证码")
    private String captcha;

    @NotBlank(message = ExceptionConstant.CAPTCHA_UUID_REQUIRED)
    @Pattern(regexp = CommonConstant.CAPTCHA_UUID_PATTERN, message = ExceptionConstant.CAPTCHA_UUID_INVALID)
    @Schema(description = "验证码UUID")
    private String captchaUuid;
}

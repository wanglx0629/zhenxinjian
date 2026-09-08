package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图形验证码 VO
 * 作者: wanglx
 */
@Data
@Schema(description = "图形验证码")
public class CaptchaVO {

    @Schema(description = "验证码UUID")
    private String uuid;

    @Schema(description = "验证码图片Base64")
    private String image;
}

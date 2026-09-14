package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信登录请求
 * 作者: wanglx
 */
@Data
@Schema(description = "微信登录请求")
public class WechatLoginDTO {

    @Schema(description = "wx.login() 获取的临时登录凭证", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code不能为空")
    private String code;

    @Schema(description = "游客标识（游客登录时携带，触发数据迁移）")
    private String guestKey;

    @Schema(description = "微信昵称（小程序端 input type=\"nickname\" 采集，可选；缺省落默认昵称）")
    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;
}

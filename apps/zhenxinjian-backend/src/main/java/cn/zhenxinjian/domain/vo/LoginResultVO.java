package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应 VO
 * 作者: wanglx
 */
@Data
@Schema(description = "登录响应")
public class LoginResultVO {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户信息")
    private UserVO user;
}

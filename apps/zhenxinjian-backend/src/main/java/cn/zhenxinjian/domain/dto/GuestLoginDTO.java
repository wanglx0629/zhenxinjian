package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 游客身份请求
 * 作者: wanglx
 */
@Data
@Schema(description = "游客身份请求")
public class GuestLoginDTO {

    @Schema(description = "既有游客标识（首次进入不传，由服务端生成）")
    private String guestKey;
}

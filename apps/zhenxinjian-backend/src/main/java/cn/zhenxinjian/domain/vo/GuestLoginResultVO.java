package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 游客登录响应 VO
 * 作者: wanglx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "游客登录响应")
public class GuestLoginResultVO extends LoginResultVO {

    @Schema(description = "游客标识（客户端持久化，后续换 token / 登录迁移时携带）")
    private String guestKey;
}

package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象
 * 作者: wanglx
 */
@Data
@Schema(description = "用户视图")
public class UserVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "用户类型：WECHAT微信正式用户 GUEST游客")
    private String userType;

    @Schema(description = "游客到期时间（游客身份有效）")
    private LocalDateTime guestExpireAt;

    @Schema(description = "微信OpenID")
    private String wechatOpenid;

    @Schema(description = "微信UnionID")
    private String wechatUnionid;

    @Schema(description = "微信昵称")
    private String wechatNickname;

    @Schema(description = "微信头像URL")
    private String wechatAvatar;

    @Schema(description = "微信绑定:0未绑定 1已绑定")
    private Integer wechatBindStatus;

    @Schema(description = "微信绑定时间")
    private LocalDateTime wechatBindTime;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "状态:0冻结 1正常 2注销")
    private Integer status;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

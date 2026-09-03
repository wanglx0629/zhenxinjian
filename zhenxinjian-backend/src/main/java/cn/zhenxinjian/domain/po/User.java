package cn.zhenxinjian.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 作者: luote (luote) - https://luote996.cn
 */
@Data
@TableName("users")
@Schema(description = "用户实体")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

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

    @Schema(description = "性别:0未知 1男 2女")
    private Integer gender;

    @Schema(description = "状态:0禁用 1正常")
    private Integer status;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "软删除:0未删 1已删")
    private Integer deleted;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}

package cn.zhenxinjian.domain.query;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件
 * 作者: wanglx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询")
public class UserQuery extends PageQuery {

    @Size(max = CommonConstant.MAX_KEYWORD_LENGTH, message = ExceptionConstant.KEYWORD_TOO_LONG)
    @Schema(description = "关键词（用户名/昵称/邮箱模糊）")
    private String keyword;

    @Schema(description = "状态:0禁用 1正常")
    private Integer status;

    @Size(max = 32, message = ExceptionConstant.ROLE_INVALID)
    @Schema(description = "角色:USER/ADMIN")
    private String role;
}

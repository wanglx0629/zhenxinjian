package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端用户聚合视图（用户 + 身体档案 + 当前模式 + 进行中周期）
 * 作者: wanglx
 */
@Data
@Schema(description = "管理端用户聚合视图")
public class AdminUserProfileVO {

    @Schema(description = "用户基础信息")
    private UserVO user;

    @Schema(description = "身体档案（未录入为 null）")
    private BodyProfileVO bodyProfile;

    @Schema(description = "当前减脂模式:1=532 2=碳循环（无档案为 null）")
    private Integer currentMode;

    @Schema(description = "当前进行中碳循环周期（无周期或非碳循环为 null）")
    private CyclePlanVO currentPlan;
}

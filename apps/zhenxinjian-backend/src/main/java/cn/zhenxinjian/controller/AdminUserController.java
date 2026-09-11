package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.vo.AdminUserProfileVO;
import cn.zhenxinjian.domain.vo.BodyProfileVO;
import cn.zhenxinjian.domain.vo.CyclePlanVO;
import cn.zhenxinjian.service.UserService;
import cn.zhenxinjian.service.impl.BodyProfileService;
import cn.zhenxinjian.service.impl.CyclePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户聚合视图控制器（用户管理详情抽屉数据源）
 * 作者: wanglx
 */
@Tag(name = "管理端-用户聚合视图")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminUserController {

    private final UserService userService;
    private final BodyProfileService bodyProfileService;
    private final CyclePlanService cyclePlanService;

    @Operation(summary = "用户聚合视图（基础信息 + 身体档案 + 当前模式 + 进行中周期）")
    @GetMapping("/{id}/profile")
    public Result<AdminUserProfileVO> profile(@PathVariable Long id) {
        AdminUserProfileVO vo = new AdminUserProfileVO();
        vo.setUser(userService.getUserById(id));
        BodyProfileVO bodyProfile = bodyProfileService.getCurrent(id);
        if (Boolean.TRUE.equals(bodyProfile.getRecorded())) {
            vo.setBodyProfile(bodyProfile);
            vo.setCurrentMode(bodyProfile.getMode());
        }
        CyclePlanVO plan = cyclePlanService.getCurrent(id);
        if (plan.getId() != null) {
            vo.setCurrentPlan(plan);
        }
        return Result.ok(vo);
    }
}

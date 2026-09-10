package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.CyclePlanCreateDTO;
import cn.zhenxinjian.domain.vo.CyclePlanVO;
import cn.zhenxinjian.service.impl.CyclePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 碳循环周期控制器
 * 作者: wanglx
 */
@Tag(name = "碳循环周期")
@RestController
@RequestMapping("/cycle/plans")
@RequiredArgsConstructor
public class CyclePlanController {

    private final CyclePlanService cyclePlanService;

    @Operation(summary = "创建碳循环周期", description = "按图片公式计算三大池与逐日日型目标（创建时档案快照固化）；已建档校验 40601；存在进行中周期时先终止旧周期")
    @PostMapping
    public Result<CyclePlanVO> create(@Valid @RequestBody CyclePlanCreateDTO dto) {
        return Result.ok(cyclePlanService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "查询当前进行中周期", description = "周期参数 + 逐日计划 + 今日定位；无进行中周期返回空态")
    @GetMapping("/current")
    public Result<CyclePlanVO> getCurrent() {
        return Result.ok(cyclePlanService.getCurrent(UserContext.getUserId()));
    }

    @Operation(summary = "查询历史周期详情", description = "按周期ID查询（含已终止/已完成）；仅本人可查，越权统一 40605")
    @GetMapping("/{id}")
    public Result<CyclePlanVO> getById(@PathVariable Long id) {
        return Result.ok(cyclePlanService.getById(UserContext.getUserId(), id));
    }

    @Operation(summary = "终止当前周期", description = "进行中周期置为已终止（每日计划保留供历史查询）；无进行中周期幂等不报错")
    @PostMapping("/current/terminate")
    public Result<Void> terminateCurrent() {
        cyclePlanService.terminateCurrent(UserContext.getUserId());
        return Result.ok();
    }
}

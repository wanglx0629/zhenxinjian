package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.vo.Taper532VO;
import cn.zhenxinjian.service.impl.Taper532Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 532 碳水渐降推进控制器
 * 作者: wanglx
 */
@Tag(name = "532 推进")
@RestController
@RequestMapping("/taper-532")
@RequiredArgsConstructor
public class Taper532Controller {

    private final Taper532Service taper532Service;

    @Operation(summary = "四阶段计划卡与今日目标", description = "阶段 4 平台触发式 −20/−80；今日目标=基线+下调+经期上浮")
    @GetMapping("/plan")
    public Result<Taper532VO> plan() {
        return Result.ok(taper532Service.plan(UserContext.getUserId()));
    }
}
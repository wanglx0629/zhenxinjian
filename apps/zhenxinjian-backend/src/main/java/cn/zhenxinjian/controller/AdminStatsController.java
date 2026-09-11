package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.vo.DailyActiveVO;
import cn.zhenxinjian.domain.vo.EventRankVO;
import cn.zhenxinjian.domain.vo.PageRankVO;
import cn.zhenxinjian.domain.vo.StatsOverviewVO;
import cn.zhenxinjian.service.impl.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端数据看板控制器（仅管理员）
 * 作者: wanglx
 */
@Tag(name = "管理端-数据看板")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "总览卡片（今日 DAU 实时）")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(adminStatsService.overview());
    }

    @Operation(summary = "DAU 趋势（聚合表，默认 30 天）")
    @GetMapping("/active-trend")
    public Result<List<DailyActiveVO>> activeTrend(@RequestParam(defaultValue = "30") Integer days) {
        return Result.ok(adminStatsService.activeTrend(days));
    }

    @Operation(summary = "常用功能排行 TOP N")
    @GetMapping("/event-rank")
    public Result<List<EventRankVO>> eventRank(@RequestParam(defaultValue = "7") Integer days,
                                               @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(adminStatsService.eventRank(days, limit));
    }

    @Operation(summary = "页面访问排行 TOP N")
    @GetMapping("/page-rank")
    public Result<List<PageRankVO>> pageRank(@RequestParam(defaultValue = "7") Integer days,
                                             @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(adminStatsService.pageRank(days, limit));
    }

    @Operation(summary = "手工重跑某日聚合（补数，幂等）")
    @PostMapping("/aggregate")
    public Result<Void> aggregate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        adminStatsService.rerunAggregate(date);
        return Result.ok();
    }
}

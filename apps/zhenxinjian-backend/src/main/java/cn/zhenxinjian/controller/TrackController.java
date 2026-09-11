package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.service.impl.TrackEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 埋点上报控制器（登录态可选：游客/未登录引导页也可上报）
 * 作者: wanglx
 */
@Tag(name = "埋点上报")
@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackEventService trackEventService;

    @Operation(summary = "批量上报埋点事件", description = "单批≤50条；事件码白名单校验，任一非法整批拒收")
    @PostMapping("/events")
    public Result<Void> events(@Valid @RequestBody TrackEventBatchDTO dto) {
        LoginUserVO current = UserContext.get();
        trackEventService.saveBatch(
                current != null ? current.getId() : null,
                current != null ? current.getUserType() : null,
                dto);
        return Result.ok();
    }
}

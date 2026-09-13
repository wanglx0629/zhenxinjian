package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.service.impl.TrackEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "批量上报埋点事件",
            description = "单批≤50条；事件码白名单逐条校验，非法条目毒性隔离不入库，返回值携带被剔除的非法事件码（端上永久剔除）；按用户/IP 限流，超阈值 429（端上整批保留下次重试）")
    @PostMapping("/events")
    public Result<List<String>> events(@Valid @RequestBody TrackEventBatchDTO dto, HttpServletRequest request) {
        LoginUserVO current = UserContext.get();
        List<String> rejected = trackEventService.saveBatch(
                current != null ? current.getId() : null,
                current != null ? current.getUserType() : null,
                resolveClientIp(request),
                dto);
        return Result.ok(rejected);
    }

    /** 取客户端 IP（限流维度）：反向代理取 X-Forwarded-For 首跳，无代理取远端地址 */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

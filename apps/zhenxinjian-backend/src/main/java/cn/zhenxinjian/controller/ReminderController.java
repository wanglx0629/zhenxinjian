package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.ReminderSaveDTO;
import cn.zhenxinjian.domain.vo.ReminderVO;
import cn.zhenxinjian.service.impl.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 三餐饮食提醒控制器
 * 作者: wanglx
 */
@Tag(name = "三餐饮食提醒")
@RestController
@RequestMapping("/reminder")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @Operation(summary = "查询提醒设置", description = "无记录时按默认值落库（全开 08:30/12:00/18:30）；响应附订阅额度与模板ID")
    @GetMapping
    public Result<ReminderVO> get() {
        return Result.ok(reminderService.getOrCreate(UserContext.getUserId()));
    }

    @Operation(summary = "保存提醒设置", description = "总开关 + 三餐开关/时间整体保存；时间须 HH:mm，非法返回 40701")
    @PutMapping
    public Result<ReminderVO> save(@Valid @RequestBody ReminderSaveDTO dto) {
        return Result.ok(reminderService.save(UserContext.getUserId(), dto));
    }

    @Operation(summary = "订阅授权上报", description = "wx.requestSubscribeMessage 授权成功后调用，额度+1；同分钟重复上报幂等")
    @PostMapping("/subscribe")
    public Result<Void> reportSubscribe() {
        reminderService.reportSubscribe(UserContext.getUserId());
        return Result.ok();
    }
}

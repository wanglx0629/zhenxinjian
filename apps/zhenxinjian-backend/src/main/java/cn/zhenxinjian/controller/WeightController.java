package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.WeightSaveDTO;
import cn.zhenxinjian.domain.vo.AdjustLogVO;
import cn.zhenxinjian.domain.vo.WeightRecordVO;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.service.impl.WeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 体重记录与调碳日志控制器
 * 作者: wanglx
 */
@Tag(name = "体重记录")
@RestController
@RequestMapping("/weight")
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    @Operation(summary = "保存体重记录", description = "25-200kg，未来日期拒绝；同日允许多条共存（每日末值=MAX(id)）；触发平台下调/恢复判定")
    @PutMapping
    public Result<WeightRecordVO> save(@Valid @RequestBody WeightSaveDTO dto) {
        return Result.ok(weightService.save(UserContext.getUserId(), dto));
    }

    @Operation(summary = "查询体重记录", description = "按日期范围过滤，按日期倒序、同日 id 倒序；不传 limit 返回全部；统一返回 kg")
    @GetMapping
    public Result<List<WeightRecordVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(weightService.list(UserContext.getUserId(), startDate, endDate, limit));
    }

    @Operation(summary = "查询体重趋势", description = "按窗口(7/30/60/90/365/all，默认30)返回每日末值点与体重差(最早末值−最新末值)")
    @GetMapping("/trend")
    public Result<WeightTrendVO> trend(@RequestParam(required = false) String range) {
        return Result.ok(weightService.trend(UserContext.getUserId(), range));
    }

    @Operation(summary = "删除体重记录", description = "逻辑删除；越权/不存在统一 40802")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        weightService.remove(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "查询调碳日志", description = "下调/恢复动作留痕，按时间倒序")
    @GetMapping("/logs")
    public Result<List<AdjustLogVO>> logs() {
        return Result.ok(weightService.listLogs(UserContext.getUserId()));
    }
}
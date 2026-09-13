package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.DietRecordCreateDTO;
import cn.zhenxinjian.domain.dto.DietRecordUpdateDTO;
import cn.zhenxinjian.domain.vo.DietDayVO;
import cn.zhenxinjian.domain.vo.DietRecordVO;
import cn.zhenxinjian.domain.vo.DietSummaryVO;
import cn.zhenxinjian.service.impl.DietRecordService;
import cn.zhenxinjian.service.impl.DietSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 饮食记录控制器
 * 作者: wanglx
 */
@Tag(name = "饮食记录")
@RestController
@RequestMapping("/diet")
@RequiredArgsConstructor
public class DietController {

    private final DietRecordService dietRecordService;

    private final DietSummaryService dietSummaryService;

    @Operation(summary = "新增饮食记录", description = "食物库带入（按克数换算+快照冗余）或手动输入保底；未来日期拒绝")
    @PostMapping("/records")
    public Result<DietRecordVO> create(@Valid @RequestBody DietRecordCreateDTO dto) {
        return Result.ok(dietRecordService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "编辑饮食记录", description = "食物来源仅可改餐别/份量/备注（按快照重算）；手动输入可改名称与三宏热量；仅本人记录")
    @PutMapping("/records/{id}")
    public Result<DietRecordVO> update(@PathVariable Long id,
                                       @Valid @RequestBody DietRecordUpdateDTO dto) {
        return Result.ok(dietRecordService.update(UserContext.getUserId(), id, dto));
    }

    @Operation(summary = "删除饮食记录", description = "逻辑删除（软删保历史）；仅本人记录，越权统一 40504")
    @DeleteMapping("/records/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        dietRecordService.remove(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "按日查询饮食记录", description = "餐别四组分组返回（含小计与当日合计）；空日返回空分组；未来日期拒绝")
    @GetMapping("/records")
    public Result<DietDayVO> listByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dietRecordService.listByDate(UserContext.getUserId(), date));
    }

    @Operation(summary = "当日累计与目标进度", description = "已摄入合计 + user_body 目标快照 + 达成率；未建档返回空态标记")
    @GetMapping("/summary")
    public Result<DietSummaryVO> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dietSummaryService.summary(UserContext.getUserId(), date));
    }
}

package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.MenstrualSaveDTO;
import cn.zhenxinjian.domain.vo.MenstrualVO;
import cn.zhenxinjian.service.impl.MenstrualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经期管理控制器
 * 作者: wanglx
 */
@Tag(name = "经期管理")
@RestController
@RequestMapping("/menstrual")
@RequiredArgsConstructor
public class MenstrualController {

    private final MenstrualService menstrualService;

    @Operation(summary = "查询经期设置", description = "男性返回 applicable=false；女性开启后附当前阶段与上浮值")
    @GetMapping
    public Result<MenstrualVO> get() {
        return Result.ok(menstrualService.get(UserContext.getUserId()));
    }

    @Operation(summary = "保存经期设置", description = "L 21-35 / D 3-10；开启须填非未来起始日，越界返回 40801")
    @PutMapping
    public Result<MenstrualVO> save(@Valid @RequestBody MenstrualSaveDTO dto) {
        return Result.ok(menstrualService.save(UserContext.getUserId(), dto));
    }
}
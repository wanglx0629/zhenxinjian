package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.service.impl.CustomFoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自定义食物控制器（新增/编辑一体 + 软删 + 我的列表）
 * 作者: wanglx
 */
@Tag(name = "自定义食物")
@RestController
@RequestMapping("/food/custom")
@RequiredArgsConstructor
public class CustomFoodController {

    private final CustomFoodService customFoodService;

    @Operation(summary = "新增/编辑自定义食物", description = "id 为空新增，非空编辑；名称归属唯一，宏量与能量守恒后端兜底校验")
    @PostMapping
    public Result<FoodVO> save(@Valid @RequestBody CustomFoodSaveDTO dto) {
        return Result.ok(customFoodService.save(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的自定义食物列表", description = "仅返回本人活跃自定义食物")
    @GetMapping("/mine")
    public Result<List<FoodVO>> listMine() {
        return Result.ok(customFoodService.listMine(UserContext.getUserId()));
    }

    @Operation(summary = "删除自定义食物", description = "软删除，永不物理删除；仅限本人")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        customFoodService.remove(UserContext.getUserId(), id);
        return Result.ok();
    }
}

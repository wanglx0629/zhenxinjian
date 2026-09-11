package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.dto.AdminFoodSaveDTO;
import cn.zhenxinjian.domain.query.AdminFoodQuery;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.service.impl.AdminFoodService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端食物库维护控制器（仅管理员；自定义食物只读）
 * 作者: wanglx
 */
@Tag(name = "管理端-食物库维护")
@RestController
@RequestMapping("/admin/foods")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminFoodController {

    private final AdminFoodService adminFoodService;

    @Operation(summary = "分页查询食物（关键词/分类/来源/状态）")
    @GetMapping
    public Result<IPage<FoodVO>> page(@Valid @ParameterObject AdminFoodQuery query) {
        return Result.ok(adminFoodService.page(query));
    }

    @Operation(summary = "新增内置食物（code 自动顺延 F201+）")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AdminFoodSaveDTO dto) {
        adminFoodService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑内置食物")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminFoodSaveDTO dto) {
        adminFoodService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "软删内置食物")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminFoodService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "停用/启用内置食物（停用后 C 端搜索不可见）")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestParam @Min(0) @Max(1) Integer status) {
        adminFoodService.changeStatus(id, status);
        return Result.ok();
    }
}

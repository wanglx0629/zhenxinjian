package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.query.FoodSearchQuery;
import cn.zhenxinjian.domain.vo.FoodCalcVO;
import cn.zhenxinjian.domain.vo.FoodCategoryVO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.service.impl.FoodService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 食物库控制器（搜索/分类/热门/详情/试算，内置只读）
 * 作者: wanglx
 */
@Tag(name = "食物库")
@Validated
@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @Operation(summary = "食物搜索", description = "关键字匹配名称或别名，可叠加分类筛选，分页单页≤50；空关键字返回空集")
    @GetMapping("/search")
    public Result<IPage<FoodVO>> search(@Valid @ParameterObject FoodSearchQuery query) {
        return Result.ok(foodService.search(UserContext.getUserId(), query));
    }

    @Operation(summary = "分类列表", description = "10 大分类，顺序固定 01–10")
    @GetMapping("/categories")
    public Result<List<FoodCategoryVO>> categories() {
        return Result.ok(foodService.categories());
    }

    @Operation(summary = "热门食物", description = "静态高频减脂食物清单（≤12 条）")
    @GetMapping("/hot")
    public Result<List<FoodVO>> hot() {
        return Result.ok(foodService.hot());
    }

    @Operation(summary = "食物详情", description = "内置全员可见，自定义仅本人可见")
    @GetMapping("/{id}")
    public Result<FoodVO> detail(@PathVariable("id") Long id) {
        return Result.ok(foodService.detail(UserContext.getUserId(), id));
    }

    @Operation(summary = "份量试算", description = "每 100g 值 × 克数 ÷ 100，克数 1–10000")
    @GetMapping("/{id}/calc")
    public Result<FoodCalcVO> calc(@PathVariable("id") Long id,
                                   @RequestParam("grams") @Min(1) @Max(10000) Integer grams) {
        return Result.ok(foodService.calc(UserContext.getUserId(), id, grams));
    }
}

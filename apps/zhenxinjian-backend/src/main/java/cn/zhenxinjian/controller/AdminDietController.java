package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.query.AdminDietRecordQuery;
import cn.zhenxinjian.domain.vo.AdminDietRecordVO;
import cn.zhenxinjian.service.impl.AdminDietService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端饮食记录查看控制器（只读）
 * 作者: wanglx
 */
@Tag(name = "管理端-饮食记录查看")
@RestController
@RequestMapping("/admin/diet-records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminDietController {

    private final AdminDietService adminDietService;

    @Operation(summary = "分页查询饮食记录（用户关键词/日期区间/餐别）")
    @GetMapping
    public Result<IPage<AdminDietRecordVO>> page(@Valid @ParameterObject AdminDietRecordQuery query) {
        return Result.ok(adminDietService.page(query));
    }
}

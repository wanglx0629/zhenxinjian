package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.domain.dto.ProjectConfigCreateDTO;
import cn.zhenxinjian.domain.dto.ProjectConfigUpdateDTO;
import cn.zhenxinjian.domain.vo.ProjectConfigVO;
import cn.zhenxinjian.service.ConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端项目配置控制器（组件凭据/运营开关；SECRET 值脱敏下发）
 * 修改 sensitive.filter.* 配置后触发敏感词库重建
 * 作者: wanglx
 */
@Tag(name = "管理端-系统配置")
@RestController
@RequestMapping("/admin/configs")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminConfigController {

    private final ConfigService configService;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Operation(summary = "分页查询配置（关键词匹配 key/备注）")
    @GetMapping
    public Result<IPage<ProjectConfigVO>> page(
            @RequestParam(name = "keyword", required = false)
            @Size(max = 64, message = "搜索关键词过长") String keyword,
            @RequestParam(name = "page", defaultValue = "1")
            @Min(value = 1, message = "页码最小为1") long page,
            @RequestParam(name = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = 100, message = "每页条数最大为100") long size) {
        return Result.ok(configService.page(keyword, page, size));
    }

    @Operation(summary = "新增配置（key 活跃唯一；SECRET 值加密落库）")
    @PostMapping
    public Result<ProjectConfigVO> create(@Valid @RequestBody ProjectConfigCreateDTO dto) {
        ProjectConfigVO vo = configService.create(dto);
        refreshIfSensitive(vo.getConfigKey());
        return Result.ok(vo);
    }

    @Operation(summary = "修改配置（value/备注/状态；key 与类型不可改；SECRET 传 ****** = 不改）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectConfigUpdateDTO dto) {
        ProjectConfigVO vo = configService.update(id, dto);
        refreshIfSensitive(vo.getConfigKey());
        return Result.ok();
    }

    /** 敏感词相关配置变更后重建词库（写路径已失效缓存，此处读到新值） */
    private void refreshIfSensitive(String configKey) {
        if (configKey != null && configKey.startsWith(ProjectConfigKeyConstant.SENSITIVE_FILTER_PREFIX)) {
            sensitiveWordFilter.refresh();
        }
    }
}

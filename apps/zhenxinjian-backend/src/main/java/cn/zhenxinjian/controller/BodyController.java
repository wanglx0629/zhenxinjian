package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.BodyProfileSaveDTO;
import cn.zhenxinjian.domain.vo.BodyProfileVO;
import cn.zhenxinjian.service.impl.BodyProfileService;
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
 * 身体数据控制器
 * 作者: wanglx
 */
@Tag(name = "身体数据")
@RestController
@RequestMapping("/body")
@RequiredArgsConstructor
public class BodyController {

    private final BodyProfileService bodyProfileService;

    @Operation(summary = "保存身体档案", description = "录入/修改一体，幂等覆盖当前档案；旧值归档留痕，计算结果快照同步重算")
    @PutMapping("/profile")
    public Result<BodyProfileVO> saveProfile(@Valid @RequestBody BodyProfileSaveDTO dto) {
        return Result.ok(bodyProfileService.save(UserContext.getUserId(), dto));
    }

    @Operation(summary = "查询当前身体档案", description = "返回档案与计算结果快照（含低热量风险标记与免责声明）；未录入返回空态")
    @GetMapping("/profile")
    public Result<BodyProfileVO> getProfile() {
        return Result.ok(bodyProfileService.getCurrent(UserContext.getUserId()));
    }
}

package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.dto.UserDTO;
import cn.zhenxinjian.domain.query.UserQuery;
import cn.zhenxinjian.domain.vo.UserVO;
import cn.zhenxinjian.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 CRUD 控制器（仅管理员）
 * 作者: luote (luote) - https://luote996.cn
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<IPage<UserVO>> page(@Valid @ParameterObject UserQuery query) {
        return Result.ok(userService.pageUsers(query));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserDTO dto) {
        userService.addUser(dto);
        return Result.ok();
    }

    @Operation(summary = "修改用户")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserDTO dto) {
        userService.updateUser(dto);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}

package cn.zhenxinjian.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.LoginDTO;
import cn.zhenxinjian.domain.dto.RegisterDTO;
import cn.zhenxinjian.domain.vo.CaptchaVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.domain.vo.UserVO;
import cn.zhenxinjian.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（登录/注册/验证码）
 * 作者: luote (luote) - https://luote996.cn
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(userService.getCaptcha());
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String ip = JakartaServletUtil.getClientIP(request);
        return Result.ok(userService.login(dto, ip));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @Operation(summary = "用户登出", security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }

    @Operation(summary = "获取当前登录用户", security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/me")
    public Result<UserVO> me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(CommonConstant.UNAUTHORIZED_CODE, ExceptionConstant.NOT_LOGIN);
        }
        return Result.ok(userService.getUserById(userId));
    }
}

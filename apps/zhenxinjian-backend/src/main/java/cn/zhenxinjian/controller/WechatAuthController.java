package cn.zhenxinjian.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.dto.GuestLoginDTO;
import cn.zhenxinjian.domain.dto.WechatLoginDTO;
import cn.zhenxinjian.domain.vo.GuestLoginResultVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.service.WechatAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信/游客认证控制器
 * 作者: wanglx
 */
@Tag(name = "小程序认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class WechatAuthController {

    private final WechatAuthService wechatAuthService;

    @Operation(summary = "微信一键授权登录", description = "wx.login() 的 code 换 openid 建号/找号并签发 token；可选 guestKey 触发游客数据迁移")
    @PostMapping("/wechat/login")
    public Result<LoginResultVO> wechatLogin(@Valid @RequestBody WechatLoginDTO dto, HttpServletRequest request) {
        return Result.ok(wechatAuthService.wechatLogin(dto, JakartaServletUtil.getClientIP(request)));
    }

    @Operation(summary = "游客身份签发/复用", description = "首次不传 guestKey 生成新游客；带有效 guestKey 复用记录且不重置 3 天起算")
    @PostMapping("/guest")
    public Result<GuestLoginResultVO> guestLogin(@RequestBody(required = false) GuestLoginDTO dto,
                                                 HttpServletRequest request) {
        GuestLoginDTO body = dto != null ? dto : new GuestLoginDTO();
        return Result.ok(wechatAuthService.guestLogin(body, JakartaServletUtil.getClientIP(request)));
    }
}

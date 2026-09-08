package cn.zhenxinjian.service;

import cn.zhenxinjian.domain.dto.GuestLoginDTO;
import cn.zhenxinjian.domain.dto.WechatLoginDTO;
import cn.zhenxinjian.domain.vo.GuestLoginResultVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;

/**
 * 微信/游客认证 Service
 * 作者: wanglx
 */
public interface WechatAuthService {

    /** 微信登录：code2session → openid 找/建用户 → 签发 token（可选 guestKey 触发迁移） */
    LoginResultVO wechatLogin(WechatLoginDTO dto, String ip);

    /** 游客身份：签发/复用游客记录与 token */
    GuestLoginResultVO guestLogin(GuestLoginDTO dto, String ip);
}

package cn.zhenxinjian.service;

import cn.zhenxinjian.domain.dto.GuestLoginDTO;
import cn.zhenxinjian.domain.dto.WechatLoginDTO;
import cn.zhenxinjian.domain.vo.GuestLoginResultVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 微信/游客认证 Service
 * 作者: wanglx
 */
public interface WechatAuthService {

    /**
     * 微信登录：code2session → openid 找/建用户 → 签发 token（可选 guestKey 触发迁移）
     *
     * @param dto    code + 可选 nickname/guestKey
     * @param avatar 头像文件（小程序 chooseAvatar 采集，可选；非空则上传存储后落库）
     * @param ip     客户端 IP
     */
    LoginResultVO wechatLogin(WechatLoginDTO dto, MultipartFile avatar, String ip);

    /** 游客身份：签发/复用游客记录与 token */
    GuestLoginResultVO guestLogin(GuestLoginDTO dto, String ip);
}

package cn.zhenxinjian.service;

import cn.zhenxinjian.domain.dto.LoginDTO;
import cn.zhenxinjian.domain.dto.RegisterDTO;
import cn.zhenxinjian.domain.dto.UserDTO;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.query.UserQuery;
import cn.zhenxinjian.domain.vo.CaptchaVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.domain.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户 Service 接口
 * 作者: luote (luote) - https://luote996.cn
 */
public interface UserService extends IService<User> {

    /** 获取图形验证码 */
    CaptchaVO getCaptcha();

    /** 用户登录 */
    LoginResultVO login(LoginDTO dto, String ip);

    /** 用户登出 */
    void logout();

    /** 用户注册 */
    void register(RegisterDTO dto);

    /** 分页查询用户 */
    IPage<UserVO> pageUsers(UserQuery query);

    /** 获取用户详情 */
    UserVO getUserById(Long id);

    /** 新增用户 */
    void addUser(UserDTO dto);

    /** 修改用户 */
    void updateUser(UserDTO dto);

    /** 删除用户（软删除） */
    void deleteUser(Long id);
}

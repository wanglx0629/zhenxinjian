package cn.zhenxinjian.api;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.UserStatusEnum;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * API 测试公共支撑 — 测试用户插库夹具
 * （/auth/register 端点已移除，测试用户改为直插数据库；密码统一 Test@123456）
 * 作者: wanglx
 */
public abstract class ApiTestSupport {

    /** 各测试类统一测试密码 */
    protected static final String TEST_PASS = "Test@123456";

    @Autowired
    protected UserService userService;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** 幂等插入测试用户（H2 库每次重启重建，不存在残留） */
    protected void ensureUser(String username) {
        Long count = userService.count(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count == null || count == 0) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(TEST_PASS));
            user.setNickname(username);
            user.setRole(CommonConstant.ROLE_USER);
            user.setStatus(UserStatusEnum.NORMAL.getCode());
            user.setCreateBy("api-test");
            userService.save(user);
        }
    }
}

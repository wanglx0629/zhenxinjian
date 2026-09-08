package cn.zhenxinjian.common.cache;

import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 缓存预热：启动时加载热点数据
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhenxinjianCacheWarmup implements ApplicationRunner {

    private final ZhenxinjianProperties zhenxinjianProperties;
    private final UserMapper userMapper;
    private final UserCacheService userCacheService;

    @Override
    public void run(ApplicationArguments args) {
        if (!zhenxinjianProperties.getCache().isWarmupEnabled()) {
            return;
        }
        try {
            // 优先预热 admin
            User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, "admin")
                    .last("LIMIT 1"));
            if (admin == null) {
                log.warn("[cache-warmup] 未找到 admin 用户，跳过预热");
                return;
            }
            userCacheService.getById(admin.getId());
            log.info("[cache-warmup] 用户缓存预热完成, userId={}", admin.getId());
        } catch (Exception e) {
            // 预热失败不阻断应用启动（常见于库未初始化、账号不存在、短暂连库失败）
            log.warn("[cache-warmup] 预热失败，已跳过: {}", e.getMessage());
        }
    }
}

package cn.zhenxinjian.task;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.GuestMigrationOrchestrator;
import cn.zhenxinjian.service.SessionEvictor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 过期游客清理任务
 * 每日 03:00 清理到期超 7 天未合并的游客：软删用户记录 + 调用各 Migrator 的 purge 钩子
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupTask {

    /** 单批处理条数（避免无上限 selectList） */
    private static final int BATCH_SIZE = 200;

    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final SessionEvictor sessionEvictor;
    private final GuestMigrationOrchestrator guestMigrationOrchestrator;

    /**
     * 每日 03:00 执行；一期单实例部署，上多实例时换 ShedLock
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredGuests() {
        // 到期超 7 天（宽限期外）且未合并的游客
        LocalDateTime deadline = LocalDateTime.now().minusDays(CommonConstant.GUEST_GRACE_DAYS);
        int total = 0;
        while (true) {
            List<User> batch = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getUserType, CommonConstant.USER_TYPE_GUEST)
                    .isNull(User::getMergedInto)
                    .lt(User::getGuestExpireAt, deadline)
                    .last("LIMIT " + BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (User guest : batch) {
                // 业务数据清空钩子（由编排者统一驱动，顺序经 Ordered 契约化）
                guestMigrationOrchestrator.purgeAll(guest.getId());
                // 软删用户记录（@TableLogic）+ 作废会话
                userMapper.deleteById(guest.getId());
                redisUtils.removeToken(guest.getId());
                sessionEvictor.evict(guest.getId());
                log.info("过期游客已清理: guestId={}", guest.getId());
            }
            total += batch.size();
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("过期游客清理任务完成: count={}", total);
        }
    }
}

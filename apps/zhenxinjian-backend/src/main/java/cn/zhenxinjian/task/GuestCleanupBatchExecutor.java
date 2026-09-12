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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 过期游客批级清理执行器
 * 每批独立事务（独立 Bean 经代理调用，避免同类自调用事务失效），事务长度以批大小为上界
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupBatchExecutor {

    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final SessionEvictor sessionEvictor;
    private final GuestMigrationOrchestrator guestMigrationOrchestrator;

    /**
     * 选出一批到期超宽限期且未合并的游客，在本批独立事务内完成清理
     *
     * @param deadline  到期截止时刻（早于该时刻视为过期）
     * @param batchSize 单批处理条数上限
     * @return 本批清理条数（0 表示无更多过期游客）
     */
    @Transactional(rollbackFor = Exception.class)
    public int purgeOneBatch(LocalDateTime deadline, int batchSize) {
        List<User> batch = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUserType, CommonConstant.USER_TYPE_GUEST)
                .isNull(User::getMergedInto)
                .lt(User::getGuestExpireAt, deadline)
                .last("LIMIT " + batchSize));
        for (User guest : batch) {
            // 业务数据清空钩子（由编排者统一驱动，顺序经 Ordered 契约化）
            guestMigrationOrchestrator.purgeAll(guest.getId());
            // 软删用户记录（@TableLogic）+ 作废会话
            userMapper.deleteById(guest.getId());
            redisUtils.removeToken(guest.getId());
            sessionEvictor.evict(guest.getId());
            log.info("过期游客已清理: guestId={}", guest.getId());
        }
        return batch.size();
    }
}

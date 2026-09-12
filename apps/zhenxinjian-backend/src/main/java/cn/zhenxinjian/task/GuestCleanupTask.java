package cn.zhenxinjian.task;

import cn.zhenxinjian.common.constant.CommonConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 过期游客清理任务
 * 每日 03:00 清理到期超 7 天未合并的游客：软删用户记录 + 调用各 Migrator 的 purge 钩子
 * 事务边界在 {@link GuestCleanupBatchExecutor}：while 循环只编排批次，每批独立事务，事务长度有上界
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupTask {

    /** 单批处理条数（避免无上限 selectList，同时作为单事务长度上界） */
    private static final int BATCH_SIZE = 200;

    private final GuestCleanupBatchExecutor guestCleanupBatchExecutor;

    /**
     * 每日 03:00 执行；一期单实例部署，上多实例时换 ShedLock
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeExpiredGuests() {
        // 到期超 7 天（宽限期外）且未合并的游客
        LocalDateTime deadline = LocalDateTime.now().minusDays(CommonConstant.GUEST_GRACE_DAYS);
        int total = 0;
        while (true) {
            long startMs = System.currentTimeMillis();
            int count = guestCleanupBatchExecutor.purgeOneBatch(deadline, BATCH_SIZE);
            if (count == 0) {
                break;
            }
            total += count;
            log.info("过期游客清理批次完成: size={}, costMs={}", count, System.currentTimeMillis() - startMs);
            if (count < BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("过期游客清理任务完成: count={}", total);
        }
    }
}

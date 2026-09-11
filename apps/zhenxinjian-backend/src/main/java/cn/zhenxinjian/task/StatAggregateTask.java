package cn.zhenxinjian.task;

import cn.zhenxinjian.service.impl.StatAggregateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 行为统计定时聚合任务（每日 01:00 统计昨日，与提醒推送/游客清理同模式）
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatAggregateTask {

    private final StatAggregateService statAggregateService;

    /** 每日 01:00 聚合昨日数据；一期单实例部署，上多实例时换 ShedLock */
    @Scheduled(cron = "0 0 1 * * ?")
    public void aggregateYesterday() {
        try {
            statAggregateService.aggregate(LocalDate.now().minusDays(1));
        } catch (Exception e) {
            log.error("stat aggregate failed", e);
        }
    }
}

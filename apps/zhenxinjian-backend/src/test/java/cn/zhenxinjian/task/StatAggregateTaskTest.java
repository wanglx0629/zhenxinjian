package cn.zhenxinjian.task;

import cn.zhenxinjian.service.impl.StatAggregateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * StatAggregateTask 行为统计定时聚合任务单元测试
 * 覆盖：聚合口径为昨日、聚合异常被吞掉不向上抛出
 * 作者: wanglx
 */
class StatAggregateTaskTest {

    private final StatAggregateService statAggregateService = mock(StatAggregateService.class);
    private final StatAggregateTask task = new StatAggregateTask(statAggregateService);

    @Test
    void aggregateYesterdayShouldAggregateTheDayBeforeToday() {
        task.aggregateYesterday();

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(statAggregateService, times(1)).aggregate(captor.capture());
        assertEquals(LocalDate.now().minusDays(1), captor.getValue());
    }

    @Test
    void aggregateYesterdayShouldSwallowExceptionWhenServiceFails() {
        doThrow(new RuntimeException("boom")).when(statAggregateService)
                .aggregate(org.mockito.ArgumentMatchers.any(LocalDate.class));

        // 不抛异常即通过（定时任务不能因单日失败而中断调度）
        task.aggregateYesterday();

        verify(statAggregateService, times(1))
                .aggregate(org.mockito.ArgumentMatchers.any(LocalDate.class));
    }
}

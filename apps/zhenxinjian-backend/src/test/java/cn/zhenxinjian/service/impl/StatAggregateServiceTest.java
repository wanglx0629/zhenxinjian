package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.StatDailyActive;
import cn.zhenxinjian.domain.po.StatEventDaily;
import cn.zhenxinjian.mapper.StatDailyActiveMapper;
import cn.zhenxinjian.mapper.StatEventDailyMapper;
import cn.zhenxinjian.mapper.TrackEventMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 行为统计聚合服务单元测试（幂等软删重插 / 空数据零行）
 * 作者: wanglx
 */
@ExtendWith(MockitoExtension.class)
class StatAggregateServiceTest {

    @Mock
    private TrackEventMapper trackEventMapper;
    @Mock
    private StatDailyActiveMapper statDailyActiveMapper;
    @Mock
    private StatEventDailyMapper statEventDailyMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StatAggregateService statAggregateService;

    /** 场景：有数据 → 先软删当日旧行再插新行（幂等可重跑） */
    @Test
    void aggregate_softDeleteThenInsert_idempotent() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(trackEventMapper.selectObjs(any(Wrapper.class)))
                .thenReturn(List.of(5L), List.of(2L));
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(3L);
        when(trackEventMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(
                Map.of("event_code", "page_view", "event_name", "页面访问", "pv", 20L, "uv", 5L)));

        statAggregateService.aggregate(date);

        verify(statDailyActiveMapper).delete(any(Wrapper.class));
        verify(statEventDailyMapper).delete(any(Wrapper.class));
        verify(statDailyActiveMapper).insert(any(StatDailyActive.class));
        verify(statEventDailyMapper).insert(any(StatEventDaily.class));
    }

    /** 场景：当日无事件 → 活跃行插 0 值，事件聚合不插行 */
    @Test
    void aggregate_noEvents_insertZeroRow() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(trackEventMapper.selectObjs(any(Wrapper.class)))
                .thenReturn(List.of(0L), List.of(0L));
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(trackEventMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of());

        statAggregateService.aggregate(date);

        verify(statDailyActiveMapper).insert(any(StatDailyActive.class));
        verify(statEventDailyMapper, never()).insert(any(StatEventDaily.class));
    }
}

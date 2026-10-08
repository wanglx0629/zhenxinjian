package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 体重趋势聚合服务单元测试（拆自 WeightService，DESIGN-职责-001）
 * 作者: wanglx
 */
class WeightAggregateServiceTest {

    private WeightRecordMapper weightRecordMapper;
    private WeightAggregateService service;

    @BeforeEach
    void setUp() {
        weightRecordMapper = mock(WeightRecordMapper.class);
        PlateauDetector plateauDetector = mock(PlateauDetector.class);
        service = new WeightAggregateService(weightRecordMapper, plateauDetector);
    }

    /** 场景：体重下降 → delta 为正（最早末值 − 最新末值） */
    @Test
    void trend_decreased_deltaPositive() {
        LocalDate today = LocalDate.now();
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(today.minusDays(1), 57.0), record(today, 56.5)));

        WeightTrendVO vo = service.trend(1L, "7");

        assertEquals(2, vo.getPoints().size());
        assertEquals(0.5, vo.getDelta(), 0.001);
    }

    /** 场景：体重上升 → delta 为负 */
    @Test
    void trend_increased_deltaNegative() {
        LocalDate today = LocalDate.now();
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(today.minusDays(1), 57.0), record(today, 57.5)));

        WeightTrendVO vo = service.trend(1L, "7");

        assertEquals(-0.5, vo.getDelta(), 0.001);
    }

    /** 场景：窗口内不足 2 个不同日 → points/delta 为 null（曲线不渲染） */
    @Test
    void trend_lessThanTwoDays_nullPointsAndDelta() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now(), 57.0)));

        WeightTrendVO vo = service.trend(1L, "30");

        assertNull(vo.getPoints());
        assertNull(vo.getDelta());
    }

    /** 场景：默认窗口（range 为空/非法）= 近 30 天，起点 today−29 */
    @Test
    void trend_defaultRange_resolves30Days() {
        stubTwoPoints();
        service.trend(1L, null);
        assertTrendStart(LocalDate.now().minusDays(29));

        service.trend(1L, "abc");
        assertTrendStart(LocalDate.now().minusDays(29));
    }

    /** 场景：7 天窗口起点 today−6 */
    @Test
    void trend_range7_resolvesStart() {
        stubTwoPoints();
        service.trend(1L, "7");
        assertTrendStart(LocalDate.now().minusDays(6));
    }

    /** 场景：all → 起点 null（全量） */
    @Test
    void trend_all_startNull() {
        stubTwoPoints();
        service.trend(1L, "all");
        verify(weightRecordMapper).selectDailyLast(eq(1L), eq(null), eq(LocalDate.now()));
    }

    private void stubTwoPoints() {
        LocalDate today = LocalDate.now();
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(today.minusDays(1), 57.0), record(today, 56.8)));
    }

    private void assertTrendStart(LocalDate expected) {
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(weightRecordMapper, atLeastOnce())
                .selectDailyLast(eq(1L), captor.capture(), eq(LocalDate.now()));
        assertTrue(captor.getAllValues().contains(expected),
                "应含窗口起点 " + expected + "，实际: " + captor.getAllValues());
    }

    private WeightRecord record(LocalDate date, double weight) {
        WeightRecord r = new WeightRecord();
        r.setWeight(weight);
        r.setRecordDate(date);
        return r;
    }
}
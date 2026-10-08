package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.mapper.AdjustLogMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 平台/恢复判定器单元测试（拆自 WeightService，DESIGN-职责-001）
 * 作者: wanglx
 */
class PlateauDetectorTest {

    private WeightRecordMapper weightRecordMapper;
    private PlateauDetector detector;

    @BeforeEach
    void setUp() {
        weightRecordMapper = mock(WeightRecordMapper.class);
        UserBodyMapper userBodyMapper = mock(UserBodyMapper.class);
        AdjustLogMapper adjustLogMapper = mock(AdjustLogMapper.class);
        detector = new PlateauDetector(weightRecordMapper, userBodyMapper, adjustLogMapper);
    }

    /** 场景：近 7 天每日末值波动 0.1 < 0.3（≥2 个不同日）→ 平台期 */
    @Test
    void isPlateau_smallFluctuation_true() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now().minusDays(1), 57.0),
                        record(LocalDate.now(), 57.1)));

        assertTrue(detector.isPlateau(1L));
    }

    /** 场景：窗口内仅 1 个不同日 → 不判定（同日多次不干扰） */
    @Test
    void isPlateau_lessThanTwoDays_false() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now(), 57.0)));

        assertFalse(detector.isPlateau(1L));
    }

    /** 场景：每日末值波动 ≥0.3 → 不判平台 */
    @Test
    void isPlateau_fluctuationAtLeastThreshold_false() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now().minusDays(1), 57.0),
                        record(LocalDate.now(), 57.3)));

        assertFalse(detector.isPlateau(1L));
    }

    /** 口径锚点：平台判定走每日末值聚合，查询窗口为近 7 天日期区间 */
    @Test
    void isPlateau_usesDailyLastWith7DayWindow() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any())).thenReturn(List.of());

        detector.isPlateau(1L);

        LocalDate today = LocalDate.now();
        verify(weightRecordMapper).selectDailyLast(eq(1L), eq(today.minusDays(6)), eq(today));
    }

    private WeightRecord record(LocalDate date, double weight) {
        WeightRecord r = new WeightRecord();
        r.setWeight(weight);
        r.setRecordDate(date);
        return r;
    }
}
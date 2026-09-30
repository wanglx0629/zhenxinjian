package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.mapper.AdjustLogMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 体重服务单元测试（平台判定按「每日末值」近 7 天 ≥2 日 max−min<0.3；多窗口趋势与体重差）
 * 作者: wanglx
 */
class WeightServiceTest {

    private WeightRecordMapper weightRecordMapper;
    private UserBodyMapper userBodyMapper;
    private AdjustLogMapper adjustLogMapper;
    private WeightService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WeightRecord.class);

        weightRecordMapper = mock(WeightRecordMapper.class);
        userBodyMapper = mock(UserBodyMapper.class);
        adjustLogMapper = mock(AdjustLogMapper.class);
        service = new WeightService(weightRecordMapper, userBodyMapper, adjustLogMapper);
    }

    /** 场景：近 7 天每日末值波动 0.1 < 0.3（≥2 个不同日）→ 平台期 */
    @Test
    void isPlateau_smallFluctuation_true() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now().minusDays(1), 57.0),
                        record(LocalDate.now(), 57.1)));

        assertTrue(service.isPlateau(1L));
    }

    /** 场景：窗口内仅 1 个不同日 → 不判定（同日多次不干扰） */
    @Test
    void isPlateau_lessThanTwoDays_false() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now(), 57.0)));

        assertFalse(service.isPlateau(1L));
    }

    /** 场景：每日末值波动 ≥0.3 → 不判平台 */
    @Test
    void isPlateau_fluctuationAtLeastThreshold_false() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any()))
                .thenReturn(List.of(record(LocalDate.now().minusDays(1), 57.0),
                        record(LocalDate.now(), 57.3)));

        assertFalse(service.isPlateau(1L));
    }

    /** 口径锚点：平台判定走每日末值聚合，查询窗口为近 7 天日期区间 */
    @Test
    void isPlateau_usesDailyLastWith7DayWindow() {
        when(weightRecordMapper.selectDailyLast(eq(1L), any(), any())).thenReturn(List.of());

        service.isPlateau(1L);

        LocalDate today = LocalDate.now();
        verify(weightRecordMapper).selectDailyLast(eq(1L), eq(today.minusDays(6)), eq(today));
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
        // trend 内部还会以 7 天窗口调用 isPlateau，故只断言窗口起点出现在调用中
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

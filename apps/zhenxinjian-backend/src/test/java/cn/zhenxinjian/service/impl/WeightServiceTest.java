package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.WeightRecord;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 体重服务单元测试（平台判定口径：近 7 天 ≥2 条 max−min<0.3；不足不判定；查询为日期区间非条数窗口）
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

    /** 场景：近 7 天波动 0.1 < 0.3（≥2 条）→ 平台期 */
    @Test
    void isPlateau_smallFluctuation_true() {
        when(weightRecordMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(record(57.0), record(57.1), record(57.05)));

        assertTrue(service.isPlateau(1L));
    }

    /** 场景：窗口内仅 1 条 → 不判定 */
    @Test
    void isPlateau_lessThanTwoRecords_false() {
        when(weightRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(record(57.0)));

        assertFalse(service.isPlateau(1L));
    }

    /** 场景：波动 ≥0.3 → 不判平台 */
    @Test
    void isPlateau_fluctuationAtLeastThreshold_false() {
        when(weightRecordMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(record(57.0), record(57.3)));

        assertFalse(service.isPlateau(1L));
    }

    /** 口径锚点：查询按日期区间（近 7 天含今日）过滤，不用 LIMIT 条数窗口 */
    @Test
    @SuppressWarnings("unchecked")
    void isPlateau_queryUsesDateRangeNotLimit() {
        when(weightRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.isPlateau(1L);

        ArgumentCaptor<Wrapper<WeightRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(weightRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("record_date >="), "应含日期下界，实际: " + sql);
        assertTrue(sql.contains("record_date <="), "应含日期上界，实际: " + sql);
        assertFalse(sql.toUpperCase().contains("LIMIT"), "窗口内记录全量取，不应 LIMIT: " + sql);
    }

    private WeightRecord record(double weight) {
        WeightRecord r = new WeightRecord();
        r.setWeight(weight);
        r.setRecordDate(LocalDate.now());
        return r;
    }
}

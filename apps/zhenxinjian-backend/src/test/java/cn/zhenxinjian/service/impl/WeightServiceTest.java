package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 体重服务单元测试（CRUD 编排；平台判定/趋势聚合分别由 PlateauDetector / WeightAggregateService 承载）
 * 作者: wanglx
 */
class WeightServiceTest {

    private WeightRecordMapper weightRecordMapper;
    private PlateauDetector plateauDetector;
    private WeightAggregateService weightAggregateService;
    private WeightService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WeightRecord.class);

        weightRecordMapper = mock(WeightRecordMapper.class);
        plateauDetector = mock(PlateauDetector.class);
        weightAggregateService = mock(WeightAggregateService.class);
        service = new WeightService(weightRecordMapper, plateauDetector, weightAggregateService);
    }

    /** 场景：list limit 有上界封顶（超出 200 收敛为 200，防全表拖垮） */
    @Test
    @SuppressWarnings("unchecked")
    void list_limitCappedAtMax() {
        when(weightRecordMapper.selectList(any())).thenReturn(List.of());

        service.list(1L, null, null, Integer.MAX_VALUE);

        ArgumentCaptor<Wrapper<WeightRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(weightRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("LIMIT 200"), "limit 超上界应收敛为 200，实际: " + sql);
    }

    /** 场景：list 未传 limit → 不追加 LIMIT（返回全部） */
    @Test
    @SuppressWarnings("unchecked")
    void list_noLimit_noLimitClause() {
        when(weightRecordMapper.selectList(any())).thenReturn(List.of());

        service.list(1L, null, null, null);

        ArgumentCaptor<Wrapper<WeightRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(weightRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertFalse(sql.contains("LIMIT"), "未传 limit 不应追加 LIMIT，实际: " + sql);
    }

    /** 场景：trend 下沉 WeightAggregateService（仅编排委托） */
    @Test
    void trend_delegatesToAggregateService() {
        WeightTrendVO expected = new WeightTrendVO();
        when(weightAggregateService.trend(1L, "7")).thenReturn(expected);

        assertEquals(expected, service.trend(1L, "7"));
    }
}
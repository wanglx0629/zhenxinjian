package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.StatDailyActiveMapper;
import cn.zhenxinjian.mapper.StatEventDailyMapper;
import cn.zhenxinjian.mapper.TrackEventMapper;
import cn.zhenxinjian.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 管理端数据看板服务单元测试（days/limit 边界 40902 / 未来日期拒绝）
 * 作者: wanglx
 */
@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private TrackEventMapper trackEventMapper;
    @Mock
    private StatDailyActiveMapper statDailyActiveMapper;
    @Mock
    private StatEventDailyMapper statEventDailyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DietRecordMapper dietRecordMapper;
    @Mock
    private StatAggregateService statAggregateService;

    @InjectMocks
    private AdminStatsService adminStatsService;

    /** 场景：days 越界（0 / 91）→ 40902 */
    @Test
    void activeTrend_daysOutOfRange_throw40902() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminStatsService.activeTrend(0));
        assertEquals(CommonConstant.STATS_PARAM_INVALID_CODE, ex.getCode());
        assertThrows(BusinessException.class, () -> adminStatsService.activeTrend(91));
    }

    /** 场景：eventRank limit 越界（0 / 51）→ 40902 */
    @Test
    void eventRank_limitOutOfRange_throw40902() {
        assertThrows(BusinessException.class, () -> adminStatsService.eventRank(7, 0));
        assertThrows(BusinessException.class, () -> adminStatsService.eventRank(7, 51));
    }

    /** 场景：pageRank days 越界 → 40902 */
    @Test
    void pageRank_daysOutOfRange_throw40902() {
        assertThrows(BusinessException.class, () -> adminStatsService.pageRank(0, 10));
        assertThrows(BusinessException.class, () -> adminStatsService.pageRank(91, 10));
    }

    /** 场景：重跑未来日期 → 40902 */
    @Test
    void rerunAggregate_futureDate_throw40902() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminStatsService.rerunAggregate(LocalDate.now().plusDays(1)));
        assertEquals(CommonConstant.STATS_PARAM_INVALID_CODE, ex.getCode());
    }
}

package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.TrackEventEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.StatDailyActive;
import cn.zhenxinjian.domain.po.StatEventDaily;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.vo.DailyActiveVO;
import cn.zhenxinjian.domain.vo.EventRankVO;
import cn.zhenxinjian.domain.vo.PageRankVO;
import cn.zhenxinjian.domain.vo.StatsOverviewVO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.StatDailyActiveMapper;
import cn.zhenxinjian.mapper.StatEventDailyMapper;
import cn.zhenxinjian.mapper.TrackEventMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端数据看板服务（今日实时查明细表，昨日及以前查聚合表）
 * 作者: wanglx
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    /** 统计区间上限：90 天 */
    private static final int DAYS_MAX = 90;
    /** 排行条数上限：50 */
    private static final int LIMIT_MAX = 50;

    private final TrackEventMapper trackEventMapper;
    private final StatDailyActiveMapper statDailyActiveMapper;
    private final StatEventDailyMapper statEventDailyMapper;
    private final UserMapper userMapper;
    private final DietRecordMapper dietRecordMapper;
    private final StatAggregateService statAggregateService;

    /**
     * 总览卡片
     * 口径：今日 DAU 实时查 track_event（COUNT DISTINCT user_id，今日 00:00 起）；
     * 昨日 DAU 查 stat_daily_active（无行=0）；MAU = 近 30 天 track_event COUNT(DISTINCT user_id)；
     * totalUsers / totalDietRecords 全表计数
     */
    public StatsOverviewVO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayDau = countDistinctUserId(new QueryWrapper<TrackEvent>()
                .isNotNull("user_id")
                .ge("create_time", todayStart));
        Long mau = countDistinctUserId(new QueryWrapper<TrackEvent>()
                .isNotNull("user_id")
                .ge("create_time", todayStart.minusDays(29)));
        StatDailyActive yesterday = statDailyActiveMapper.selectOne(Wrappers
                .<StatDailyActive>lambdaQuery()
                .eq(StatDailyActive::getStatDate, LocalDate.now().minusDays(1))
                .last("LIMIT 1"));

        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTodayDau(todayDau.intValue());
        vo.setYesterdayDau(yesterday != null ? yesterday.getDau() : 0);
        vo.setMau(mau.intValue());
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setTotalDietRecords(dietRecordMapper.selectCount(null));
        return vo;
    }

    /**
     * DAU 趋势（聚合表，含昨日，不含今日）
     *
     * @param days 天数 1-90，越界抛 40902
     */
    public List<DailyActiveVO> activeTrend(Integer days) {
        checkDays(days);
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(days - 1L);
        List<StatDailyActive> rows = statDailyActiveMapper.selectList(Wrappers
                .<StatDailyActive>lambdaQuery()
                .between(StatDailyActive::getStatDate, start, end)
                .orderByAsc(StatDailyActive::getStatDate));
        return rows.stream().map(r -> {
            DailyActiveVO vo = new DailyActiveVO();
            vo.setStatDate(r.getStatDate());
            vo.setDau(r.getDau());
            vo.setGuestDau(r.getGuestDau());
            vo.setNewUser(r.getNewUser());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 常用功能排行 TOP N（聚合表 SUM(pv) 降序）
     *
     * @param days  天数 1-90，越界抛 40902
     * @param limit 条数 1-50，越界抛 40902
     */
    public List<EventRankVO> eventRank(Integer days, Integer limit) {
        checkDays(days);
        checkLimit(limit);
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<Map<String, Object>> rows = statEventDailyMapper.selectMaps(new QueryWrapper<StatEventDaily>()
                .select("event_code", "event_name", "SUM(pv) AS pv", "SUM(uv) AS uv")
                .ge("stat_date", start)
                .groupBy("event_code", "event_name")
                .orderByDesc("pv")
                .last("LIMIT " + limit));
        return rows.stream().map(r -> {
            EventRankVO vo = new EventRankVO();
            vo.setEventCode((String) r.get("event_code"));
            vo.setEventName((String) r.get("event_name"));
            vo.setPv(((Number) r.get("pv")).longValue());
            vo.setUv(((Number) r.get("uv")).longValue());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 页面访问排行 TOP N（明细表 page_view 事件实时聚合）
     *
     * @param days  天数 1-90，越界抛 40902
     * @param limit 条数 1-50，越界抛 40902
     */
    public List<PageRankVO> pageRank(Integer days, Integer limit) {
        checkDays(days);
        checkLimit(limit);
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Map<String, Object>> rows = trackEventMapper.selectMaps(new QueryWrapper<TrackEvent>()
                .select("page", "COUNT(*) AS pv", "COUNT(DISTINCT user_id) AS uv")
                .eq("event_code", TrackEventEnum.PAGE_VIEW.getCode())
                .isNotNull("page")
                .ge("create_time", start)
                .groupBy("page")
                .orderByDesc("pv")
                .last("LIMIT " + limit));
        return rows.stream().map(r -> {
            PageRankVO vo = new PageRankVO();
            vo.setPage((String) r.get("page"));
            vo.setPv(((Number) r.get("pv")).longValue());
            vo.setUv(((Number) r.get("uv")).longValue());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 手工重跑某日聚合（补数，幂等）
     *
     * @param date 统计日期，不可为未来，越界抛 40902
     */
    public void rerunAggregate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new BusinessException(CommonConstant.STATS_PARAM_INVALID_CODE,
                    ExceptionConstant.STATS_PARAM_INVALID);
        }
        statAggregateService.aggregate(date);
    }

    /**
     * 去重用户计数（COUNT(DISTINCT user_id)，直接取聚合单值；不用 selectCount，避免其对自定义 select 二次包裹成非法 SQL）
     */
    private Long countDistinctUserId(QueryWrapper<TrackEvent> wrapper) {
        wrapper.select("COUNT(DISTINCT user_id)");
        List<Object> rows = trackEventMapper.selectObjs(wrapper);
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return 0L;
        }
        return ((Number) rows.get(0)).longValue();
    }

    private void checkDays(Integer days) {
        if (days == null || days < 1 || days > DAYS_MAX) {
            throw new BusinessException(CommonConstant.STATS_PARAM_INVALID_CODE,
                    ExceptionConstant.STATS_PARAM_INVALID);
        }
    }

    private void checkLimit(Integer limit) {
        if (limit == null || limit < 1 || limit > LIMIT_MAX) {
            throw new BusinessException(CommonConstant.STATS_PARAM_INVALID_CODE,
                    ExceptionConstant.STATS_PARAM_INVALID);
        }
    }
}

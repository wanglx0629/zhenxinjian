package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.domain.po.StatDailyActive;
import cn.zhenxinjian.domain.po.StatEventDaily;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.mapper.StatDailyActiveMapper;
import cn.zhenxinjian.mapper.StatEventDailyMapper;
import cn.zhenxinjian.mapper.TrackEventMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 行为统计聚合服务（每日聚合 track_event → stat_daily_active / stat_event_daily）
 * 作者: wanglx
 *
 * 幂等口径：软删表禁普通 UNIQUE，先逻辑删除当日旧行再插新行，支持按日期重跑补数；
 * 活跃口径：create_time 当日且 user_id 非 NULL 去重（COUNT(DISTINCT user_id)）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatAggregateService {

    private final TrackEventMapper trackEventMapper;
    private final StatDailyActiveMapper statDailyActiveMapper;
    private final StatEventDailyMapper statEventDailyMapper;
    private final UserMapper userMapper;

    /**
     * 聚合指定日期（重跑安全：同事务内软删旧行 + 插新行）
     *
     * @param date 统计日期
     */
    @Transactional(rollbackFor = Exception.class)
    public void aggregate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // 1. 计算 DAU / guest_dau / new_user
        Long dau = countDistinctUserId(new QueryWrapper<TrackEvent>()
                .isNotNull("user_id")
                .ge("create_time", start).lt("create_time", end));
        Long guestDau = countDistinctUserId(new QueryWrapper<TrackEvent>()
                .isNotNull("user_id")
                .eq("user_type", CommonConstant.USER_TYPE_GUEST)
                .ge("create_time", start).lt("create_time", end));
        Long newUser = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreateTime, start).lt(User::getCreateTime, end));

        // 2. 活跃聚合：软删当日旧行 + 插新行
        statDailyActiveMapper.delete(Wrappers.<StatDailyActive>lambdaQuery()
                .eq(StatDailyActive::getStatDate, date));
        StatDailyActive active = new StatDailyActive();
        active.setStatDate(date);
        active.setDau(dau.intValue());
        active.setGuestDau(guestDau.intValue());
        active.setNewUser(newUser.intValue());
        active.setStatus(1);
        active.setCreateBy(CommonConstant.CREATE_BY_SYSTEM);
        statDailyActiveMapper.insert(active);

        // 3. 事件聚合：按 event_code 分组 pv/uv
        statEventDailyMapper.delete(Wrappers.<StatEventDaily>lambdaQuery()
                .eq(StatEventDaily::getStatDate, date));
        List<Map<String, Object>> rows = trackEventMapper.selectMaps(new QueryWrapper<TrackEvent>()
                .select("event_code", "event_name", "COUNT(*) AS pv", "COUNT(DISTINCT user_id) AS uv")
                .ge("create_time", start).lt("create_time", end)
                .groupBy("event_code", "event_name"));
        for (Map<String, Object> row : rows) {
            StatEventDaily daily = new StatEventDaily();
            daily.setStatDate(date);
            daily.setEventCode((String) row.get("event_code"));
            daily.setEventName((String) row.get("event_name"));
            daily.setPv(((Number) row.get("pv")).intValue());
            daily.setUv(((Number) row.get("uv")).intValue());
            daily.setStatus(1);
            daily.setCreateBy(CommonConstant.CREATE_BY_SYSTEM);
            statEventDailyMapper.insert(daily);
        }
        log.info("stat aggregate done: date={}, dau={}, events={}", date, dau, rows.size());
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
}

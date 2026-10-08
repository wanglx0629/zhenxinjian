package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 体重趋势聚合服务（窗口解析 + 每日末值聚合 + 体重差）
 * 拆自 WeightService（DESIGN-职责-001），独立承载趋势口径
 * 作者: wanglx
 */
@Service
@RequiredArgsConstructor
public class WeightAggregateService {

    /** 趋势窗口合法天数（all 单独处理） */
    private static final Set<Integer> TREND_RANGES = Set.of(7, 30, 60, 90, 365);

    /** 默认趋势窗口 */
    private static final int DEFAULT_TREND_RANGE = 30;

    private final WeightRecordMapper weightRecordMapper;

    private final PlateauDetector plateauDetector;

    /**
     * 体重趋势：按窗口取「每日末值」（时间序）并计算体重差。
     *
     * @param userId 当前用户ID
     * @param range  窗口 7/30/60/90/365/all；非法或空回退默认 30
     * @return 趋势（不足 2 个不同日时 points/delta 为 null）
     */
    public WeightTrendVO trend(Long userId, String range) {
        LocalDate today = LocalDate.now();
        LocalDate start = resolveTrendStart(range, today);

        List<WeightRecord> dailyLast = weightRecordMapper.selectDailyLast(userId, start, today);

        WeightTrendVO vo = new WeightTrendVO();
        vo.setStartDate(start == null ? null : start.toString());
        vo.setEndDate(today.toString());
        if (dailyLast.size() < 2) {
            return vo;
        }
        boolean plateauNow = plateauDetector.isPlateau(userId);
        List<WeightTrendVO.Point> points = dailyLast.stream().map(r -> {
            WeightTrendVO.Point p = new WeightTrendVO.Point();
            p.setDate(r.getRecordDate().toString());
            p.setWeight(r.getWeight());
            // 仅当前（最新）点反映当下平台态，历史点不回溯判定
            p.setPlateau(r.getRecordDate().equals(today) && plateauNow);
            return p;
        }).collect(Collectors.toList());
        vo.setPoints(points);
        double first = dailyLast.get(0).getWeight();
        double last = dailyLast.get(dailyLast.size() - 1).getWeight();
        vo.setDelta(BigDecimal.valueOf(first).subtract(BigDecimal.valueOf(last)).doubleValue());
        return vo;
    }

    /** 解析趋势窗口起点；"all" 或非法值处理 */
    private LocalDate resolveTrendStart(String range, LocalDate today) {
        if ("all".equalsIgnoreCase(range)) {
            return null;
        }
        Integer days = null;
        try {
            days = range == null ? null : Integer.valueOf(range);
        } catch (NumberFormatException ignored) {
            // 非数字且非 all → 默认窗口
        }
        if (days == null || !TREND_RANGES.contains(days)) {
            days = DEFAULT_TREND_RANGE;
        }
        return today.minusDays((long) days - 1);
    }
}
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.DietModeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.Numbers;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.vo.DietSummaryVO;
import cn.zhenxinjian.domain.vo.Taper532VO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 饮食汇总服务（当日累计与目标进度对照；未建档空态：recorded=false，目标与达成率为空）
 * 作者: wanglx
 *
 * 口径: 目标来源按模式分发（carb-cycle design §4.1）——532 取推进口径；碳循环取进行中周期当日日型目标，
 * 无周期/当日不在周期内同未建档空态口径（D3 复用 recorded=false）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietSummaryService {

    private final DietRecordMapper dietRecordMapper;

    private final UserBodyMapper userBodyMapper;

    private final CyclePlanService cyclePlanService;

    private final Taper532Service taper532Service;

    /**
     * 当日累计与目标进度
     *
     * @param userId 当前用户ID
     * @param date   查询日期（空默认当日；未来日期 40505）
     */
    public DietSummaryVO summary(Long userId, LocalDate date) {
        LocalDate queryDate = resolveDate(date);
        Map<String, Object> sums = dietRecordMapper.selectMaps(
                new QueryWrapper<DietRecord>()
                        .select("COALESCE(SUM(carb_g),0) AS carb",
                                "COALESCE(SUM(protein_g),0) AS protein",
                                "COALESCE(SUM(fat_g),0) AS fat",
                                "COALESCE(SUM(kcal),0) AS kcal")
                        .eq("user_id", userId)
                        .eq("record_date", queryDate))
                .get(0);

        DietSummaryVO vo = new DietSummaryVO();
        vo.setDate(queryDate);
        double carb = toDouble(sums.get("carb"));
        double protein = toDouble(sums.get("protein"));
        double fat = toDouble(sums.get("fat"));
        int kcal = (int) Math.round(toDouble(sums.get("kcal")));
        vo.setCarbActual(Numbers.round1(carb));
        vo.setProteinActual(Numbers.round1(protein));
        vo.setFatActual(Numbers.round1(fat));
        vo.setKcalActual(kcal);

        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        if (body == null) {
            vo.setRecorded(false);
            vo.setMode(DietModeEnum.TAPER_532.getCode());
            return vo;
        }
        Integer mode = body.getMode() == null ? DietModeEnum.TAPER_532.getCode() : body.getMode();
        vo.setMode(mode);

        // 经期上浮值（跨模式通用：碳水 +X / 热量 +Y，蛋白脂肪不变）
        MenstrualCalcService.PhaseResult phase = taper532Service.resolvePhase(body);
        int carbUplift = phase == null ? 0 : phase.carbUplift();
        int kcalUplift = phase == null ? 0 : phase.kcalUplift();

        if (DietModeEnum.CARB_CYCLE.getCode().equals(mode)) {
            CarbCycleDay day = cyclePlanService.findActiveDay(userId, queryDate);
            if (day == null) {
                vo.setRecorded(false);
                return vo;
            }
            vo.setRecorded(true);
            Double carbTarget = day.getCarbG() == null ? null
                    : Numbers.round1(day.getCarbG().doubleValue() + carbUplift);
            Double proteinTarget = day.getProteinG() == null ? null : day.getProteinG().doubleValue();
            Double fatTarget = day.getFatG() == null ? null : day.getFatG().doubleValue();
            int kcalTarget = (day.getKcal() == null ? 0 : day.getKcal()) + kcalUplift;
            vo.setCarbTarget(carbTarget);
            vo.setProteinTarget(proteinTarget);
            vo.setFatTarget(fatTarget);
            vo.setKcalTarget(kcalTarget);
            vo.setCarbRate(rate(carb, carbTarget));
            vo.setProteinRate(rate(protein, proteinTarget));
            vo.setFatRate(rate(fat, fatTarget));
            vo.setKcalRate(rate(kcal, (double) kcalTarget));
            return vo;
        }

        // 532 模式：目标取推进口径（基线 + 平台下调 + 经期上浮，蛋白脂肪不变）
        Taper532VO.TodayTarget target = taper532Service.todayTarget(userId);
        vo.setRecorded(true);
        vo.setCarbTarget(target.getCarb());
        vo.setProteinTarget(target.getProtein());
        vo.setFatTarget(target.getFat());
        vo.setKcalTarget(target.getKcal());
        vo.setCarbRate(rate(carb, target.getCarb()));
        vo.setProteinRate(rate(protein, target.getProtein()));
        vo.setFatRate(rate(fat, target.getFat()));
        vo.setKcalRate(rate(kcal, target.getKcal() == null ? null : target.getKcal().doubleValue()));
        return vo;
    }

    /** 日期解析：空默认当日；未来日期 40505（以服务端日期为准） */
    private LocalDate resolveDate(LocalDate date) {
        LocalDate value = date == null ? LocalDate.now() : date;
        if (value.isAfter(LocalDate.now())) {
            throw new BusinessException(CommonConstant.DIET_FUTURE_DATE_CODE,
                    ExceptionConstant.DIET_FUTURE_DATE);
        }
        return value;
    }

    /** 达成率%（1位小数；目标为空或 ≤0 返回 null） */
    private Double rate(double actual, Double target) {
        if (target == null || target <= 0) {
            return null;
        }
        return Numbers.round1(actual * 100 / target);
    }

    /** Number → double 安全转换（SUM 聚合可能返回 BigDecimal/Double/Long） */
    private double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }
}

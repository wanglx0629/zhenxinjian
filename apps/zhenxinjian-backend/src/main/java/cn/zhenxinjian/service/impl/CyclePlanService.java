package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.CycleDayTypeEnum;
import cn.zhenxinjian.common.enums.CyclePlanStatusEnum;
import cn.zhenxinjian.common.enums.DietModeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.domain.dto.CyclePlanCreateDTO;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.CarbCyclePlan;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.vo.CycleDayVO;
import cn.zhenxinjian.domain.vo.CyclePlanVO;
import cn.zhenxinjian.mapper.CarbCycleDayMapper;
import cn.zhenxinjian.mapper.CarbCyclePlanMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 碳循环周期服务（创建/查询/终止/模式切换；进行中唯一由服务层保证，design D2）
 * 作者: wanglx
 *
 * 口径：创建先终止进行中旧周期（spec「至多一个进行中」）；周期目标以创建时档案快照固化（D1 不回改）；
 * 模式切出碳循环自动终止进行中周期（G4 切换即重置）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CyclePlanService {

    /** 默认周期天数 */
    private static final int DEFAULT_CYCLE_DAYS = 7;

    /** 默认脂肪系数 */
    private static final BigDecimal DEFAULT_CFC = new BigDecimal("0.8");

    /** 脂肪系数可选值 */
    private static final Set<BigDecimal> CFC_OPTIONS = Set.of(new BigDecimal("0.8"), new BigDecimal("1.0"));

    private final CarbCyclePlanMapper carbCyclePlanMapper;

    private final CarbCycleDayMapper carbCycleDayMapper;

    private final UserBodyMapper userBodyMapper;

    private final CycleCalcService cycleCalcService;

    /**
     * 创建周期：校验已建档（40601）→ 终止进行中旧周期 → 计算并落 plan+days（体重/池快照）
     *
     * @param userId 当前用户ID
     * @param dto    创建入参（天数缺省 7、cfc 缺省 0.8、运动日可空）
     * @return 完整周期计划
     */
    @Transactional(rollbackFor = Exception.class)
    public CyclePlanVO create(Long userId, CyclePlanCreateDTO dto) {
        UserBody body = selectActiveBody(userId);
        if (body == null) {
            throw new BusinessException(CommonConstant.CYCLE_NOT_PROFILED_CODE,
                    ExceptionConstant.CYCLE_NOT_PROFILED);
        }
        int cycleDays = dto.getCycleDays() == null ? DEFAULT_CYCLE_DAYS : dto.getCycleDays();
        if (cycleDays < 7 || cycleDays > 14) {
            throw new BusinessException(CommonConstant.CYCLE_PARAM_INVALID_CODE,
                    ExceptionConstant.CYCLE_PARAM_INVALID);
        }
        BigDecimal cfc = dto.getCfc() == null ? DEFAULT_CFC : BigDecimal.valueOf(dto.getCfc());
        if (!CFC_OPTIONS.contains(cfc)) {
            throw new BusinessException(CommonConstant.CYCLE_PARAM_INVALID_CODE,
                    ExceptionConstant.CYCLE_PARAM_INVALID);
        }
        Set<Integer> sportDays = dto.getSportDays() == null ? Set.of() : new HashSet<>(dto.getSportDays());
        if (sportDays.size() > cycleDays) {
            throw new BusinessException(CommonConstant.CYCLE_SPORT_DAY_INVALID_CODE,
                    ExceptionConstant.CYCLE_SPORT_DAY_INVALID);
        }
        for (Integer dayIndex : sportDays) {
            if (dayIndex == null || dayIndex < 1 || dayIndex > cycleDays) {
                throw new BusinessException(CommonConstant.CYCLE_SPORT_DAY_INVALID_CODE,
                        ExceptionConstant.CYCLE_SPORT_DAY_INVALID);
            }
        }

        // 进行中唯一（D2 服务层保证）：先终止旧周期
        terminateActive(userId);

        BigDecimal currentWeight = BigDecimal.valueOf(body.getWeight());
        BigDecimal targetWeight = BigDecimal.valueOf(body.getTargetWeight());
        CycleCalcService.CycleResult result = cycleCalcService.compute(
                currentWeight, targetWeight, cycleDays, cfc, sportDays);

        LocalDate startDate = LocalDate.now();
        CarbCyclePlan plan = new CarbCyclePlan();
        plan.setUserId(userId);
        plan.setCycleDays(cycleDays);
        plan.setCfc(cfc);
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusDays(cycleDays - 1L));
        plan.setWeightSnapshot(currentWeight);
        plan.setTargetWeightSnapshot(targetWeight);
        plan.setCarbPool(result.carbPool());
        plan.setFatPool(result.fatPool());
        plan.setDailyProtein(result.dailyProtein());
        plan.setStatus(CyclePlanStatusEnum.ACTIVE.getCode());
        plan.setCreateBy(Operators.user(userId));
        carbCyclePlanMapper.insert(plan);

        List<CarbCycleDay> days = new ArrayList<>(cycleDays);
        for (CycleCalcService.DayResult d : result.days()) {
            CarbCycleDay day = new CarbCycleDay();
            day.setPlanId(plan.getId());
            day.setUserId(userId);
            day.setDayIndex(d.dayIndex());
            day.setDayDate(startDate.plusDays(d.dayIndex() - 1L));
            day.setDayType(d.dayType().getCode());
            day.setIsSport(d.sport() ? 1 : 0);
            day.setCarbG(d.carbG());
            day.setProteinG(d.proteinG());
            day.setFatG(d.fatG());
            day.setKcal(d.kcal());
            day.setStatus(1);
            day.setCreateBy(Operators.user(userId));
            carbCycleDayMapper.insert(day);
            days.add(day);
        }
        return toVO(plan, days);
    }

    /**
     * 查询当前进行中周期（plan + days + 今日定位）；无进行中返回空态（id=null）
     *
     * @param userId 当前用户ID
     * @return 周期计划（空态时仅 days 空列表）
     */
    public CyclePlanVO getCurrent(Long userId) {
        CarbCyclePlan plan = selectActivePlan(userId);
        if (plan == null) {
            CyclePlanVO empty = new CyclePlanVO();
            empty.setDays(List.of());
            return empty;
        }
        return toVO(plan, selectDays(plan.getId()));
    }

    /**
     * 查询历史周期详情（本人校验，越权/不存在统一 40605 不泄露存在性）
     *
     * @param userId 当前用户ID
     * @param id     周期ID
     * @return 周期计划
     */
    public CyclePlanVO getById(Long userId, Long id) {
        CarbCyclePlan plan = id == null ? null : carbCyclePlanMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(CommonConstant.CYCLE_NOT_OWNER_CODE,
                    ExceptionConstant.CYCLE_NOT_OWNER);
        }
        return toVO(plan, selectDays(plan.getId()));
    }

    /**
     * 终止当前进行中周期；无进行中周期幂等不报错
     *
     * @param userId 当前用户ID
     */
    public void terminateCurrent(Long userId) {
        terminateActive(userId);
    }

    /**
     * 切换减脂模式：持久化 user_body.mode；切出碳循环自动终止进行中周期（G4）
     *
     * @param userId 当前用户ID
     * @param mode   目标模式（1=532 2=碳循环；非法 40602）
     */
    @Transactional(rollbackFor = Exception.class)
    public void switchMode(Long userId, Integer mode) {
        DietModeEnum target = DietModeEnum.of(mode);
        if (target == null) {
            throw new BusinessException(CommonConstant.CYCLE_PARAM_INVALID_CODE,
                    ExceptionConstant.CYCLE_PARAM_INVALID);
        }
        UserBody body = selectActiveBody(userId);
        if (body == null) {
            throw new BusinessException(CommonConstant.CYCLE_NOT_PROFILED_CODE,
                    ExceptionConstant.CYCLE_NOT_PROFILED);
        }
        Integer current = body.getMode() == null ? DietModeEnum.TAPER_532.getCode() : body.getMode();
        if (DietModeEnum.CARB_CYCLE.getCode().equals(current)
                && !DietModeEnum.CARB_CYCLE.getCode().equals(target.getCode())) {
            terminateActive(userId);
        }
        body.setMode(target.getCode());
        body.setUpdateBy(Operators.user(userId));
        userBodyMapper.updateById(body);
    }

    /**
     * 查进行中周期指定日期的日计划行（summary 目标分发用；无进行中周期/日期不在周期内返回 null）
     *
     * @param userId 当前用户ID
     * @param date   查询日期
     * @return 日计划行或 null
     */
    public CarbCycleDay findActiveDay(Long userId, LocalDate date) {
        CarbCyclePlan active = selectActivePlan(userId);
        if (active == null) {
            return null;
        }
        return carbCycleDayMapper.selectOne(
                Wrappers.<CarbCycleDay>lambdaQuery()
                        .eq(CarbCycleDay::getPlanId, active.getId())
                        .eq(CarbCycleDay::getDayDate, date)
                        .last("LIMIT 1"));
    }

    /** 查当前活跃档案（逻辑删除由 MyBatis-Plus 全局过滤） */
    private UserBody selectActiveBody(Long userId) {
        return userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
    }

    /** 查进行中周期（无则 null） */
    private CarbCyclePlan selectActivePlan(Long userId) {
        return carbCyclePlanMapper.selectOne(
                Wrappers.<CarbCyclePlan>lambdaQuery()
                        .eq(CarbCyclePlan::getUserId, userId)
                        .eq(CarbCyclePlan::getStatus, CyclePlanStatusEnum.ACTIVE.getCode())
                        .orderByDesc(CarbCyclePlan::getId)
                        .last("LIMIT 1"));
    }

    /** 查周期逐日计划（按日序升序） */
    private List<CarbCycleDay> selectDays(Long planId) {
        return carbCycleDayMapper.selectList(
                Wrappers.<CarbCycleDay>lambdaQuery()
                        .eq(CarbCycleDay::getPlanId, planId)
                        .orderByAsc(CarbCycleDay::getDayIndex));
    }

    /** 终止进行中周期（幂等：无进行中不报错；同事务内由调用方保证） */
    private void terminateActive(Long userId) {
        CarbCyclePlan active = selectActivePlan(userId);
        if (active == null) {
            return;
        }
        active.setStatus(CyclePlanStatusEnum.TERMINATED.getCode());
        active.setUpdateBy(Operators.user(userId));
        carbCyclePlanMapper.updateById(active);
        log.info("[CyclePlan] 周期终止: userId={}, planId={}", userId, active.getId());
    }

    /** PO + days → VO（今日在周期内时回填 todayIndex） */
    private CyclePlanVO toVO(CarbCyclePlan plan, List<CarbCycleDay> days) {
        CyclePlanVO vo = new CyclePlanVO();
        vo.setId(plan.getId());
        vo.setCycleDays(plan.getCycleDays());
        vo.setCfc(plan.getCfc() == null ? null : plan.getCfc().doubleValue());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setCarbPool(plan.getCarbPool() == null ? null : plan.getCarbPool().doubleValue());
        vo.setFatPool(plan.getFatPool() == null ? null : plan.getFatPool().doubleValue());
        vo.setDailyProtein(plan.getDailyProtein() == null ? null : plan.getDailyProtein().doubleValue());
        vo.setStatus(plan.getStatus());

        LocalDate today = LocalDate.now();
        List<CycleDayVO> dayVOs = new ArrayList<>(days.size());
        for (CarbCycleDay day : days) {
            CycleDayVO d = new CycleDayVO();
            d.setDayIndex(day.getDayIndex());
            d.setDayDate(day.getDayDate());
            d.setDayType(day.getDayType());
            CycleDayTypeEnum type = CycleDayTypeEnum.of(day.getDayType());
            d.setDayTypeName(type == null ? null : type.getDesc());
            d.setIsSport(day.getIsSport());
            d.setCarbG(day.getCarbG() == null ? null : day.getCarbG().doubleValue());
            d.setProteinG(day.getProteinG() == null ? null : day.getProteinG().doubleValue());
            d.setFatG(day.getFatG() == null ? null : day.getFatG().doubleValue());
            d.setKcal(day.getKcal());
            dayVOs.add(d);
            if (today.equals(day.getDayDate())) {
                vo.setTodayIndex(day.getDayIndex());
            }
        }
        vo.setDays(dayVOs);
        return vo;
    }
}

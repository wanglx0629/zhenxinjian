package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.CycleDayTypeEnum;
import cn.zhenxinjian.common.enums.CyclePlanStatusEnum;
import cn.zhenxinjian.common.enums.DietModeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.CyclePlanCreateDTO;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.CarbCyclePlan;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.vo.CyclePlanVO;
import cn.zhenxinjian.mapper.CarbCycleDayMapper;
import cn.zhenxinjian.mapper.CarbCyclePlanMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 碳循环周期服务单元测试
 * （创建顶替旧周期 / 未建档 40601 / 参数与运动日校验 / 终止幂等 / 模式切换双向 / 历史详情越权 40605）
 * 作者: wanglx
 */
class CyclePlanServiceTest {

    private CarbCyclePlanMapper planMapper;
    private CarbCycleDayMapper dayMapper;
    private UserBodyMapper userBodyMapper;
    private CyclePlanService service;

    private final AtomicLong planIdSeq = new AtomicLong(100L);
    private final AtomicLong dayIdSeq = new AtomicLong(1000L);

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CarbCyclePlan.class);
        TableInfoHelper.initTableInfo(assistant, CarbCycleDay.class);
        TableInfoHelper.initTableInfo(assistant, UserBody.class);

        planMapper = mock(CarbCyclePlanMapper.class);
        dayMapper = mock(CarbCycleDayMapper.class);
        userBodyMapper = mock(UserBodyMapper.class);
        service = new CyclePlanService(planMapper, dayMapper, userBodyMapper, new CycleCalcService());

        when(planMapper.insert(any(CarbCyclePlan.class))).thenAnswer(inv -> {
            ((CarbCyclePlan) inv.getArgument(0)).setId(planIdSeq.incrementAndGet());
            return 1;
        });
        when(dayMapper.insert(any(CarbCycleDay.class))).thenAnswer(inv -> {
            ((CarbCycleDay) inv.getArgument(0)).setId(dayIdSeq.incrementAndGet());
            return 1;
        });
    }

    /** 金标创建：57/55 档案 + 默认 7 天/0.8 → 池 962.5/308/85.5，7 日落库，起始今日 */
    @Test
    void create_golden_persistsPlanAndDays() {
        stubBody(1L, 57.0, 55.0, DietModeEnum.TAPER_532.getCode());
        stubNoActivePlan();

        CyclePlanVO vo = service.create(1L, new CyclePlanCreateDTO());

        assertNotNull(vo.getId());
        assertEquals(7, vo.getCycleDays());
        assertEquals(962.5, vo.getCarbPool(), 0.001);
        assertEquals(308.0, vo.getFatPool(), 0.001);
        assertEquals(85.5, vo.getDailyProtein(), 0.001);
        assertEquals(CyclePlanStatusEnum.ACTIVE.getCode(), vo.getStatus());
        assertEquals(LocalDate.now(), vo.getStartDate());
        assertEquals(LocalDate.now().plusDays(6), vo.getEndDate());
        assertEquals(7, vo.getDays().size());
        assertEquals(1, vo.getTodayIndex());
        assertEquals(CycleDayTypeEnum.HIGH.getCode(), vo.getDays().get(0).getDayType());
        assertEquals(240.6, vo.getDays().get(0).getCarbG(), 0.001);
        assertEquals("高碳日", vo.getDays().get(0).getDayTypeName());

        ArgumentCaptor<CarbCyclePlan> planCaptor = ArgumentCaptor.forClass(CarbCyclePlan.class);
        verify(planMapper).insert(planCaptor.capture());
        assertEquals(57.0, planCaptor.getValue().getWeightSnapshot().doubleValue(), 0.001);
        assertEquals(55.0, planCaptor.getValue().getTargetWeightSnapshot().doubleValue(), 0.001);
        verify(dayMapper, times(7)).insert(any(CarbCycleDay.class));
    }

    /** 未建档创建 → 40601，不落库 */
    @Test
    void create_notProfiled_40601() {
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(1L, new CyclePlanCreateDTO()));
        assertEquals(CommonConstant.CYCLE_NOT_PROFILED_CODE, e.getCode());
        verify(planMapper, never()).insert(any(CarbCyclePlan.class));
    }

    /** 非法入参：天数 15 → 40602；cfc 0.9 → 40602；运动日越界 → 40604 */
    @Test
    void create_invalidParams_rejected() {
        stubBody(1L, 57.0, 55.0, DietModeEnum.TAPER_532.getCode());

        CyclePlanCreateDTO badDays = new CyclePlanCreateDTO();
        badDays.setCycleDays(15);
        BusinessException e1 = assertThrows(BusinessException.class, () -> service.create(1L, badDays));
        assertEquals(CommonConstant.CYCLE_PARAM_INVALID_CODE, e1.getCode());

        CyclePlanCreateDTO badCfc = new CyclePlanCreateDTO();
        badCfc.setCfc(0.9);
        BusinessException e2 = assertThrows(BusinessException.class, () -> service.create(1L, badCfc));
        assertEquals(CommonConstant.CYCLE_PARAM_INVALID_CODE, e2.getCode());

        CyclePlanCreateDTO badSport = new CyclePlanCreateDTO();
        badSport.setSportDays(List.of(9));
        BusinessException e3 = assertThrows(BusinessException.class, () -> service.create(1L, badSport));
        assertEquals(CommonConstant.CYCLE_SPORT_DAY_INVALID_CODE, e3.getCode());

        verify(planMapper, never()).insert(any(CarbCyclePlan.class));
    }

    /** 新建顶替：已有进行中周期 → 旧周期先置已终止，新周期成为唯一进行中 */
    @Test
    void create_replacesActivePlan() {
        stubBody(1L, 57.0, 55.0, DietModeEnum.CARB_CYCLE.getCode());
        CarbCyclePlan oldPlan = activePlan(50L, 1L);
        when(planMapper.selectOne(any(Wrapper.class))).thenReturn(oldPlan);

        CyclePlanVO vo = service.create(1L, new CyclePlanCreateDTO());

        assertEquals(CyclePlanStatusEnum.TERMINATED.getCode(), oldPlan.getStatus());
        verify(planMapper).updateById(oldPlan);
        assertNotNull(vo.getId());
        assertEquals(CyclePlanStatusEnum.ACTIVE.getCode(), vo.getStatus());
    }

    /** 当前查询：无周期 → 空态（id=null、days 空）；有周期 → 今日定位 */
    @Test
    void getCurrent_emptyAndActive() {
        stubNoActivePlan();
        CyclePlanVO empty = service.getCurrent(1L);
        assertNull(empty.getId());
        assertTrue(empty.getDays().isEmpty());
        assertNull(empty.getTodayIndex());

        CarbCyclePlan plan = activePlan(50L, 1L);
        when(planMapper.selectOne(any(Wrapper.class))).thenReturn(plan);
        when(dayMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                day(51L, plan.getId(), 1, LocalDate.now(), CycleDayTypeEnum.HIGH)));
        CyclePlanVO vo = service.getCurrent(1L);
        assertEquals(50L, vo.getId());
        assertEquals(1, vo.getTodayIndex());
    }

    /** 历史详情：本人可查；他人/不存在统一 40605 */
    @Test
    void getById_ownerCheck() {
        CarbCyclePlan plan = activePlan(50L, 2L);
        when(planMapper.selectById(50L)).thenReturn(plan);

        BusinessException e = assertThrows(BusinessException.class, () -> service.getById(1L, 50L));
        assertEquals(CommonConstant.CYCLE_NOT_OWNER_CODE, e.getCode());

        when(planMapper.selectById(99L)).thenReturn(null);
        BusinessException e2 = assertThrows(BusinessException.class, () -> service.getById(1L, 99L));
        assertEquals(CommonConstant.CYCLE_NOT_OWNER_CODE, e2.getCode());
    }

    /** 终止幂等：无进行中不报错不更新；有进行中置已终止 */
    @Test
    void terminate_idempotent() {
        stubNoActivePlan();
        service.terminateCurrent(1L);
        verify(planMapper, never()).updateById(any(CarbCyclePlan.class));

        CarbCyclePlan plan = activePlan(50L, 1L);
        when(planMapper.selectOne(any(Wrapper.class))).thenReturn(plan);
        service.terminateCurrent(1L);
        assertEquals(CyclePlanStatusEnum.TERMINATED.getCode(), plan.getStatus());
        verify(planMapper).updateById(plan);
    }

    /** 切换：碳循环 → 532 自动终止进行中周期；532 → 碳循环不触碰周期；非法模式 40602；未建档 40601 */
    @Test
    void switchMode_bidirectional() {
        // 碳循环 → 532：终止周期
        UserBody body = stubBody(1L, 57.0, 55.0, DietModeEnum.CARB_CYCLE.getCode());
        CarbCyclePlan plan = activePlan(50L, 1L);
        when(planMapper.selectOne(any(Wrapper.class))).thenReturn(plan);
        service.switchMode(1L, DietModeEnum.TAPER_532.getCode());
        assertEquals(DietModeEnum.TAPER_532.getCode(), body.getMode());
        assertEquals(CyclePlanStatusEnum.TERMINATED.getCode(), plan.getStatus());
        verify(planMapper).updateById(plan);
        verify(userBodyMapper).updateById(body);

        // 532 → 碳循环：无周期可终止，仅改模式
        UserBody body2 = stubBody(2L, 57.0, 55.0, DietModeEnum.TAPER_532.getCode());
        stubNoActivePlan();
        service.switchMode(2L, DietModeEnum.CARB_CYCLE.getCode());
        assertEquals(DietModeEnum.CARB_CYCLE.getCode(), body2.getMode());
        verify(planMapper, times(1)).updateById(any(CarbCyclePlan.class));

        // 非法模式
        BusinessException e = assertThrows(BusinessException.class, () -> service.switchMode(1L, 9));
        assertEquals(CommonConstant.CYCLE_PARAM_INVALID_CODE, e.getCode());

        // 未建档
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> service.switchMode(3L, DietModeEnum.CARB_CYCLE.getCode()));
        assertEquals(CommonConstant.CYCLE_NOT_PROFILED_CODE, e2.getCode());
    }

    /** 构造并 stub 活跃档案 */
    private UserBody stubBody(Long userId, double weight, double targetWeight, Integer mode) {
        UserBody body = new UserBody();
        body.setId(10L + userId);
        body.setUserId(userId);
        body.setWeight(weight);
        body.setTargetWeight(targetWeight);
        body.setMode(mode);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        return body;
    }

    /** stub 无进行中周期 */
    private void stubNoActivePlan() {
        when(planMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    }

    /** 构造进行中周期 */
    private CarbCyclePlan activePlan(Long id, Long userId) {
        CarbCyclePlan plan = new CarbCyclePlan();
        plan.setId(id);
        plan.setUserId(userId);
        plan.setCycleDays(7);
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(6));
        plan.setStatus(CyclePlanStatusEnum.ACTIVE.getCode());
        return plan;
    }

    /** 构造日计划行 */
    private CarbCycleDay day(Long id, Long planId, int dayIndex, LocalDate date, CycleDayTypeEnum type) {
        CarbCycleDay day = new CarbCycleDay();
        day.setId(id);
        day.setPlanId(planId);
        day.setUserId(1L);
        day.setDayIndex(dayIndex);
        day.setDayDate(date);
        day.setDayType(type.getCode());
        day.setIsSport(0);
        day.setCarbG(new java.math.BigDecimal("240.6"));
        day.setProteinG(new java.math.BigDecimal("85.5"));
        day.setFatG(new java.math.BigDecimal("23.1"));
        day.setKcal(1512);
        return day;
    }
}

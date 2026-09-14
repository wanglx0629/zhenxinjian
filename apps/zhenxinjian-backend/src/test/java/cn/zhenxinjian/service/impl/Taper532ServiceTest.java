package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserMenstrual;
import cn.zhenxinjian.domain.vo.Taper532VO;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.UserMenstrualMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 532 碳水渐降推进服务单元测试
 * 作者: wanglx
 */
class Taper532ServiceTest {

    private UserBodyMapper userBodyMapper;
    private UserMenstrualMapper userMenstrualMapper;
    private MenstrualCalcService menstrualCalcService;
    private Taper532Service service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserBody.class);
        TableInfoHelper.initTableInfo(assistant, UserMenstrual.class);

        userBodyMapper = mock(UserBodyMapper.class);
        userMenstrualMapper = mock(UserMenstrualMapper.class);
        menstrualCalcService = mock(MenstrualCalcService.class);
        service = new Taper532Service(userBodyMapper, userMenstrualMapper, menstrualCalcService);
    }

    /** 场景：未建档 → stages 空列表、today 为 null */
    @Test
    void plan_noBodyProfile_returnsEmptyStages() {
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Taper532VO result = service.plan(1L);

        assertNotNull(result);
        assertTrue(result.getStages().isEmpty());
        assertNull(result.getToday());
    }

    /** 场景：已建档无下调无经期 → 返回基线目标 */
    @Test
    void plan_withBody_returnsStagesAndToday() {
        UserBody body = body(1L, 180.0, 90.0, 45.0, 1600, 0);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Taper532VO result = service.plan(1L);

        assertNotNull(result);
        assertEquals(4, result.getStages().size());
        assertNotNull(result.getToday());
        assertEquals(180.0, result.getToday().getCarb(), 0.01);
        assertEquals(90.0, result.getToday().getProtein(), 0.01);
        assertEquals(45.0, result.getToday().getFat(), 0.01);
        assertEquals(1600, result.getToday().getKcal());
        assertFalse(result.getToday().getAdjusted());
    }

    /** 场景：已建档且平台下调态 → 碳水 −20g、热量 −80kcal，蛋白脂肪不变 */
    @Test
    void plan_adjusted_appliesDeduction() {
        UserBody body = body(1L, 180.0, 90.0, 45.0, 1600, 1);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Taper532VO result = service.plan(1L);

        assertNotNull(result.getToday());
        assertEquals(160.0, result.getToday().getCarb(), 0.01);
        assertEquals(90.0, result.getToday().getProtein(), 0.01);
        assertEquals(45.0, result.getToday().getFat(), 0.01);
        assertEquals(1520, result.getToday().getKcal());
        assertTrue(result.getToday().getAdjusted());
    }

    /** 场景：经期阶段上浮碳水与热量 */
    @Test
    void plan_withMenstrualPhase_appliesUplift() {
        UserBody body = body(1L, 180.0, 90.0, 45.0, 1600, 0);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        UserMenstrual menstrual = new UserMenstrual();
        menstrual.setEnabled(1);
        menstrual.setCycleLen(28);
        menstrual.setPeriodDays(5);
        menstrual.setPeriodStartDate(java.time.LocalDate.now().minusDays(3));
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(menstrual);

        cn.zhenxinjian.common.enums.MenstrualPhaseEnum phaseEnum =
                cn.zhenxinjian.common.enums.MenstrualPhaseEnum.MENSTRUAL;
        MenstrualCalcService.PhaseResult phase = new MenstrualCalcService.PhaseResult(4, phaseEnum);
        when(menstrualCalcService.resolvePhase(any(GenderEnum.class), anyBoolean(),
                any(LocalDate.class), anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(phase);

        Taper532VO result = service.plan(1L);

        assertNotNull(result.getToday());
        int expectedCarb = (int)(180.0 + phase.carbUplift());
        int expectedKcal = 1600 + phase.kcalUplift();
        assertEquals(expectedCarb, result.getToday().getCarb(), 0.01);
        assertEquals(expectedKcal, result.getToday().getKcal());
        assertNotNull(result.getToday().getPhaseKey());
    }

    /** 场景：todayTarget 未建档 → null */
    @Test
    void todayTarget_noBody_returnsNull() {
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Taper532VO.TodayTarget result = service.todayTarget(1L);

        assertNull(result);
    }

    /** 场景：todayTarget 有建档 → 返回目标值 */
    @Test
    void todayTarget_withBody_returnsTarget() {
        UserBody body = body(1L, 180.0, 90.0, 45.0, 1600, 0);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Taper532VO.TodayTarget result = service.todayTarget(1L);

        assertNotNull(result);
        assertEquals(180.0, result.getCarb(), 0.01);
    }

    /** 场景：阶段卡文案包含动态经期信息（有 phase） */
    @Test
    void plan_stage2desc_containsPhaseInfo() {
        UserBody body = body(1L, 180.0, 90.0, 45.0, 1600, 0);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        UserMenstrual menstrual = new UserMenstrual();
        menstrual.setEnabled(1);
        menstrual.setCycleLen(28);
        menstrual.setPeriodDays(5);
        menstrual.setPeriodStartDate(java.time.LocalDate.now().minusDays(5));
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(menstrual);

        cn.zhenxinjian.common.enums.MenstrualPhaseEnum phaseEnum =
                cn.zhenxinjian.common.enums.MenstrualPhaseEnum.FOLLICULAR;
        MenstrualCalcService.PhaseResult phase = new MenstrualCalcService.PhaseResult(6, phaseEnum);
        when(menstrualCalcService.resolvePhase(any(GenderEnum.class), anyBoolean(),
                any(LocalDate.class), anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(phase);

        Taper532VO result = service.plan(1L);

        Taper532VO.StageItem stage2 = result.getStages().get(1);
        assertTrue(stage2.getDesc().contains("第 " + phase.dayIdx() + " 天"));
    }

    private UserBody body(Long userId, double targetCarb, double targetProtein, double targetFat,
                          int targetKcal, int isAdjusted) {
        UserBody b = new UserBody();
        b.setId(userId);
        b.setUserId(userId);
        b.setGender(2); // FEMALE — phase uplift requires female
        b.setWeight(57.0);
        b.setTargetWeight(55.0);
        b.setTargetCarb(targetCarb);
        b.setTargetProtein(targetProtein);
        b.setTargetFat(targetFat);
        b.setTargetKcal(targetKcal);
        b.setIsAdjusted(isAdjusted);
        return b;
    }
}
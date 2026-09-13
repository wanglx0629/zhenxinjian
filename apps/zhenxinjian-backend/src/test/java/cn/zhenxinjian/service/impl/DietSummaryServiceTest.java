package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.DietModeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.vo.DietSummaryVO;
import cn.zhenxinjian.domain.vo.Taper532VO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 饮食汇总服务单元测试（B-T21 自 DietRecordService 拆出）
 * （532 推进口径分发 / 碳循环日型分发 / 未建档空态 / 未来日期 40505）
 * 作者: wanglx
 */
class DietSummaryServiceTest {

    private DietRecordMapper dietRecordMapper;
    private UserBodyMapper userBodyMapper;
    private CyclePlanService cyclePlanService;
    private Taper532Service taper532Service;
    private DietSummaryService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DietRecord.class);
        TableInfoHelper.initTableInfo(assistant, UserBody.class);

        dietRecordMapper = mock(DietRecordMapper.class);
        userBodyMapper = mock(UserBodyMapper.class);
        cyclePlanService = mock(CyclePlanService.class);
        taper532Service = mock(Taper532Service.class);
        service = new DietSummaryService(dietRecordMapper, userBodyMapper, cyclePlanService, taper532Service);
    }

    /** 场景：summary 有档案 → 目标与达成率正常（碳水 120/190.1 ≈ 63.1%） */
    @Test
    void summary_withProfile_returnsTargetsAndRates() {
        when(dietRecordMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(sumMap("120", "60", "30", "1000")));
        UserBody body = new UserBody();
        body.setTargetCarb(190.1);
        body.setTargetProtein(114.1);
        body.setTargetFat(33.8);
        body.setTargetKcal(1521);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        // 532 目标由 Taper532Service 推进口径提供
        Taper532VO.TodayTarget todayTarget = new Taper532VO.TodayTarget();
        todayTarget.setCarb(190.1);
        todayTarget.setProtein(114.1);
        todayTarget.setFat(33.8);
        todayTarget.setKcal(1521);
        when(taper532Service.todayTarget(1L)).thenReturn(todayTarget);

        DietSummaryVO vo = service.summary(1L, LocalDate.now().minusDays(7));

        assertTrue(vo.getRecorded());
        assertEquals(DietModeEnum.TAPER_532.getCode(), vo.getMode());
        assertEquals(120.0, vo.getCarbActual(), 0.001);
        assertEquals(190.1, vo.getCarbTarget(), 0.001);
        assertEquals(63.1, vo.getCarbRate(), 0.05);
        assertEquals(1521, vo.getKcalTarget());
        assertEquals(65.7, vo.getKcalRate(), 0.05);
    }

    /** 场景：summary 未建档 → 空态，目标与达成率为空 */
    @Test
    void summary_withoutProfile_emptyState() {
        when(dietRecordMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(sumMap("50", "20", "10", "400")));
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        DietSummaryVO vo = service.summary(1L, null);

        assertFalse(vo.getRecorded());
        assertEquals(50.0, vo.getCarbActual(), 0.001);
        assertNull(vo.getCarbTarget());
        assertNull(vo.getCarbRate());
        assertNull(vo.getKcalTarget());
    }

    /** 场景：summary 未来日期 → 40505 */
    @Test
    void summary_futureDate_throws40505() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.summary(1L, LocalDate.now().plusDays(1)));
        assertEquals(CommonConstant.DIET_FUTURE_DATE_CODE, e.getCode());
    }

    /** 场景：碳循环模式 → 目标取当日日型行（高碳日 240.6/85.5/23.1/1512），mode=2 */
    @Test
    void summary_carbCycleMode_dispatchesToCycleDay() {
        LocalDate today = LocalDate.now();
        when(dietRecordMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(sumMap("120", "60", "30", "1000")));
        UserBody body = new UserBody();
        body.setMode(DietModeEnum.CARB_CYCLE.getCode());
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        CarbCycleDay day = new CarbCycleDay();
        day.setCarbG(new BigDecimal("240.6"));
        day.setProteinG(new BigDecimal("85.5"));
        day.setFatG(new BigDecimal("23.1"));
        day.setKcal(1512);
        when(cyclePlanService.findActiveDay(1L, today)).thenReturn(day);

        DietSummaryVO vo = service.summary(1L, today);

        assertTrue(vo.getRecorded());
        assertEquals(DietModeEnum.CARB_CYCLE.getCode(), vo.getMode());
        assertEquals(240.6, vo.getCarbTarget(), 0.001);
        assertEquals(85.5, vo.getProteinTarget(), 0.001);
        assertEquals(23.1, vo.getFatTarget(), 0.001);
        assertEquals(1512, vo.getKcalTarget());
        assertEquals(49.9, vo.getCarbRate(), 0.05);
        // 532 档案快照不参与分发
        assertNull(body.getTargetCarb());
    }

    /** 场景：碳循环模式无进行中周期 → 空态（recorded=false，目标为空），不报错 */
    @Test
    void summary_carbCycleNoPlan_emptyState() {
        LocalDate today = LocalDate.now();
        when(dietRecordMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(sumMap("50", "20", "10", "400")));
        UserBody body = new UserBody();
        body.setMode(DietModeEnum.CARB_CYCLE.getCode());
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        when(cyclePlanService.findActiveDay(1L, today)).thenReturn(null);

        DietSummaryVO vo = service.summary(1L, today);

        assertFalse(vo.getRecorded());
        assertEquals(DietModeEnum.CARB_CYCLE.getCode(), vo.getMode());
        assertEquals(50.0, vo.getCarbActual(), 0.001);
        assertNull(vo.getCarbTarget());
        assertNull(vo.getKcalTarget());
    }

    // ==================== 构造辅助 ====================

    private Map<String, Object> sumMap(String carb, String protein, String fat, String kcal) {
        Map<String, Object> map = new HashMap<>();
        map.put("carb", new BigDecimal(carb));
        map.put("protein", new BigDecimal(protein));
        map.put("fat", new BigDecimal(fat));
        map.put("kcal", new BigDecimal(kcal));
        return map;
    }
}

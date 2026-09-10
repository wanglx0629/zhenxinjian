package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.DietModeEnum;
import cn.zhenxinjian.common.enums.DietRecordSourceEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.enums.MealTypeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.DietRecordCreateDTO;
import cn.zhenxinjian.domain.dto.DietRecordUpdateDTO;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.vo.DietRecordVO;
import cn.zhenxinjian.domain.vo.DietSummaryVO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.FoodMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 饮食记录服务单元测试
 * （换算精度金标 / 守恒 ±10% 边界 / 越权 40504 / 未来日期 40505 / 快照不随食物变更 / summary 建档与空态）
 * 作者: wanglx
 */
class DietRecordServiceTest {

    private DietRecordMapper dietRecordMapper;
    private FoodMapper foodMapper;
    private UserBodyMapper userBodyMapper;
    private CyclePlanService cyclePlanService;
    private DietRecordService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DietRecord.class);
        TableInfoHelper.initTableInfo(assistant, UserBody.class);

        dietRecordMapper = mock(DietRecordMapper.class);
        foodMapper = mock(FoodMapper.class);
        userBodyMapper = mock(UserBodyMapper.class);
        cyclePlanService = mock(CyclePlanService.class);
        service = new DietRecordService(dietRecordMapper, foodMapper, userBodyMapper, cyclePlanService);
    }

    /** 场景：150g 鸡胸肉（24.6/1.9/0.6/118 每100g）→ 摄入 36.9/2.9/0.9/177（±0.1g/±1kcal） */
    @Test
    void create_chickenBreast150g_goldenValues() {
        Food chicken = food(11L, FoodSourceEnum.BUILT_IN.getCode(), null,
                "0.6", "24.6", "1.9", 118);
        when(foodMapper.selectById(11L)).thenReturn(chicken);
        when(dietRecordMapper.insert(any(DietRecord.class))).thenAnswer(inv -> {
            ((DietRecord) inv.getArgument(0)).setId(99L);
            return 1;
        });
        when(dietRecordMapper.selectById(99L)).thenAnswer(inv -> {
            DietRecord r = persisted(inv.getArgument(0));
            r.setCarbG(0.9);
            r.setProteinG(36.9);
            r.setFatG(2.9);
            r.setKcal(177);
            return r;
        });

        DietRecordCreateDTO dto = foodDto(11L, "150");
        DietRecordVO vo = service.create(1L, dto);

        // insert 前的实体即换算结果（快照与摄入一次落定）
        org.mockito.ArgumentCaptor<DietRecord> captor = org.mockito.ArgumentCaptor.forClass(DietRecord.class);
        verify(dietRecordMapper).insert(captor.capture());
        DietRecord saved = captor.getValue();
        assertEquals(0.9, saved.getCarbG(), 0.001);
        assertEquals(36.9, saved.getProteinG(), 0.001);
        assertEquals(2.9, saved.getFatG(), 0.001);
        assertEquals(177, saved.getKcal());
        assertEquals("鸡胸肉", saved.getFoodName());
        assertEquals(DietRecordSourceEnum.BUILT_IN_FOOD.getCode(), saved.getSource());
        assertEquals(36.9, vo.getProteinG(), 0.001);
    }

    /** 场景：他人自定义食物 → 40506（不泄露存在性） */
    @Test
    void create_othersCustomFood_throws40506() {
        Food others = food(12L, FoodSourceEnum.CUSTOM.getCode(), 2L, "10", "10", "10", 170);
        when(foodMapper.selectById(12L)).thenReturn(others);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(1L, foodDto(12L, "100")));
        assertEquals(CommonConstant.DIET_FOOD_INVALID_CODE, e.getCode());
        verify(dietRecordMapper, never()).insert(any(DietRecord.class));
    }

    /** 场景：份量 0 或超 5000 → 40501 */
    @Test
    void create_amountInvalid_throws40501() {
        Food chicken = food(11L, FoodSourceEnum.BUILT_IN.getCode(), null, "0.6", "24.6", "1.9", 118);
        when(foodMapper.selectById(11L)).thenReturn(chicken);

        BusinessException e1 = assertThrows(BusinessException.class,
                () -> service.create(1L, foodDto(11L, "0")));
        assertEquals(CommonConstant.DIET_AMOUNT_INVALID_CODE, e1.getCode());
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> service.create(1L, foodDto(11L, "6000")));
        assertEquals(CommonConstant.DIET_AMOUNT_INVALID_CODE, e2.getCode());
    }

    /** 场景：手动输入守恒临界（期望 58.2，标 53，偏差 8.9% ≤10%）→ 通过 */
    @Test
    void create_manualWithinTolerance_pass() {
        when(dietRecordMapper.insert(any(DietRecord.class))).thenAnswer(inv -> {
            ((DietRecord) inv.getArgument(0)).setId(98L);
            return 1;
        });
        when(dietRecordMapper.selectById(98L)).thenReturn(persisted(98L));

        DietRecordCreateDTO dto = manualDto("苹果", "13.8", "0.3", "0.2", 53);
        service.create(1L, dto);

        verify(dietRecordMapper).insert(any(DietRecord.class));
    }

    /** 场景：手动输入不守恒（期望 500 却标 100）→ 40503 */
    @Test
    void create_manualNotConserved_throws40503() {
        DietRecordCreateDTO dto = manualDto("假食物", "50", "30", "20", 100);
        BusinessException e = assertThrows(BusinessException.class, () -> service.create(1L, dto));
        assertEquals(CommonConstant.DIET_KCAL_MISMATCH_CODE, e.getCode());
        verify(dietRecordMapper, never()).insert(any(DietRecord.class));
    }

    /** 场景：未来日期 → 40505 */
    @Test
    void create_futureDate_throws40505() {
        DietRecordCreateDTO dto = manualDto("苹果", "13.8", "0.3", "0.2", 53);
        dto.setRecordDate(LocalDate.now().plusDays(1));
        BusinessException e = assertThrows(BusinessException.class, () -> service.create(1L, dto));
        assertEquals(CommonConstant.DIET_FUTURE_DATE_CODE, e.getCode());
    }

    /** 场景：编辑他人记录 → 40504（不泄露存在性） */
    @Test
    void update_othersRecord_throws40504() {
        DietRecord record = persisted(88L);
        record.setUserId(2L);
        when(dietRecordMapper.selectById(88L)).thenReturn(record);

        DietRecordUpdateDTO dto = new DietRecordUpdateDTO();
        dto.setMealType(MealTypeEnum.LUNCH.getCode());
        dto.setAmountG(new BigDecimal("200"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.update(1L, 88L, dto));
        assertEquals(CommonConstant.DIET_RECORD_NOT_FOUND_CODE, e.getCode());
        verify(dietRecordMapper, never()).updateById(any(DietRecord.class));
    }

    /** 场景：删除不存在记录 → 40504 */
    @Test
    void remove_notFound_throws40504() {
        when(dietRecordMapper.selectById(77L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class, () -> service.remove(1L, 77L));
        assertEquals(CommonConstant.DIET_RECORD_NOT_FOUND_CODE, e.getCode());
    }

    /** 场景：编辑食物来源记录按快照重算，不回查食物表（快照不随食物变更） */
    @Test
    void update_foodRecord_recalcFromSnapshotWithoutFoodQuery() {
        DietRecord record = persisted(66L);
        record.setSource(DietRecordSourceEnum.BUILT_IN_FOOD.getCode());
        record.setFoodId(11L);
        record.setFoodName("鸡胸肉");
        record.setCarb100g(new BigDecimal("0.6"));
        record.setProtein100g(new BigDecimal("24.6"));
        record.setFat100g(new BigDecimal("1.9"));
        record.setKcal100g(118);
        record.setAmountG(150.0);
        record.setCarbG(0.9);
        record.setProteinG(36.9);
        record.setFatG(2.9);
        record.setKcal(177);
        when(dietRecordMapper.selectById(66L)).thenReturn(record);

        DietRecordUpdateDTO dto = new DietRecordUpdateDTO();
        dto.setMealType(MealTypeEnum.DINNER.getCode());
        dto.setAmountG(new BigDecimal("200"));
        service.update(1L, 66L, dto);

        // 不回查食物表
        verify(foodMapper, never()).selectById(any(Long.class));
        // 快照 200g 重算：24.6×2=49.2、1.9×2=3.8、0.6×2=1.2、118×2=236
        org.mockito.ArgumentCaptor<DietRecord> captor = org.mockito.ArgumentCaptor.forClass(DietRecord.class);
        verify(dietRecordMapper).updateById(captor.capture());
        DietRecord updated = captor.getValue();
        assertEquals(49.2, updated.getProteinG(), 0.001);
        assertEquals(3.8, updated.getFatG(), 0.001);
        assertEquals(1.2, updated.getCarbG(), 0.001);
        assertEquals(236, updated.getKcal());
        assertEquals(MealTypeEnum.DINNER.getCode(), updated.getMealType());
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

    private Food food(Long id, Integer source, Long userId,
                      String carb, String protein, String fat, int kcal) {
        Food food = new Food();
        food.setId(id);
        food.setName("鸡胸肉");
        food.setSource(source);
        food.setUserId(userId);
        food.setCarb(new BigDecimal(carb));
        food.setProtein(new BigDecimal(protein));
        food.setFat(new BigDecimal(fat));
        food.setKcal(kcal);
        return food;
    }

    private DietRecordCreateDTO foodDto(Long foodId, String amount) {
        DietRecordCreateDTO dto = new DietRecordCreateDTO();
        dto.setMealType(MealTypeEnum.LUNCH.getCode());
        dto.setSource(DietRecordSourceEnum.BUILT_IN_FOOD.getCode());
        dto.setFoodId(foodId);
        dto.setAmountG(new BigDecimal(amount));
        return dto;
    }

    private DietRecordCreateDTO manualDto(String name, String carb, String protein, String fat, int kcal) {
        DietRecordCreateDTO dto = new DietRecordCreateDTO();
        dto.setMealType(MealTypeEnum.BREAKFAST.getCode());
        dto.setSource(DietRecordSourceEnum.MANUAL.getCode());
        dto.setName(name);
        dto.setCarb(new BigDecimal(carb));
        dto.setProtein(new BigDecimal(protein));
        dto.setFat(new BigDecimal(fat));
        dto.setKcal(kcal);
        return dto;
    }

    private DietRecord persisted(Long id) {
        DietRecord record = new DietRecord();
        record.setId(id);
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType(MealTypeEnum.LUNCH.getCode());
        record.setSource(DietRecordSourceEnum.MANUAL.getCode());
        record.setFoodName("苹果");
        record.setAmountG(1.0);
        record.setCarbG(13.8);
        record.setProteinG(0.3);
        record.setFatG(0.2);
        record.setKcal(53);
        return record;
    }

    private Map<String, Object> sumMap(String carb, String protein, String fat, String kcal) {
        Map<String, Object> map = new HashMap<>();
        map.put("carb", new BigDecimal(carb));
        map.put("protein", new BigDecimal(protein));
        map.put("fat", new BigDecimal(fat));
        map.put("kcal", new BigDecimal(kcal));
        return map;
    }
}

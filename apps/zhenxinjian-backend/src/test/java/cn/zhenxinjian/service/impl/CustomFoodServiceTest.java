package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自定义食物服务单元测试（宏量区间 40402 / 能量守恒 40403 / 归属 40405 / 内置拒绝 40404 / 重名 40401 / 列表有界 LIMIT 200）
 * 作者: wanglx
 */
class CustomFoodServiceTest {

    private FoodMapper foodMapper;
    private SensitiveWordFilter sensitiveWordFilter;
    private CustomFoodService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);

        foodMapper = mock(FoodMapper.class);
        sensitiveWordFilter = mock(SensitiveWordFilter.class);
        service = new CustomFoodService(foodMapper, sensitiveWordFilter);
    }

    /** 场景：能量不守恒（守恒值 170 却标 500）→ 40403 */
    @Test
    void save_kcalNotConserved_throws40403() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 500);
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_KCAL_MISMATCH_CODE, e.getCode());
        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 场景：守恒临界（期望 170，偏差 10% 内）→ 通过 */
    @Test
    void save_kcalWithinTolerance_pass() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 185);
        Food created = customFood(11L, 1L, dto.getName());
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            ((Food) inv.getArgument(0)).setId(11L);
            return 1;
        });
        when(foodMapper.selectById(11L)).thenReturn(created);

        service.save(1L, dto);

        verify(foodMapper).insert(any(Food.class));
    }

    /** 场景：宏量负值 → 40402 */
    @Test
    void save_negativeMacro_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "-1", "10", "2", 100);
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
    }

    /** 场景：宏量超 100 → 40402 */
    @Test
    void save_macroOver100_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "101", "10", "2", 100);
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
    }

    /** 场景：能量超 900 → 40402 */
    @Test
    void save_kcalOver900_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 901);
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
    }

    /** 场景：单份克数低于 5 → 40402 */
    @Test
    void save_servingTooSmall_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setServing(new BigDecimal("4"));
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
    }

    /** 场景：名称归属唯一冲突 → 40401（不抛 500） */
    @Test
    void save_duplicateName_throws40401() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        when(foodMapper.insert(any(Food.class))).thenThrow(new DuplicateKeyException("dup"));
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_NAME_DUPLICATE_CODE, e.getCode());
    }

    /** 场景：编辑他人自定义食物 → 40405 */
    @Test
    void save_editOthersFood_throws40405() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setId(99L);
        when(foodMapper.selectById(99L)).thenReturn(customFood(99L, 2L, "别人的食物"));
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_NOT_OWNER_CODE, e.getCode());
    }

    /** 场景：编辑内置食物 → 40404 */
    @Test
    void save_editBuiltIn_throws40404() {
        CustomFoodSaveDTO dto = validDto("米饭", "10", "10", "10", 170);
        dto.setId(5L);
        when(foodMapper.selectById(5L)).thenReturn(builtInFood(5L));
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_NOT_FOUND_CODE, e.getCode());
    }

    /** 场景：删除内置食物 → 40404 */
    @Test
    void remove_builtIn_throws40404() {
        when(foodMapper.selectById(5L)).thenReturn(builtInFood(5L));
        BusinessException e = assertThrows(BusinessException.class, () -> service.remove(1L, 5L));
        assertEquals(CommonConstant.FOOD_NOT_FOUND_CODE, e.getCode());
        verify(foodMapper, never()).deleteById(any(Long.class));
    }

    /** 场景：删除他人自定义 → 40405 */
    @Test
    void remove_othersCustom_throws40405() {
        when(foodMapper.selectById(99L)).thenReturn(customFood(99L, 2L, "别人的"));
        BusinessException e = assertThrows(BusinessException.class, () -> service.remove(1L, 99L));
        assertEquals(CommonConstant.FOOD_NOT_OWNER_CODE, e.getCode());
    }

    /** 场景：删除本人自定义 → 软删成功 */
    @Test
    void remove_ownCustom_softDelete() {
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "我的"));
        service.remove(1L, 11L);
        verify(foodMapper).deleteById(11L);
    }

    /** 口径锚点：我的自定义列表为有界查询（LIMIT 200），防用户无限积累全表拉取 */
    @Test
    @SuppressWarnings("unchecked")
    void listMine_boundedByLimit() {
        when(foodMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.listMine(1L);

        ArgumentCaptor<Wrapper<Food>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(foodMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("LIMIT " + CommonConstant.CUSTOM_FOOD_MINE_LIMIT),
                "应含列表上限，实际: " + sql);
    }

    private CustomFoodSaveDTO validDto(String name, String carb, String protein, String fat, int kcal) {
        CustomFoodSaveDTO dto = new CustomFoodSaveDTO();
        dto.setName(name);
        dto.setCategoryCode("01");
        dto.setCarb(new BigDecimal(carb));
        dto.setProtein(new BigDecimal(protein));
        dto.setFat(new BigDecimal(fat));
        dto.setKcal(kcal);
        return dto;
    }

    private Food customFood(Long id, Long userId, String name) {
        Food food = new Food();
        food.setId(id);
        food.setUserId(userId);
        food.setName(name);
        food.setSource(2);
        food.setCarb(new BigDecimal("10"));
        food.setProtein(new BigDecimal("10"));
        food.setFat(new BigDecimal("10"));
        food.setKcal(170);
        return food;
    }

    private Food builtInFood(Long id) {
        Food food = new Food();
        food.setId(id);
        food.setCode("F002");
        food.setSource(1);
        return food;
    }
}

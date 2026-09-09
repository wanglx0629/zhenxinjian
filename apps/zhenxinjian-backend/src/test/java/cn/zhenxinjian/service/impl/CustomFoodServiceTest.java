package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.mapper.FoodMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自定义食物服务单元测试（宏量区间 40402 / 能量守恒 40403 / 归属 40405 / 内置拒绝 40404 / 重名 40401）
 * 作者: wanglx
 */
class CustomFoodServiceTest {

    private FoodMapper foodMapper;
    private CustomFoodService service;

    @BeforeEach
    void setUp() {
        foodMapper = mock(FoodMapper.class);
        service = new CustomFoodService(foodMapper);
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

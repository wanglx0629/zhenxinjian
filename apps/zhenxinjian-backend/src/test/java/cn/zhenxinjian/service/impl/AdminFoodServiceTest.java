package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.domain.dto.AdminFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端食物库维护服务单元测试（同名 40903 / 守恒 40403 / 自定义拒写 40404 / code 顺延）
 * 作者: wanglx
 */
@ExtendWith(MockitoExtension.class)
class AdminFoodServiceTest {

    @Mock
    private FoodMapper foodMapper;

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private AdminFoodService adminFoodService;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);
        // ServiceImpl 的 baseMapper 为继承字段，构造器注入不会填充，显式装配
        ReflectionTestUtils.setField(adminFoodService, "baseMapper", foodMapper);
    }

    private AdminFoodSaveDTO validDto() {
        AdminFoodSaveDTO dto = new AdminFoodSaveDTO();
        dto.setName("鸡胸肉(测试)");
        dto.setCategoryCode("04");
        dto.setCategoryName("肉蛋水产");
        dto.setCarb(new BigDecimal("2.5"));
        dto.setProtein(new BigDecimal("24.6"));
        dto.setFat(new BigDecimal("1.9"));
        dto.setKcal(133);
        return dto;
    }

    /** 场景：同名内置食物已存在 → 40903 */
    @Test
    void create_duplicateName_throw40903() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.create(validDto()));
        assertEquals(CommonConstant.ADMIN_FOOD_CONFLICT_CODE, ex.getCode());
    }

    /** 场景：能量与 4/4/9 换算偏差超 ±10% → 40403 */
    @Test
    void create_kcalMismatch_throw40403() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        AdminFoodSaveDTO dto = validDto();
        dto.setKcal(999);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.create(dto));
        assertEquals(CommonConstant.FOOD_KCAL_MISMATCH_CODE, ex.getCode());
    }

    /** 场景：编辑自定义食物 → 40404 拒写 */
    @Test
    void update_customFood_throw40404() {
        Food custom = new Food();
        custom.setId(1L);
        custom.setSource(2);
        when(foodMapper.selectById(1L)).thenReturn(custom);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.update(1L, validDto()));
        assertEquals(CommonConstant.FOOD_NOT_FOUND_CODE, ex.getCode());
    }

    /** 场景：首个新增内置食物（库中无 F 码）→ code 从 F201 起 */
    @Test
    void create_firstCustomCode_startF201() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        adminFoodService.create(validDto());
        verify(foodMapper).insert(argThat((Food f) -> "F201".equals(f.getCode()) && f.getSource() == 1));
    }
}

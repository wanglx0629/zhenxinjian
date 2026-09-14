package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.FoodCategoryEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.query.FoodSearchQuery;
import cn.zhenxinjian.domain.vo.FoodCalcVO;
import cn.zhenxinjian.domain.vo.FoodCategoryVO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 食物库查询服务单元测试
 * 作者: wanglx
 */
class FoodServiceTest {

    private FoodMapper foodMapper;
    private FoodService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);

        foodMapper = mock(FoodMapper.class);
        service = new FoodService(foodMapper);
    }

    /** 场景：空关键字且未选分类 → 返回空分页 */
    @Test
    void search_emptyKeywordAndCategory_returnsEmptyPage() {
        FoodSearchQuery query = new FoodSearchQuery();
        query.setKeyword("");
        query.setCategoryCode("");

        var result = service.search(1L, query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    /** 场景：关键字匹配 → 返回搜索结果 */
    @Test
    void search_withKeyword_returnsResults() {
        FoodSearchQuery query = new FoodSearchQuery();
        query.setKeyword("鸡胸肉");
        Page<Food> page = new Page<>(1, 10);
        page.setRecords(List.of(food(1L, "鸡胸肉", "01", 133, 2.5, 19.4, 5.0)));
        page.setTotal(1);

        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        var result = service.search(1L, query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("鸡胸肉", result.getRecords().get(0).getName());
    }

    /** 场景：分类列表 → 返回所有 FoodCategoryEnum 枚举值 */
    @Test
    void categories_returnsAllCategories() {
        List<FoodCategoryVO> result = service.categories();

        assertNotNull(result);
        assertEquals(FoodCategoryEnum.values().length, result.size());
        assertEquals("01", result.get(0).getCode());
    }

    /** 场景：详情查询内置食物 → 返回 VO */
    @Test
    void detail_builtinFood_returnsVO() {
        when(foodMapper.selectById(1L)).thenReturn(food(1L, "鸡蛋", "01", 144, 0.0, 13.3, 8.8));

        FoodVO result = service.detail(1L, 1L);

        assertNotNull(result);
        assertEquals("鸡蛋", result.getName());
    }

    /** 场景：自定义食物非本人可见 → 抛错 */
    @Test
    void detail_customFoodNotOwn_throwsException() {
        Food custom = food(10L, "我的菜", "01", 100, 10.0, 5.0, 3.0);
        custom.setSource(FoodSourceEnum.CUSTOM.getCode());
        custom.setUserId(99L);

        when(foodMapper.selectById(10L)).thenReturn(custom);

        assertThrows(BusinessException.class, () -> service.detail(1L, 10L));
    }

    /** 场景：自定义食物本人可见 → 正常返回 */
    @Test
    void detail_customFoodOwn_returnsVO() {
        Food custom = food(10L, "我的菜", "01", 100, 10.0, 5.0, 3.0);
        custom.setSource(FoodSourceEnum.CUSTOM.getCode());
        custom.setUserId(1L);

        when(foodMapper.selectById(10L)).thenReturn(custom);

        FoodVO result = service.detail(1L, 10L);

        assertNotNull(result);
        assertEquals("我的菜", result.getName());
    }

    /** 场景：食物不存在 → 抛错 */
    @Test
    void detail_foodNotFound_throwsException() {
        when(foodMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.detail(1L, 999L));
    }

    /** 场景：试算 100g → 输出与原始值一致 */
    @Test
    void calc_100g_returnsSameValues() {
        when(foodMapper.selectById(1L)).thenReturn(food(1L, "鸡蛋", "01", 144, 0.0, 13.3, 8.8));

        FoodCalcVO result = service.calc(1L, 1L, 100);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("0.00").compareTo(result.getCarb()));
        assertEquals(0, new BigDecimal("13.30").compareTo(result.getProtein()));
        assertEquals(0, new BigDecimal("8.80").compareTo(result.getFat()));
        assertEquals(144, result.getKcal());
    }

    /** 场景：试算 200g → 输出翻倍 */
    @Test
    void calc_200g_doublesValues() {
        when(foodMapper.selectById(1L)).thenReturn(food(1L, "鸡蛋", "01", 144, 0.0, 13.3, 8.8));

        FoodCalcVO result = service.calc(1L, 1L, 200);

        assertEquals(0, new BigDecimal("0.00").compareTo(result.getCarb()));
        assertEquals(0, new BigDecimal("26.60").compareTo(result.getProtein()));
        assertEquals(0, new BigDecimal("17.60").compareTo(result.getFat()));
        assertEquals(288, result.getKcal());
    }

    /** 场景：试算克数为 null → 抛错 */
    @Test
    void calc_nullGrams_throwsException() {
        assertThrows(BusinessException.class, () -> service.calc(1L, 1L, null));
    }

    /** 场景：试算克数 < 1 → 抛错 */
    @Test
    void calc_gramsLessThan1_throwsException() {
        assertThrows(BusinessException.class, () -> service.calc(1L, 1L, 0));
    }

    /** 场景：试算克数 > 10000 → 抛错 */
    @Test
    void calc_gramsExceedsMax_throwsException() {
        assertThrows(BusinessException.class, () -> service.calc(1L, 1L, 10001));
    }

    private Food food(Long id, String name, String categoryCode, int kcal, double carb, double protein, double fat) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setCategoryCode(categoryCode);
        f.setSource(FoodSourceEnum.BUILT_IN.getCode());
        f.setKcal(kcal);
        f.setCarb(new BigDecimal(String.valueOf(carb)));
        f.setProtein(new BigDecimal(String.valueOf(protein)));
        f.setFat(new BigDecimal(String.valueOf(fat)));
        return f;
    }
}
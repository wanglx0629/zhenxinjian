package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.FoodAuditStatusEnum;
import cn.zhenxinjian.common.enums.FoodCategoryEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.FoodImage;
import cn.zhenxinjian.domain.query.FoodSearchQuery;
import cn.zhenxinjian.domain.vo.FoodCalcVO;
import cn.zhenxinjian.domain.vo.FoodCategoryVO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodImageMapper;
import cn.zhenxinjian.mapper.FoodMapper;
import cn.zhenxinjian.service.StorageService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 食物库查询服务单元测试
 * 可见性收敛（内置 status=1 / 共建已通过 audit_status=1+status=1 / 本人全部 0/1/2；停用不可见）
 * + 搜索/热门 SQL 锚点 + foods.image 优先 food_images 兜底
 * 作者: wanglx
 */
class FoodServiceTest {

    private FoodMapper foodMapper;
    private FoodImageMapper foodImageMapper;
    private StorageService storageService;
    private FoodService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);
        TableInfoHelper.initTableInfo(assistant, FoodImage.class);

        foodMapper = mock(FoodMapper.class);
        foodImageMapper = mock(FoodImageMapper.class);
        storageService = mock(StorageService.class);
        // 默认恒等解析（绝对 URL/空值原样），仅相对 objectKey 解析场景按需重打桩
        when(storageService.publicUrl(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new FoodService(foodMapper, foodImageMapper, storageService);
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

    /** 口径锚点：搜索可见性 = (内置,status=1) ∪ (共建,已通过,status=1) ∪ (本人共建,status=1) */
    @Test
    @SuppressWarnings("unchecked")
    void search_visibilityWrapper_filtersStatusAndAudit() {
        FoodSearchQuery query = new FoodSearchQuery();
        query.setKeyword("鸡");
        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<>(1, 10));

        service.search(1L, query);

        ArgumentCaptor<Wrapper<Food>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(foodMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("audit_status"), "应含审核状态过滤，实际: " + sql);
        assertTrue(sql.contains("status"), "应含有效状态过滤，实际: " + sql);
        assertTrue(sql.contains("user_id"), "应含本人可达分支，实际: " + sql);
    }

    /** 口径锚点：热门仅返回有效内置食物（status=1） */
    @Test
    @SuppressWarnings("unchecked")
    void hot_filtersDisabled() {
        when(foodMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.hot();

        ArgumentCaptor<Wrapper<Food>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(foodMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("status"), "热门应过滤停用食物，实际: " + sql);
    }

    /** 场景：foods.image 优先、food_images 兜底（无 foods.image 的内置食物回退预热图） */
    @Test
    void search_foodImagePriorityOverFoodImages() {
        FoodSearchQuery query = new FoodSearchQuery();
        query.setKeyword("测");
        Food builtIn = food(1L, "内置鸡", "01", 100, 10.0, 5.0, 3.0);
        builtIn.setCode("F001");
        Food custom = customFood(2L, 1L, "共建鸡", FoodAuditStatusEnum.APPROVED.getCode());
        Page<Food> page = new Page<>(1, 10);
        page.setRecords(List.of(builtIn, custom));
        page.setTotal(2);
        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        FoodImage img = new FoodImage();
        img.setFoodCode("F001");
        img.setUrl("https://img.example.com/F001.jpg");
        when(foodImageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(img));

        var result = service.search(1L, query);

        assertEquals("https://img.example.com/F001.jpg", result.getRecords().get(0).getImage());
        assertEquals("/f/2026/upload.jpg", result.getRecords().get(1).getImage());
    }

    /** 场景：共建食物 image 存相对 objectKey → VO 解析为公网地址（存相对、展示绝对） */
    @Test
    void search_relativeImage_resolvedToPublicUrl() {
        FoodSearchQuery query = new FoodSearchQuery();
        query.setKeyword("测");
        Food custom = customFood(2L, 1L, "共建鸡", FoodAuditStatusEnum.APPROVED.getCode());
        Page<Food> page = new Page<>(1, 10);
        page.setRecords(List.of(custom));
        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(storageService.publicUrl("/f/2026/upload.jpg"))
                .thenReturn("http://localhost:9000/zhenxinjian/f/2026/upload.jpg");

        var result = service.search(1L, query);

        assertEquals("http://localhost:9000/zhenxinjian/f/2026/upload.jpg",
                result.getRecords().get(0).getImage());
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

    /** 场景：停用的内置食物 → 40404 不可见 */
    @Test
    void detail_disabledBuiltIn_throwsException() {
        Food disabled = food(1L, "鸡蛋", "01", 144, 0.0, 13.3, 8.8);
        disabled.setStatus(0);
        when(foodMapper.selectById(1L)).thenReturn(disabled);

        assertThrows(BusinessException.class, () -> service.detail(1L, 1L));
    }

    /** 场景：他人已通过(1)的共建食物 → 全员可见 */
    @Test
    void detail_othersApprovedCustom_returnsVO() {
        when(foodMapper.selectById(10L)).thenReturn(customFood(10L, 99L, "共建菜", FoodAuditStatusEnum.APPROVED.getCode()));

        FoodVO result = service.detail(1L, 10L);

        assertNotNull(result);
        assertEquals("共建菜", result.getName());
    }

    /** 场景：他人待审核(0)的共建食物 → 40404 不可见 */
    @Test
    void detail_othersPendingCustom_throwsException() {
        when(foodMapper.selectById(10L)).thenReturn(customFood(10L, 99L, "待审菜", FoodAuditStatusEnum.PENDING.getCode()));

        assertThrows(BusinessException.class, () -> service.detail(1L, 10L));
    }

    /** 场景：他人已驳回(2)的共建食物 → 40404 不可见 */
    @Test
    void detail_othersRejectedCustom_throwsException() {
        when(foodMapper.selectById(10L)).thenReturn(customFood(10L, 99L, "驳回菜", FoodAuditStatusEnum.REJECTED.getCode()));

        assertThrows(BusinessException.class, () -> service.detail(1L, 10L));
    }

    /** 场景：停用的已通过共建食物（管理员停用）→ 40404 不可见 */
    @Test
    void detail_disabledApprovedCustom_throwsException() {
        Food disabled = customFood(10L, 99L, "共建菜", FoodAuditStatusEnum.APPROVED.getCode());
        disabled.setStatus(0);
        when(foodMapper.selectById(10L)).thenReturn(disabled);

        assertThrows(BusinessException.class, () -> service.detail(1L, 10L));
    }

    /** 场景：本人已驳回(2)的共建食物 → 可见可记 */
    @Test
    void detail_ownRejectedCustom_returnsVO() {
        when(foodMapper.selectById(10L)).thenReturn(customFood(10L, 1L, "我的驳回菜", FoodAuditStatusEnum.REJECTED.getCode()));

        FoodVO result = service.detail(1L, 10L);

        assertNotNull(result);
        assertEquals("我的驳回菜", result.getName());
    }

    /** 场景：自定义食物非本人可见 → 抛错 */
    @Test
    void detail_customFoodNotOwn_throwsException() {
        when(foodMapper.selectById(10L)).thenReturn(customFood(10L, 99L, "我的菜", FoodAuditStatusEnum.PENDING.getCode()));

        assertThrows(BusinessException.class, () -> service.detail(1L, 10L));
    }

    /** 场景：自定义食物本人可见 → 正常返回 */
    @Test
    void detail_customFoodOwn_returnsVO() {
        when(foodMapper.selectById(10L)).thenReturn(customFood(10L, 1L, "我的菜", FoodAuditStatusEnum.PENDING.getCode()));

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
        f.setAuditStatus(FoodAuditStatusEnum.NOT_REQUIRED.getCode());
        f.setStatus(1);
        f.setKcal(kcal);
        f.setCarb(new BigDecimal(String.valueOf(carb)));
        f.setProtein(new BigDecimal(String.valueOf(protein)));
        f.setFat(new BigDecimal(String.valueOf(fat)));
        return f;
    }

    private Food customFood(Long id, Long userId, String name, Integer auditStatus) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setCategoryCode("01");
        f.setSource(FoodSourceEnum.CUSTOM.getCode());
        f.setUserId(userId);
        f.setAuditStatus(auditStatus);
        f.setStatus(1);
        f.setKcal(100);
        f.setCarb(new BigDecimal("10"));
        f.setProtein(new BigDecimal("5"));
        f.setFat(new BigDecimal("3"));
        f.setImage("/f/2026/upload.jpg");
        return f;
    }
}

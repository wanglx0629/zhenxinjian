package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.FoodAiVerdictEnum;
import cn.zhenxinjian.common.enums.FoodAuditActionEnum;
import cn.zhenxinjian.common.enums.FoodAuditStatusEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodMapper;
import cn.zhenxinjian.service.StorageService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自定义食物服务单元测试
 * 状态机（新增置 0/驳回重提回 0/已通过只读 40407）+ 宏量区间 40402 / 能量守恒 40403 / 归属 40405
 * / 内置拒绝 40404 / 重名 40401 / kj-kcal 换算 / AI 校验降级不阻塞 / SUBMIT/RESUBMIT+AI_CHECK 流水
 * 作者: wanglx
 */
class CustomFoodServiceTest {

    private FoodMapper foodMapper;
    private SensitiveWordFilter sensitiveWordFilter;
    private FoodAuditAiService foodAuditAiService;
    private FoodAuditLogService foodAuditLogService;
    private StorageService storageService;
    private CustomFoodService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);

        foodMapper = mock(FoodMapper.class);
        sensitiveWordFilter = mock(SensitiveWordFilter.class);
        foodAuditAiService = mock(FoodAuditAiService.class);
        foodAuditLogService = mock(FoodAuditLogService.class);
        storageService = mock(StorageService.class);
        // 默认恒等解析（绝对 URL/空值原样），仅相对 objectKey 解析场景按需重打桩
        when(storageService.publicUrl(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new CustomFoodService(foodMapper, sensitiveWordFilter, foodAuditAiService,
                foodAuditLogService, storageService);

        // 默认：AI 校验降级 none（未配置/异常场景）；流水快照桩固定 JSON 便于断言
        when(foodAuditAiService.audit(any(Food.class))).thenReturn(FoodAuditAiService.AiCheckResult.none());
        when(foodAuditLogService.snapshot(any(Food.class))).thenReturn("{}");
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

    /** 场景：kcal 与 kj 均为空 → 40402（二选一必填） */
    @Test
    void save_bothEnergyMissing_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setKcal(null);
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 场景：仅填千焦 502kJ → 后端按 ÷4.184 换算 kcal=120 入库并过守恒 */
    @Test
    void save_kjOnly_derivesKcal() {
        CustomFoodSaveDTO dto = validDto("燕麦能量棒", "13", "8", "4", 170);
        dto.setKcal(null);
        dto.setKj(502);
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            ((Food) inv.getArgument(0)).setId(11L);
            return 1;
        });
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "燕麦能量棒"));

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        Food inserted = captor.getValue();
        assertEquals(120, inserted.getKcal());
        assertEquals(502, inserted.getKj());
    }

    /** 场景：分类编号 18（加工调理食品及其他类）→ 合法入库 */
    @Test
    void save_category18_accepted() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setCategoryCode("18");
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            ((Food) inv.getArgument(0)).setId(11L);
            return 1;
        });
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "燕麦碗"));

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        assertEquals("18", captor.getValue().getCategoryCode());
        assertEquals("加工调理食品及其他类", captor.getValue().getCategoryName());
    }

    /** 场景：分类编号非法（19 越界）→ 40402 拒绝，不入库 */
    @Test
    void save_invalidCategoryCode_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setCategoryCode("19");
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 场景：分类编号缺省 → 默认 18 加工调理食品及其他类（共建典型为自制复合食物） */
    @Test
    void save_categoryMissing_defaultsProcessed18() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setCategoryCode(null);
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            ((Food) inv.getArgument(0)).setId(11L);
            return 1;
        });
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "燕麦碗"));

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        assertEquals("18", captor.getValue().getCategoryCode());
        assertEquals("加工调理食品及其他类", captor.getValue().getCategoryName());
    }

    /** 场景：kj 超 3800 → 40402 */
    @Test
    void save_kjOverMax_throws40402() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setKj(3801);
        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));
        assertEquals(CommonConstant.FOOD_MACRO_INVALID_CODE, e.getCode());
    }

    /** 场景：新增 → source=2 / audit_status=0 / submitTime 落库 + SUBMIT/AI_CHECK 双流水 */
    @Test
    void save_create_marksPendingWithLogs() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        when(foodAuditAiService.audit(any(Food.class)))
                .thenReturn(new FoodAuditAiService.AiCheckResult(FoodAiVerdictEnum.PASS, "合理"));
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            ((Food) inv.getArgument(0)).setId(11L);
            return 1;
        });
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "燕麦碗"));

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        Food inserted = captor.getValue();
        assertEquals(2, inserted.getSource());
        assertEquals(0, inserted.getAuditStatus());
        assertNotNull(inserted.getSubmitTime());
        assertEquals("pass", inserted.getAiVerdict());
        assertEquals("合理", inserted.getAiSuggestion());
        verify(foodAuditLogService).record(eq(11L), eq(FoodAuditActionEnum.SUBMIT), eq(1L),
                isNull(), isNull(), isNull(), isNull(), eq("{}"));
        verify(foodAuditLogService).record(eq(11L), eq(FoodAuditActionEnum.AI_CHECK), eq(0L),
                eq(FoodAiVerdictEnum.PASS), eq("合理"), isNull(), isNull(), eq("{}"));
    }

    /** 场景：AI 校验降级 none（未配置/超时）→ 不阻塞投稿，结论置未校验 */
    @Test
    void save_create_aiDowngradeNotBlocking() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            ((Food) inv.getArgument(0)).setId(11L);
            return 1;
        });
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "燕麦碗"));

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        assertEquals("none", captor.getValue().getAiVerdict());
        assertNull(captor.getValue().getAiSuggestion());
        verify(foodAuditLogService).record(eq(11L), eq(FoodAuditActionEnum.AI_CHECK), eq(0L),
                eq(FoodAiVerdictEnum.NONE), isNull(), isNull(), isNull(), eq("{}"));
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

    /** 场景：编辑已驳回(2)本人食物 → 状态回 0、清驳回原因、重跑 AI、RESUBMIT+AI_CHECK 流水 */
    @Test
    void save_editRejected_backToPendingResubmit() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setId(11L);
        Food rejected = customFood(11L, 1L, "燕麦碗", FoodAuditStatusEnum.REJECTED.getCode());
        rejected.setAuditRemark("数值不合理");
        rejected.setAiVerdict("suspect");
        when(foodMapper.selectById(11L)).thenReturn(rejected);
        when(foodAuditAiService.audit(any(Food.class)))
                .thenReturn(new FoodAuditAiService.AiCheckResult(FoodAiVerdictEnum.PASS, "合理"));

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        Food updated = captor.getValue();
        assertEquals(0, updated.getAuditStatus());
        assertNull(updated.getAuditRemark());
        assertEquals("pass", updated.getAiVerdict());
        assertEquals("合理", updated.getAiSuggestion());
        assertNotNull(updated.getSubmitTime());
        verify(foodAuditLogService).record(eq(11L), eq(FoodAuditActionEnum.RESUBMIT), eq(1L),
                isNull(), isNull(), isNull(), eq("{}"), eq("{}"));
        verify(foodAuditLogService).record(eq(11L), eq(FoodAuditActionEnum.AI_CHECK), eq(0L),
                eq(FoodAiVerdictEnum.PASS), eq("合理"), isNull(), isNull(), eq("{}"));
    }

    /** 场景：编辑待审核(0)本人食物 → 保持 0、刷新提交时间、RESUBMIT+AI_CHECK 流水 */
    @Test
    void save_editPending_staysPendingWithResubmit() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setId(11L);
        Food pending = customFood(11L, 1L, "燕麦碗", FoodAuditStatusEnum.PENDING.getCode());
        when(foodMapper.selectById(11L)).thenReturn(pending);

        service.save(1L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getAuditStatus());
        verify(foodAuditLogService).record(eq(11L), eq(FoodAuditActionEnum.RESUBMIT), eq(1L),
                isNull(), isNull(), isNull(), eq("{}"), eq("{}"));
    }

    /** 场景：编辑已通过(1)转公共食物 → 40407 只读拒绝，数据未变更 */
    @Test
    void save_editApproved_throws40407() {
        CustomFoodSaveDTO dto = validDto("燕麦碗", "10", "10", "10", 170);
        dto.setId(11L);
        when(foodMapper.selectById(11L))
                .thenReturn(customFood(11L, 1L, "燕麦碗", FoodAuditStatusEnum.APPROVED.getCode()));

        BusinessException e = assertThrows(BusinessException.class, () -> service.save(1L, dto));

        assertEquals(CommonConstant.FOOD_PUBLIC_READONLY_CODE, e.getCode());
        verify(foodMapper, never()).updateById(any(Food.class));
        verify(foodAuditLogService, never()).record(any(), any(), any(), any(), any(), any(), any(), any());
    }

    /** 场景：删除已通过(1)转公共食物 → 40407 */
    @Test
    void remove_approved_throws40407() {
        when(foodMapper.selectById(11L))
                .thenReturn(customFood(11L, 1L, "我的", FoodAuditStatusEnum.APPROVED.getCode()));

        BusinessException e = assertThrows(BusinessException.class, () -> service.remove(1L, 11L));

        assertEquals(CommonConstant.FOOD_PUBLIC_READONLY_CODE, e.getCode());
        verify(foodMapper, never()).deleteById(any(Long.class));
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

    /** 场景：删除本人待审核(0) → 软删成功 */
    @Test
    void remove_ownPending_softDelete() {
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "我的"));
        service.remove(1L, 11L);
        verify(foodMapper).deleteById(11L);
    }

    /** 场景：删除本人已驳回(2) → 软删成功 */
    @Test
    void remove_ownRejected_softDelete() {
        when(foodMapper.selectById(11L)).thenReturn(customFood(11L, 1L, "我的", FoodAuditStatusEnum.REJECTED.getCode()));
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

    /** 场景：我的列表 image 存相对 objectKey → VO 解析为公网地址（存相对、展示绝对） */
    @Test
    @SuppressWarnings("unchecked")
    void listMine_relativeImage_resolvedToPublicUrl() {
        Food mine = customFood(11L, 1L, "燕麦碗");
        mine.setImage("upload/2026/09/24/a.jpg");
        when(foodMapper.selectList(any(Wrapper.class))).thenReturn(List.of(mine));
        when(storageService.publicUrl("upload/2026/09/24/a.jpg"))
                .thenReturn("http://localhost:9000/zhenxinjian/upload/2026/09/24/a.jpg");

        List<FoodVO> result = service.listMine(1L);

        assertEquals("http://localhost:9000/zhenxinjian/upload/2026/09/24/a.jpg",
                result.get(0).getImage());
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
        return customFood(id, userId, name, FoodAuditStatusEnum.PENDING.getCode());
    }

    private Food customFood(Long id, Long userId, String name, Integer auditStatus) {
        Food food = new Food();
        food.setId(id);
        food.setUserId(userId);
        food.setName(name);
        food.setSource(2);
        food.setAuditStatus(auditStatus);
        food.setStatus(1);
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
        food.setAuditStatus(FoodAuditStatusEnum.NOT_REQUIRED.getCode());
        food.setStatus(1);
        return food;
    }
}

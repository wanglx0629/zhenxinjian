package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.FoodAuditActionEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.FoodAuditDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.mapper.FoodMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端共建食物审核服务单元测试
 * （状态机：0/2 可审、1 只可维护停用/软删；REJECT 必填原因 40916；ADMIN_FIX 合并校验 40917；目标非法 40915；流水留痕）
 * 作者: wanglx
 */
@ExtendWith(MockitoExtension.class)
class AdminFoodAuditServiceTest {

    @Mock
    private FoodMapper foodMapper;

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @Mock
    private FoodAuditLogService foodAuditLogService;

    @InjectMocks
    private AdminFoodAuditService service;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        LoginUserVO admin = new LoginUserVO();
        admin.setId(5L);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::get).thenReturn(admin);
        userContextMock.when(UserContext::getUserId).thenReturn(5L);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    /** 场景：待审核(0)共建食物 APPROVE → audit_status=1、清驳回原因、写 APPROVE 流水 */
    @Test
    void audit_approve_pending_setsApprovedAndLogs() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));

        service.audit(21L, dto("APPROVE", null, null));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getAuditStatus());
        assertNull(captor.getValue().getAuditRemark());
        assertEquals("admin", captor.getValue().getUpdateBy());
        verify(foodAuditLogService).record(eq(21L), eq(FoodAuditActionEnum.APPROVE), eq(5L),
                isNull(), isNull(), isNull(), nullable(String.class), nullable(String.class));
    }

    /** 场景：已驳回(2)的共建食物管理员可直接再审通过（重提不必等用户） */
    @Test
    void audit_approve_rejected_allowed() {
        when(foodMapper.selectById(22L)).thenReturn(customFood(22L, 2));

        service.audit(22L, dto("APPROVE", null, null));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getAuditStatus());
    }

    /** 场景：REJECT 缺原因/空白原因 → 40916，不动库不写流水 */
    @Test
    void audit_reject_withoutRemark_throws40916() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));

        BusinessException e1 = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("REJECT", null, null)));
        assertEquals(CommonConstant.ADMIN_FOOD_REJECT_REASON_REQUIRED_CODE, e1.getCode());
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("REJECT", "   ", null)));
        assertEquals(CommonConstant.ADMIN_FOOD_REJECT_REASON_REQUIRED_CODE, e2.getCode());
        verify(foodMapper, never()).updateById(any(Food.class));
        verify(foodAuditLogService, never()).record(anyLong(), any(), anyLong(),
                any(), any(), any(), any(), any());
    }

    /** 场景：REJECT 带原因 → audit_status=2、落 audit_remark、写 REJECT 流水 */
    @Test
    void audit_reject_withRemark_setsRejectedAndRemark() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));

        service.audit(21L, dto("REJECT", "营养数据不合理", null));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getAuditStatus());
        assertEquals("营养数据不合理", captor.getValue().getAuditRemark());
        verify(foodAuditLogService).record(eq(21L), eq(FoodAuditActionEnum.REJECT), eq(5L),
                isNull(), isNull(), eq("营养数据不合理"),
                nullable(String.class), nullable(String.class));
    }

    /** 场景：ADMIN_FIX 空字段沿用原值、非空字段覆盖 → audit_status=1、写 ADMIN_FIX 流水 */
    @Test
    void audit_adminFix_partialMerge_approves() {
        Food food = customFood(21L, 0);
        when(foodMapper.selectById(21L)).thenReturn(food);
        FoodAuditDTO.Fix fix = new FoodAuditDTO.Fix();
        fix.setName("燕麦粥");
        fix.setKcal(342);
        fix.setUnit("碗");

        service.audit(21L, dto("ADMIN_FIX", "修正能量", fix));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        Food saved = captor.getValue();
        assertEquals(1, saved.getAuditStatus());
        assertEquals("燕麦粥", saved.getName());
        assertEquals(342, saved.getKcal());
        assertEquals("碗", saved.getUnit());
        // 未提供的字段沿用原值
        assertEquals(new BigDecimal("60"), saved.getCarb());
        assertEquals(new BigDecimal("12"), saved.getProtein());
        verify(foodAuditLogService).record(eq(21L), eq(FoodAuditActionEnum.ADMIN_FIX), eq(5L),
                isNull(), isNull(), eq("修正能量"),
                nullable(String.class), nullable(String.class));
    }

    /** 场景：ADMIN_FIX 未携带修正值 → 40917 */
    @Test
    void audit_adminFix_missingFix_throws40917() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("ADMIN_FIX", null, null)));
        assertEquals(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE, e.getCode());
        verify(foodMapper, never()).updateById(any(Food.class));
    }

    /** 场景：ADMIN_FIX 修正宏量越界（脂肪 120g/100g）→ 40917 */
    @Test
    void audit_adminFix_macroOutOfRange_throws40917() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));
        FoodAuditDTO.Fix fix = new FoodAuditDTO.Fix();
        fix.setFat(new BigDecimal("120"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("ADMIN_FIX", null, fix)));
        assertEquals(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE, e.getCode());
    }

    /** 场景：ADMIN_FIX 修正能量与 4/4/9 偏差超 ±10% → 40917 */
    @Test
    void audit_adminFix_kcalMismatch_throws40917() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));
        FoodAuditDTO.Fix fix = new FoodAuditDTO.Fix();
        fix.setKcal(100);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("ADMIN_FIX", null, fix)));
        assertEquals(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE, e.getCode());
    }

    /** 场景：ADMIN_FIX 修正分类为 18（加工调理食品及其他类）→ 编号/名称一并落库 */
    @Test
    void audit_adminFix_category18_applied() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));
        FoodAuditDTO.Fix fix = new FoodAuditDTO.Fix();
        fix.setCategoryCode("18");

        service.audit(21L, dto("ADMIN_FIX", "归类修正", fix));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals("18", captor.getValue().getCategoryCode());
        assertEquals("加工调理食品及其他类", captor.getValue().getCategoryName());
    }

    /** 场景：ADMIN_FIX 修正分类编号非法（19 越界）→ 40917 */
    @Test
    void audit_adminFix_invalidCategoryCode_throws40917() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));
        FoodAuditDTO.Fix fix = new FoodAuditDTO.Fix();
        fix.setCategoryCode("19");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("ADMIN_FIX", null, fix)));
        assertEquals(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE, e.getCode());
        verify(foodMapper, never()).updateById(any(Food.class));
    }

    /** 场景：审核基础食物（source=1）→ 40915 目标非法 */
    @Test
    void audit_builtinTarget_throws40915() {
        Food builtin = customFood(31L, 3);
        builtin.setSource(1);
        when(foodMapper.selectById(31L)).thenReturn(builtin);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(31L, dto("APPROVE", null, null)));
        assertEquals(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE, e.getCode());
    }

    /** 场景：食物不存在 → 40404 */
    @Test
    void audit_notFound_throws40404() {
        when(foodMapper.selectById(404L)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(404L, dto("APPROVE", null, null)));
        assertEquals(CommonConstant.FOOD_NOT_FOUND_CODE, e.getCode());
    }

    /** 场景：已通过(1)的共建食物不再是审核目标（维护走停用/软删入口）→ 40915 */
    @Test
    void audit_alreadyApproved_throws40915() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 1));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.audit(21L, dto("APPROVE", null, null)));
        assertEquals(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE, e.getCode());
    }

    /** 场景：停用已通过共建食物（明确维护入口）→ status 落库 */
    @Test
    void changeCustomStatus_approved_updatesStatus() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 1));

        service.changeCustomStatus(21L, 0);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals("admin", captor.getValue().getUpdateBy());
    }

    /** 场景：维护入口目标非共建食物（source=1）→ 40915 */
    @Test
    void changeCustomStatus_builtin_throws40915() {
        Food builtin = customFood(31L, 1);
        builtin.setSource(1);
        when(foodMapper.selectById(31L)).thenReturn(builtin);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeCustomStatus(31L, 0));
        assertEquals(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE, e.getCode());
    }

    /** 场景：未通过（0）的共建食物不走维护入口（驳回即可）→ 40915 */
    @Test
    void changeCustomStatus_notApproved_throws40915() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeCustomStatus(21L, 0));
        assertEquals(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE, e.getCode());
    }

    /** 场景：软删已通过共建食物 → 逻辑删除（饮食记录引用不断链） */
    @Test
    void removeCustom_approved_softDeletes() {
        when(foodMapper.selectById(21L)).thenReturn(customFood(21L, 1));

        service.removeCustom(21L);

        verify(foodMapper).deleteById(21L);
    }

    /** 场景：软删入口目标非共建食物 → 40915 */
    @Test
    void removeCustom_builtin_throws40915() {
        Food builtin = customFood(31L, 1);
        builtin.setSource(1);
        when(foodMapper.selectById(31L)).thenReturn(builtin);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.removeCustom(31L));
        assertEquals(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE, e.getCode());
        verify(foodMapper, never()).deleteById(anyLong());
    }

    // ==================== 构造辅助 ====================

    /** 共建食物：source=2、60/12/6/350（期望能量 342，偏差 2.3% 守恒通过） */
    private Food customFood(Long id, int auditStatus) {
        Food food = new Food();
        food.setId(id);
        food.setUserId(9L);
        food.setSource(2);
        food.setStatus(1);
        food.setAuditStatus(auditStatus);
        food.setName("燕麦碗");
        food.setCategoryCode("18");
        food.setCategoryName("加工调理食品及其他类");
        food.setCarb(new BigDecimal("60"));
        food.setProtein(new BigDecimal("12"));
        food.setFat(new BigDecimal("6"));
        food.setKcal(350);
        return food;
    }

    private FoodAuditDTO dto(String action, String remark, FoodAuditDTO.Fix fix) {
        FoodAuditDTO dto = new FoodAuditDTO();
        dto.setAction(action);
        dto.setRemark(remark);
        dto.setFix(fix);
        return dto;
    }
}

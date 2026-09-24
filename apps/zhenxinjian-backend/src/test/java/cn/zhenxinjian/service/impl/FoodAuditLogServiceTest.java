package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.FoodAiVerdictEnum;
import cn.zhenxinjian.common.enums.FoodAuditActionEnum;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.FoodAuditLog;
import cn.zhenxinjian.mapper.FoodAuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 审核流水服务单元测试（只追加写：动作/操作人/AI 快照/原因/前后 JSON；AI_CHECK 系统操作人 0）
 * 作者: wanglx
 */
class FoodAuditLogServiceTest {

    private FoodAuditLogMapper foodAuditLogMapper;
    private FoodAuditLogService service;

    @BeforeEach
    void setUp() {
        foodAuditLogMapper = mock(FoodAuditLogMapper.class);
        service = new FoodAuditLogService(foodAuditLogMapper);
    }

    /** 场景：SUBMIT 流水 → 动作/操作人/审计标识/后快照落库 */
    @Test
    void record_submit_writesActionAndOperator() {
        service.record(11L, FoodAuditActionEnum.SUBMIT, 1L, null, null, null, null, "{\"name\":\"燕麦碗\"}");

        FoodAuditLog saved = captureLog();
        assertEquals(11L, saved.getFoodId());
        assertEquals("SUBMIT", saved.getAction());
        assertEquals(1L, saved.getOperatorId());
        assertEquals("user:1", saved.getCreateBy());
        assertEquals(1, saved.getStatus());
        assertNull(saved.getAiVerdict());
        assertNull(saved.getAiSuggestion());
        assertNull(saved.getRemark());
        assertNull(saved.getSnapshotBefore());
        assertEquals("{\"name\":\"燕麦碗\"}", saved.getSnapshotAfter());
    }

    /** 场景：AI_CHECK 流水 → 系统操作人 0 + AI 结论与建议快照 */
    @Test
    void record_aiCheck_systemOperatorWithVerdictSnapshot() {
        service.record(11L, FoodAuditActionEnum.AI_CHECK, 0L,
                FoodAiVerdictEnum.SUSPECT, "脂肪偏高，建议复核", null, null, "{\"name\":\"鸡胸肉\"}");

        FoodAuditLog saved = captureLog();
        assertEquals("AI_CHECK", saved.getAction());
        assertEquals(0L, saved.getOperatorId());
        assertEquals("system", saved.getCreateBy());
        assertEquals("suspect", saved.getAiVerdict());
        assertEquals("脂肪偏高，建议复核", saved.getAiSuggestion());
        assertEquals("{\"name\":\"鸡胸肉\"}", saved.getSnapshotAfter());
    }

    /** 场景：REJECT 流水 → 驳回原因必达 */
    @Test
    void record_reject_remarkPersisted() {
        service.record(11L, FoodAuditActionEnum.REJECT, 9L, null, null, "名称与营养明显不符", "{\"a\":1}", "{\"b\":2}");

        FoodAuditLog saved = captureLog();
        assertEquals("REJECT", saved.getAction());
        assertEquals("user:9", saved.getCreateBy());
        assertEquals("名称与营养明显不符", saved.getRemark());
        assertEquals("{\"a\":1}", saved.getSnapshotBefore());
        assertEquals("{\"b\":2}", saved.getSnapshotAfter());
    }

    /** 场景：快照包含名称/分类/三宏/能量/单位/图片关键字段 */
    @Test
    void snapshot_containsKeyFields() {
        Food food = new Food();
        food.setName("燕麦碗");
        food.setCategoryCode("01");
        food.setCarb(new BigDecimal("10"));
        food.setProtein(new BigDecimal("10"));
        food.setFat(new BigDecimal("10"));
        food.setKcal(170);
        food.setKj(711);
        food.setUnit("杯");
        food.setImage("/f/2026/a.jpg");

        String json = service.snapshot(food);

        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"name\":\"燕麦碗\""), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"categoryCode\":\"01\""), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"carb\":10"), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"kcal\":170"), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"kj\":711"), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"unit\":\"杯\""), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"image\":\"/f/2026/a.jpg\""), json);
    }

    /** 场景：空食物快照返回 null（新增动作无前值） */
    @Test
    void snapshot_nullFood_returnsNull() {
        assertNull(service.snapshot(null));
    }

    private FoodAuditLog captureLog() {
        ArgumentCaptor<FoodAuditLog> captor = ArgumentCaptor.forClass(FoodAuditLog.class);
        verify(foodAuditLogMapper).insert(captor.capture());
        return captor.getValue();
    }
}

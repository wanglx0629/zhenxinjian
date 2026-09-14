package cn.zhenxinjian.it.service;

import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.it.TestConfig;
import cn.zhenxinjian.mapper.DietRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 饮食记录 Service 集成测试 — 验证 DB CRUD
 * 作者: wanglx
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@org.springframework.context.annotation.Import(TestConfig.class)
class DietRecordServiceITest {

    @Autowired
    private DietRecordMapper dietRecordMapper;

    /** 场景：insert → selectById → 返回实体（含 BigDecimal 列） */
    @Test
    void insertAndSelectById_returnsRecord() {
        DietRecord record = new DietRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType(1);
        record.setFoodName("米饭");
        record.setCarb100g(new BigDecimal("25.9"));
        record.setProtein100g(new BigDecimal("2.6"));
        record.setFat100g(new BigDecimal("0.3"));
        record.setKcal100g(116);
        record.setAmountG(200.0);
        record.setCarbG(51.8);
        record.setProteinG(5.2);
        record.setFatG(0.6);
        record.setKcal(232);

        int rows = dietRecordMapper.insert(record);
        assertEquals(1, rows);
        assertNotNull(record.getId());

        DietRecord found = dietRecordMapper.selectById(record.getId());
        assertEquals("米饭", found.getFoodName());
        assertTrue(new BigDecimal("25.9").compareTo(found.getCarb100g()) == 0);
        assertEquals(232, found.getKcal());
    }

    /** 场景：逻辑删除 */
    @Test
    void delete_softDeletes() {
        DietRecord record = new DietRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType(2);
        dietRecordMapper.insert(record);

        dietRecordMapper.deleteById(record.getId());
        assertNull(dietRecordMapper.selectById(record.getId()));
    }

    /** 场景：update 更新备注 */
    @Test
    void update_updatesRemark() {
        DietRecord record = new DietRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType(1);
        record.setRemark("原备注");
        dietRecordMapper.insert(record);

        record.setRemark("新备注");
        dietRecordMapper.updateById(record);

        assertEquals("新备注", dietRecordMapper.selectById(record.getId()).getRemark());
    }
}
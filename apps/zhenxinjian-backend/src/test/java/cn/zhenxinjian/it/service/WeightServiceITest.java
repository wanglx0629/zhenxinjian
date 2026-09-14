package cn.zhenxinjian.it.service;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.WeightRecordVO;
import cn.zhenxinjian.it.TestConfig;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import cn.zhenxinjian.service.impl.WeightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 体重记录 Service 集成测试 — 验证 DB + Service 全链路
 * 作者: wanglx
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@org.springframework.context.annotation.Import(TestConfig.class)
class WeightServiceITest {

    @Autowired
    private WeightService weightService;

    @Autowired
    private WeightRecordMapper weightRecordMapper;

    /** 场景：insert → selectById → 返回实体 */
    @Test
    void save_insertsNewRecord() {
        WeightRecord record = new WeightRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setWeight(70.5);
        weightRecordMapper.insert(record);

        assertNotNull(record.getId());
        WeightRecord found = weightRecordMapper.selectById(record.getId());
        assertEquals(70.5, found.getWeight(), 0.01);
    }

    /** 场景：逻辑删除 → selectById 返回 null */
    @Test
    void remove_softDeletes() {
        WeightRecord record = new WeightRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setWeight(68.0);
        weightRecordMapper.insert(record);

        weightRecordMapper.deleteById(record.getId());

        WeightRecord found = weightRecordMapper.selectById(record.getId());
        assertEquals(null, found);
    }

    /** 场景：按日期范围查询 */
    @Test
    void list_returnsInDateRange() {
        WeightRecord r1 = new WeightRecord();
        r1.setUserId(1L);
        r1.setRecordDate(LocalDate.now().minusDays(1));
        r1.setWeight(70.0);
        weightRecordMapper.insert(r1);

        WeightRecord r2 = new WeightRecord();
        r2.setUserId(1L);
        r2.setRecordDate(LocalDate.now());
        r2.setWeight(71.0);
        weightRecordMapper.insert(r2);

        List<WeightRecord> list = weightRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, 1L)
                        .ge(WeightRecord::getRecordDate, LocalDate.now().minusDays(2))
                        .orderByDesc(WeightRecord::getRecordDate));
        assertEquals(2, list.size());
    }
}
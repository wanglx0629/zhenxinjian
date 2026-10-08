package cn.zhenxinjian.it.service;

import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.WeightRecordVO;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.it.TestConfig;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import cn.zhenxinjian.service.impl.PlateauDetector;
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
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Autowired
    private PlateauDetector plateauDetector;

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

    /** 场景：同日多条共存，每日末值聚合只返回 id 最大者 */
    @Test
    void selectDailyLast_sameDayMultiple_returnsLastOnly() {
        LocalDate today = LocalDate.now();
        insert(1L, today, 58.0);
        insert(1L, today, 57.5);
        insert(1L, today, 57.0);

        List<WeightRecord> dailyLast = weightRecordMapper.selectDailyLast(1L, today, today);

        assertEquals(1, dailyLast.size());
        assertEquals(57.0, dailyLast.get(0).getWeight(), 0.01);
    }

    /** 场景：跨日每日末值各取一点，按日期升序 */
    @Test
    void selectDailyLast_multipleDays_onePointPerDay() {
        LocalDate today = LocalDate.now();
        insert(1L, today.minusDays(1), 58.0);
        insert(1L, today.minusDays(1), 57.8);
        insert(1L, today, 57.0);

        List<WeightRecord> dailyLast =
                weightRecordMapper.selectDailyLast(1L, today.minusDays(1), today);

        assertEquals(2, dailyLast.size());
        assertEquals(today.minusDays(1), dailyLast.get(0).getRecordDate());
        assertEquals(57.8, dailyLast.get(0).getWeight(), 0.01);
        assertEquals(57.0, dailyLast.get(1).getWeight(), 0.01);
    }

    /** 场景：同日多次称重不干扰平台判定（每日仅一点，波动按日末值） */
    @Test
    void isPlateau_sameDayNoise_ignored() {
        LocalDate today = LocalDate.now();
        insert(1L, today.minusDays(1), 57.0);
        insert(1L, today, 58.0);
        insert(1L, today, 57.1);

        // 日末值为 57.0 与 57.1，波动 0.1 → 平台
        assertEquals(true, plateauDetector.isPlateau(1L));
    }

    /** 场景：列表不传 limit 返回全部（无条数上限） */
    @Test
    void list_noLimit_returnsAll() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            insert(1L, today, 57.0 + i);
        }

        List<WeightRecordVO> list = weightService.list(1L, null, null, null);

        assertEquals(3, list.size());
    }

    /** 场景：趋势窗口每日末值点 + 体重差（正=下降） */
    @Test
    void trend_returnsPointsAndDelta() {
        LocalDate today = LocalDate.now();
        insert(1L, today.minusDays(1), 58.0);
        insert(1L, today, 57.0);

        WeightTrendVO vo = weightService.trend(1L, "7");

        assertEquals(2, vo.getPoints().size());
        assertEquals(1.0, vo.getDelta(), 0.001);
        assertEquals(today.minusDays(6).toString(), vo.getStartDate());
    }

    /** 场景：窗口内不足两个不同日 → points/delta 为 null */
    @Test
    void trend_lessThanTwoDays_nullPoints() {
        insert(1L, LocalDate.now(), 57.0);

        WeightTrendVO vo = weightService.trend(1L, "30");

        assertNull(vo.getPoints());
        assertNull(vo.getDelta());
    }

    private void insert(Long userId, LocalDate date, double weight) {
        WeightRecord r = new WeightRecord();
        r.setUserId(userId);
        r.setRecordDate(date);
        r.setWeight(weight);
        weightRecordMapper.insert(r);
    }
}
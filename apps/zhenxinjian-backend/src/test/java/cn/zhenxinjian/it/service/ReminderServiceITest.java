package cn.zhenxinjian.it.service;

import cn.zhenxinjian.domain.dto.ReminderSaveDTO;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.domain.vo.ReminderVO;
import cn.zhenxinjian.it.TestConfig;
import cn.zhenxinjian.mapper.UserReminderMapper;
import cn.zhenxinjian.service.impl.ReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 三餐提醒设置 Service 集成测试 — 验证 ReminderService + DB 全链路
 * 覆盖：首次查询默认值落库、重复查询幂等、整体保存覆盖、到点扫描命中
 * 作者: wanglx
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@org.springframework.context.annotation.Import(TestConfig.class)
class ReminderServiceITest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private UserReminderMapper userReminderMapper;

    /** 场景：首次查询无记录 → 按默认值（全开 08:30/12:00/18:30）落库 */
    @Test
    void getOrCreate_firstTime_persistsDefaults() {
        ReminderVO vo = reminderService.getOrCreate(9001L);

        assertNotNull(vo);
        assertEquals(1, vo.getMasterSwitch());
        assertEquals(1, vo.getBreakfastSwitch());
        assertEquals("08:30", vo.getBreakfastTime());
        assertEquals("12:00", vo.getLunchTime());
        assertEquals("18:30", vo.getDinnerTime());
        assertEquals(0, vo.getSubscribeCredit());

        Long count = userReminderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserReminder>()
                        .eq(UserReminder::getUserId, 9001L));
        assertEquals(1L, count);
    }

    /** 场景：重复查询不重复落库（每用户活跃唯一） */
    @Test
    void getOrCreate_secondTime_isIdempotent() {
        reminderService.getOrCreate(9002L);
        reminderService.getOrCreate(9002L);

        Long count = userReminderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserReminder>()
                        .eq(UserReminder::getUserId, 9002L));
        assertEquals(1L, count);
    }

    /** 场景：整体保存覆盖总开关与三餐时间 */
    @Test
    void save_overwritesSettings() {
        ReminderSaveDTO dto = new ReminderSaveDTO();
        dto.setMasterSwitch(1);
        dto.setBreakfastSwitch(0);
        dto.setBreakfastTime("07:15");
        dto.setLunchSwitch(1);
        dto.setLunchTime("12:30");
        dto.setDinnerSwitch(0);
        dto.setDinnerTime("19:00");

        ReminderVO vo = reminderService.save(9003L, dto);

        assertEquals(1, vo.getMasterSwitch());
        assertEquals(0, vo.getBreakfastSwitch());
        assertEquals("07:15", vo.getBreakfastTime());
        assertEquals("12:30", vo.getLunchTime());
        assertEquals(0, vo.getDinnerSwitch());
        assertEquals("19:00", vo.getDinnerTime());
    }

    /** 场景：到点扫描只返回总开关与对应餐开关均开且时间命中窗口的设置 */
    @Test
    void scanDueReminders_returnsOnlyMatchingWindow() {
        // 命中：早餐 08:30
        reminderService.save(9101L, build(1, 1, "08:30", 0, "12:00", 0, "18:30"));
        // 不命中：总开关关
        reminderService.save(9102L, build(0, 1, "08:30", 1, "12:00", 1, "18:30"));
        // 不命中：早餐开关关
        reminderService.save(9103L, build(1, 0, "08:30", 1, "12:00", 1, "18:30"));

        List<UserReminder> due = reminderService.scanDueReminders(List.of("08:30"), 100);

        assertEquals(1, due.size());
        assertEquals(9101L, due.get(0).getUserId());
    }

    private ReminderSaveDTO build(int master, int bSw, String bTime,
                                  int lSw, String lTime, int dSw, String dTime) {
        ReminderSaveDTO dto = new ReminderSaveDTO();
        dto.setMasterSwitch(master);
        dto.setBreakfastSwitch(bSw);
        dto.setBreakfastTime(bTime);
        dto.setLunchSwitch(lSw);
        dto.setLunchTime(lTime);
        dto.setDinnerSwitch(dSw);
        dto.setDinnerTime(dTime);
        return dto;
    }
}

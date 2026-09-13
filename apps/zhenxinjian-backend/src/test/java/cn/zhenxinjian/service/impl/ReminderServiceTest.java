package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.config.WxMaConfiguration;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.mapper.ReminderSendLogMapper;
import cn.zhenxinjian.mapper.UserReminderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 三餐提醒设置服务单元测试（推送判定下沉查询：scanDueReminders / hasSuccessPushToday）
 * 作者: wanglx
 */
class ReminderServiceTest {

    private UserReminderMapper userReminderMapper;
    private ReminderSendLogMapper reminderSendLogMapper;
    private ReminderService service;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserReminder.class);
        TableInfoHelper.initTableInfo(assistant, cn.zhenxinjian.domain.po.ReminderSendLog.class);

        userReminderMapper = mock(UserReminderMapper.class);
        reminderSendLogMapper = mock(ReminderSendLogMapper.class);
        service = new ReminderService(userReminderMapper, reminderSendLogMapper,
                new WxMaConfiguration.WxMaProperties(), mock(StringRedisTemplate.class));
    }

    /** 场景：scanDueReminders 透传查询结果（窗口 IN 匹配 + 上限），命中列表原样返回 */
    @Test
    void scanDueReminders_passthroughWithLimit() {
        UserReminder hit = new UserReminder();
        hit.setUserId(1L);
        when(userReminderMapper.selectList(any())).thenReturn(List.of(hit));

        List<UserReminder> result = service.scanDueReminders(List.of("08:28", "08:29", "08:30"), 500);

        assertSame(hit, result.get(0));
        verify(userReminderMapper).selectList(any());
    }

    /** 场景：hasSuccessPushToday 已成功 → true；未推送/计数 null → false */
    @Test
    void hasSuccessPushToday_branches() {
        when(reminderSendLogMapper.selectCount(any())).thenReturn(1L);
        assertTrue(service.hasSuccessPushToday(1L, 1, LocalDate.now()));

        when(reminderSendLogMapper.selectCount(any())).thenReturn(0L);
        assertFalse(service.hasSuccessPushToday(1L, 2, LocalDate.now()));

        when(reminderSendLogMapper.selectCount(any())).thenReturn(null);
        assertFalse(service.hasSuccessPushToday(1L, 3, LocalDate.now()));
    }
}

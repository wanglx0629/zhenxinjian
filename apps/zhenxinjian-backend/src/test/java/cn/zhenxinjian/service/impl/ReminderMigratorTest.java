package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.ReminderSendLog;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.mapper.ReminderSendLogMapper;
import cn.zhenxinjian.mapper.UserReminderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提醒迁移器单元测试
 * 作者: wanglx
 */
class ReminderMigratorTest {

    private UserReminderMapper userReminderMapper;
    private ReminderSendLogMapper reminderSendLogMapper;
    private ReminderMigrator migrator;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserReminder.class);
        TableInfoHelper.initTableInfo(assistant, ReminderSendLog.class);

        userReminderMapper = mock(UserReminderMapper.class);
        reminderSendLogMapper = mock(ReminderSendLogMapper.class);
        migrator = new ReminderMigrator(userReminderMapper, reminderSendLogMapper);
    }

    /** 场景：正式用户已有设置 → 游客设置逻辑删除，保留正式设置 */
    @Test
    void migrate_formalHasExisting_deletesGuest() {
        when(userReminderMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(userReminderMapper.delete(any(Wrapper.class))).thenReturn(1);
        when(reminderSendLogMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        migrator.migrate(10L, 20L);

        verify(userReminderMapper).delete(any(Wrapper.class));
        verify(reminderSendLogMapper).update(any(), any(Wrapper.class));
    }

    /** 场景：正式用户无设置 → 游客设置迁移归属 */
    @Test
    void migrate_formalNone_updatesGuest() {
        when(userReminderMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userReminderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(reminderSendLogMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        migrator.migrate(10L, 20L);

        verify(userReminderMapper).update(any(), any(Wrapper.class));
        verify(reminderSendLogMapper).update(any(), any(Wrapper.class));
    }

    /** 场景：过期游客清理 → 删除提醒设置和推送日志 */
    @Test
    void purge_deletesBoth() {
        when(reminderSendLogMapper.delete(any(Wrapper.class))).thenReturn(1);
        when(userReminderMapper.delete(any(Wrapper.class))).thenReturn(1);

        migrator.purge(10L);

        verify(reminderSendLogMapper).delete(any(Wrapper.class));
        verify(userReminderMapper).delete(any(Wrapper.class));
    }
}
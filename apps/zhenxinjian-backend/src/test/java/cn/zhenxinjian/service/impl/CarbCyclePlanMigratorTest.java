package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.CarbCyclePlan;
import cn.zhenxinjian.mapper.CarbCycleDayMapper;
import cn.zhenxinjian.mapper.CarbCyclePlanMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 碳循环周期游客迁移器单元测试（归属迁移幂等 / 冲突降级 / purge 两表逻辑删除）
 * 作者: wanglx
 */
class CarbCyclePlanMigratorTest {

    private CarbCyclePlanMapper planMapper;
    private CarbCycleDayMapper dayMapper;
    private CarbCyclePlanMigrator migrator;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CarbCyclePlan.class);
        TableInfoHelper.initTableInfo(assistant, CarbCycleDay.class);

        planMapper = mock(CarbCyclePlanMapper.class);
        dayMapper = mock(CarbCycleDayMapper.class);
        migrator = new CarbCyclePlanMigrator(planMapper, dayMapper);
    }

    /** 场景：正式用户无进行中周期 → 两表各 1 条归属 UPDATE，无降级终止 */
    @Test
    @SuppressWarnings("unchecked")
    void migrate_noConflict_updatesOwnership() {
        when(planMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        migrator.migrate(7L, 1L);

        // 仅归属 UPDATE（plan 1 次 + day 1 次），无额外终止 UPDATE
        verify(planMapper, times(1)).update(isNull(), any(Wrapper.class));
        verify(dayMapper, times(1)).update(isNull(), any(Wrapper.class));
    }

    /** 场景：正式用户已有进行中周期 → 游客进行中周期先降级终止，再改归属 */
    @Test
    @SuppressWarnings("unchecked")
    void migrate_conflict_terminatesGuestActiveFirst() {
        when(planMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        migrator.migrate(7L, 1L);

        // plan 两次 UPDATE：先终止游客进行中周期，再改归属
        verify(planMapper, times(2)).update(isNull(), any(Wrapper.class));
        verify(dayMapper, times(1)).update(isNull(), any(Wrapper.class));
    }

    /** 场景：migrate 重复执行 → 同样 UPDATE 重复下发，数据库层幂等（影响 0 行） */
    @Test
    @SuppressWarnings("unchecked")
    void migrate_repeated_idempotent() {
        when(planMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        migrator.migrate(7L, 1L);
        migrator.migrate(7L, 1L);

        verify(planMapper, times(2)).update(isNull(), any(Wrapper.class));
        verify(dayMapper, times(2)).update(isNull(), any(Wrapper.class));
    }

    /** 场景：purge 两表按 guestId 删除（@TableLogic 转 UPDATE delete_flag 逻辑删除） */
    @Test
    @SuppressWarnings("unchecked")
    void purge_deletesBothTablesByGuestId() {
        migrator.purge(7L);

        verify(planMapper).delete(any(Wrapper.class));
        verify(dayMapper).delete(any(Wrapper.class));
    }
}

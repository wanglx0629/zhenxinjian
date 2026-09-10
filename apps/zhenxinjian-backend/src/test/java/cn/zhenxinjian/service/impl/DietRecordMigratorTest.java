package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.mapper.DietRecordMapper;
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

/**
 * 饮食记录游客迁移器单元测试（归属迁移 / 幂等重复执行 / purge 逻辑删除）
 * 作者: wanglx
 */
class DietRecordMigratorTest {

    private DietRecordMapper dietRecordMapper;
    private DietRecordMigrator migrator;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DietRecord.class);

        dietRecordMapper = mock(DietRecordMapper.class);
        migrator = new DietRecordMigrator(dietRecordMapper);
    }

    /** 场景：migrate 改归属 → 单条 UPDATE 下发（按 guestId 过滤，正式用户记录不受影响） */
    @Test
    @SuppressWarnings("unchecked")
    void migrate_updatesOwnershipByGuestId() {
        migrator.migrate(7L, 1L);

        verify(dietRecordMapper).update(isNull(), any(Wrapper.class));
    }

    /** 场景：migrate 重复执行 → 同样 UPDATE 重复下发，数据库层幂等（影响 0 行） */
    @Test
    @SuppressWarnings("unchecked")
    void migrate_repeated_idempotent() {
        migrator.migrate(7L, 1L);
        migrator.migrate(7L, 1L);

        verify(dietRecordMapper, times(2)).update(isNull(), any(Wrapper.class));
    }

    /** 场景：purge 按 guestId 删除（@TableLogic 转 UPDATE delete_flag 逻辑删除） */
    @Test
    @SuppressWarnings("unchecked")
    void purge_deletesByGuestId() {
        migrator.purge(7L);

        verify(dietRecordMapper).delete(any(Wrapper.class));
    }
}

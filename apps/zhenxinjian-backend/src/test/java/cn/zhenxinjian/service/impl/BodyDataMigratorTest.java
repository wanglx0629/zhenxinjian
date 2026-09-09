package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserBodyHistory;
import cn.zhenxinjian.mapper.UserBodyHistoryMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身体档案游客迁移器单元测试（Mock mapper 验证归属迁移/冲突降级/幂等三场景）
 * 作者: wanglx
 */
class BodyDataMigratorTest {

    private UserBodyMapper userBodyMapper;
    private UserBodyHistoryMapper userBodyHistoryMapper;
    private BodyDataMigrator migrator;

    @BeforeEach
    void setUp() {
        // 初始化实体元数据：无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserBody.class);
        TableInfoHelper.initTableInfo(assistant, UserBodyHistory.class);

        userBodyMapper = mock(UserBodyMapper.class);
        userBodyHistoryMapper = mock(UserBodyHistoryMapper.class);
        migrator = new BodyDataMigrator(userBodyMapper, userBodyHistoryMapper);
    }

    /** 场景1：正式用户无档案 → 游客当前档案整体改归属 */
    @Test
    void migrate_formalEmpty_transfersOwnership() {
        UserBody guestBody = activeBody(11L, 7L);
        // selectOne 调用顺序：先查游客、后查正式用户
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(guestBody, (UserBody) null);

        migrator.migrate(7L, 1L);

        // 历史快照改归属
        verify(userBodyHistoryMapper).update(isNull(), any(Wrapper.class));
        // 当前档案改归属，不走降级删除
        verify(userBodyMapper).update(isNull(), any(Wrapper.class));
        verify(userBodyMapper, never()).deleteById(any(Long.class));
        verify(userBodyHistoryMapper, never()).insert(any(UserBodyHistory.class));
    }

    /** 场景2：正式用户已有档案 → 游客档案降级入 history 且原行逻辑删除，不覆盖正式用户当前值 */
    @Test
    void migrate_formalHasBody_demotesGuestBodyToHistory() {
        UserBody guestBody = activeBody(11L, 7L);
        UserBody formalBody = activeBody(22L, 1L);
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(guestBody, formalBody);

        migrator.migrate(7L, 1L);

        ArgumentCaptor<UserBodyHistory> captor = ArgumentCaptor.forClass(UserBodyHistory.class);
        verify(userBodyHistoryMapper).insert(captor.capture());
        UserBodyHistory demoted = captor.getValue();
        assertNull(demoted.getId());
        assertEquals(11L, demoted.getUserBodyId());
        assertEquals(1L, demoted.getUserId());
        assertNotNull(demoted.getArchivedAt());

        verify(userBodyMapper).deleteById(11L);
        // 绝不改正式用户当前档案
        verify(userBodyMapper, never()).update(isNull(), any(Wrapper.class));
    }

    /** 场景3：幂等——游客档案已迁移/不存在，仅历史改归属（重复执行影响 0 行） */
    @Test
    void migrate_guestBodyAbsent_idempotent() {
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn((UserBody) null);

        migrator.migrate(7L, 1L);

        verify(userBodyHistoryMapper).update(isNull(), any(Wrapper.class));
        verify(userBodyMapper, never()).update(isNull(), any(Wrapper.class));
        verify(userBodyMapper, never()).deleteById(any(Long.class));
        verify(userBodyHistoryMapper, never()).insert(any(UserBodyHistory.class));
    }

    /** purge：逻辑删除游客名下档案与历史 */
    @Test
    void purge_deletesBothTables() {
        migrator.purge(7L);

        verify(userBodyMapper).delete(any(Wrapper.class));
        verify(userBodyHistoryMapper).delete(any(Wrapper.class));
    }

    private UserBody activeBody(Long id, Long userId) {
        UserBody ub = new UserBody();
        ub.setId(id);
        ub.setUserId(userId);
        ub.setGender(2);
        ub.setAge(30);
        ub.setHeight(162.0);
        ub.setWeight(55.0);
        ub.setBmr(1252);
        return ub;
    }
}

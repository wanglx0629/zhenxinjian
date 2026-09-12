package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.cache.UserCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 用户记录层迁移器单元测试
 * 核心契约：缓存失效延迟到事务提交之后（afterCommit），避免合并提交前并发读重建旧缓存
 * 作者: wanglx
 */
class UserRecordMigratorTest {

    private final UserCacheService userCacheService = mock(UserCacheService.class);
    private final UserRecordMigrator migrator = new UserRecordMigrator(userCacheService);

    @AfterEach
    void tearDown() {
        // 防御：用例中途失败时清理线程绑定的同步器，避免污染其他用例
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void migrateShouldDeferCacheEvictUntilAfterCommit() {
        List<TransactionSynchronization> synchronizations = new ArrayList<>();
        TransactionSynchronizationManager.initSynchronization();
        try {
            migrator.migrate(401L, 400L);
            // 提交前：缓存不失效（关键断言——修复前此处已 evict）
            verify(userCacheService, never()).evict(401L);
            verify(userCacheService, never()).evict(400L);

            // 模拟事务提交：触发 afterCommit
            synchronizations.addAll(TransactionSynchronizationManager.getSynchronizations());
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(userCacheService).evict(401L);
        verify(userCacheService).evict(400L);
    }

    @Test
    void purgeShouldDeferCacheEvictUntilAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            migrator.purge(401L);
            verify(userCacheService, never()).evict(401L);

            List<TransactionSynchronization> synchronizations =
                    new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(userCacheService).evict(401L);
    }

    @Test
    void migrateWithoutTransactionShouldEvictImmediately() {
        // 无活跃事务同步时兜底立即失效（防御非事务调用场景）
        migrator.migrate(401L, 400L);

        verify(userCacheService).evict(401L);
        verify(userCacheService).evict(400L);
    }
}

package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.service.GuestDataMigrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 用户记录层迁移器
 * 仅处理 users 表本身的合并标记与软删（WechatAuthServiceImpl.migrateGuest 已完成主体逻辑）；
 * 缓存失效延迟到事务提交之后（afterCommit），避免合并提交前并发读重建旧缓存
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRecordMigrator implements GuestDataMigrator {

    private final UserCacheService userCacheService;

    @Override
    public void migrate(Long guestId, Long formalId) {
        // 用户记录层合并（merged_into + 软删）由 WechatAuthServiceImpl 事务内统一执行；
        // 此处仅负责缓存失效，且须在合并提交之后才执行
        evictAfterCommit(guestId, formalId);
        log.info("[UserRecordMigrator] 用户记录迁移缓存刷新(提交后): guestId={}, formalId={}", guestId, formalId);
    }

    @Override
    public void purge(Long guestId) {
        evictAfterCommit(guestId);
        log.info("[UserRecordMigrator] 过期游客缓存清理(提交后): guestId={}", guestId);
    }

    /**
     * 缓存失效后置到事务提交后；无活跃事务同步时兜底立即失效
     */
    private void evictAfterCommit(Long... ids) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Long id : ids) {
                        userCacheService.evict(id);
                    }
                }
            });
        } else {
            for (Long id : ids) {
                userCacheService.evict(id);
            }
        }
    }
}

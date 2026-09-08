package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.cache.UserCacheService;
import cn.zhenxinjian.service.GuestDataMigrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户记录层迁移器
 * 仅处理 users 表本身的合并标记与软删（WechatAuthServiceImpl.migrateGuest 已完成主体逻辑，
 * 此 Migrator 作为框架首个实现，验证 SpringUtils.getBeansOfType 装配链路）
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
        // 此处仅负责缓存失效，保持迁移后读取一致
        userCacheService.evict(guestId);
        userCacheService.evict(formalId);
        log.info("[UserRecordMigrator] 用户记录迁移缓存刷新: guestId={}, formalId={}", guestId, formalId);
    }

    @Override
    public void purge(Long guestId) {
        userCacheService.evict(guestId);
        log.info("[UserRecordMigrator] 过期游客缓存清理: guestId={}", guestId);
    }
}

package cn.zhenxinjian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 游客数据迁移编排者
 * 迁移/清理双调用点（登录合并、过期清理定时任务）的唯一入口；
 * 注入 List<GuestDataMigrator> 由 Spring 按 Ordered 契约排序，消除散落的 getBeansOfType 遍历
 * 作者: wanglx
 */
@Component
@RequiredArgsConstructor
public class GuestMigrationOrchestrator {

    /** Spring 按 Ordered/getOrder 升序注入全部迁移器 */
    private final List<GuestDataMigrator> migrators;

    /**
     * 编排全部业务迁移器：游客数据归属变更到正式用户（各实现幂等）
     *
     * @param guestId  游客用户ID
     * @param formalId 正式用户ID
     */
    public void migrateAll(Long guestId, Long formalId) {
        for (GuestDataMigrator migrator : migrators) {
            migrator.migrate(guestId, formalId);
        }
    }

    /**
     * 编排全部业务迁移器：清空过期游客业务数据
     *
     * @param guestId 游客用户ID
     */
    public void purgeAll(Long guestId) {
        for (GuestDataMigrator migrator : migrators) {
            migrator.purge(guestId);
        }
    }
}

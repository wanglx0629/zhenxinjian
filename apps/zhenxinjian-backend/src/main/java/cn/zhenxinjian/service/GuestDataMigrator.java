package cn.zhenxinjian.service;

import org.springframework.core.Ordered;

/**
 * 游客数据迁移器
 * 各业务模块实现本接口，注册为 Spring Bean 后由 GuestMigrationOrchestrator 统一编排调用；
 * 执行顺序经 Ordered 契约化（默认 0，值小先执行），新增迁移器如有顺序依赖须显式声明
 * 作者: wanglx
 */
public interface GuestDataMigrator extends Ordered {

    /**
     * 执行顺序（默认 0；当前各实现操作不同业务表、无顺序依赖）
     */
    @Override
    default int getOrder() {
        return 0;
    }

    /**
     * 将游客名下业务数据归属变更为正式用户（须幂等：重复调用不产生重复数据）
     *
     * @param guestId  游客用户ID
     * @param formalId 正式用户ID
     */
    void migrate(Long guestId, Long formalId);

    /**
     * 清空过期游客的业务数据（到期超 7 天未合并，定时任务调用）
     *
     * @param guestId 游客用户ID
     */
    void purge(Long guestId);
}

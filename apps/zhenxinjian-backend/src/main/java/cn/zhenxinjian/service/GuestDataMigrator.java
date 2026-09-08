package cn.zhenxinjian.service;

/**
 * 游客数据迁移器
 * 各业务模块实现本接口，注册为 Spring Bean 后由登录流程自动调用
 * 作者: wanglx
 */
public interface GuestDataMigrator {

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

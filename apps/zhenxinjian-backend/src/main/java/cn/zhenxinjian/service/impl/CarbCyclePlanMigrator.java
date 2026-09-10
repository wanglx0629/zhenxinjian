package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.CyclePlanStatusEnum;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.CarbCyclePlan;
import cn.zhenxinjian.mapper.CarbCycleDayMapper;
import cn.zhenxinjian.mapper.CarbCyclePlanMapper;
import cn.zhenxinjian.service.GuestDataMigrator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 碳循环周期游客迁移器
 * 登录时把游客名下周期与每日计划归属正式用户（幂等 UPDATE）；
 * 冲突处理——正式用户已有进行中周期时，迁入的进行中周期置为已终止（至多一个进行中）；
 * 过期游客清理由定时任务触发，两表逻辑删除
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarbCyclePlanMigrator implements GuestDataMigrator {

    private final CarbCyclePlanMapper carbCyclePlanMapper;

    private final CarbCycleDayMapper carbCycleDayMapper;

    @Override
    public void migrate(Long guestId, Long formalId) {
        // 冲突降级：正式用户已有进行中周期时，游客进行中周期迁入前先行终止
        Long formalActive = carbCyclePlanMapper.selectCount(
                Wrappers.<CarbCyclePlan>lambdaQuery()
                        .eq(CarbCyclePlan::getUserId, formalId)
                        .eq(CarbCyclePlan::getStatus, CyclePlanStatusEnum.ACTIVE.getCode()));
        if (formalActive != null && formalActive > 0) {
            carbCyclePlanMapper.update(null, Wrappers.<CarbCyclePlan>lambdaUpdate()
                    .set(CarbCyclePlan::getStatus, CyclePlanStatusEnum.TERMINATED.getCode())
                    .eq(CarbCyclePlan::getUserId, guestId)
                    .eq(CarbCyclePlan::getStatus, CyclePlanStatusEnum.ACTIVE.getCode()));
            log.info("[CarbCyclePlanMigrator] 正式用户已有进行中周期，游客周期降级终止: guestId={}, formalId={}",
                    guestId, formalId);
        }
        // 幂等：重复执行影响 0 行；逻辑删除过滤由 MyBatis-Plus 全局处理
        carbCyclePlanMapper.update(null, Wrappers.<CarbCyclePlan>lambdaUpdate()
                .set(CarbCyclePlan::getUserId, formalId)
                .eq(CarbCyclePlan::getUserId, guestId));
        carbCycleDayMapper.update(null, Wrappers.<CarbCycleDay>lambdaUpdate()
                .set(CarbCycleDay::getUserId, formalId)
                .eq(CarbCycleDay::getUserId, guestId));
        log.info("[CarbCyclePlanMigrator] 游客碳循环周期归属迁移: guestId={}, formalId={}", guestId, formalId);
    }

    @Override
    public void purge(Long guestId) {
        carbCycleDayMapper.delete(Wrappers.<CarbCycleDay>lambdaQuery().eq(CarbCycleDay::getUserId, guestId));
        carbCyclePlanMapper.delete(Wrappers.<CarbCyclePlan>lambdaQuery().eq(CarbCyclePlan::getUserId, guestId));
        log.info("[CarbCyclePlanMigrator] 过期游客碳循环周期清理: guestId={}", guestId);
    }
}

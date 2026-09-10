package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.service.GuestDataMigrator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 饮食记录游客迁移器
 * 登录时把游客名下饮食记录归属正式用户（幂等 UPDATE，记录无跨用户唯一约束，直接改归属）；
 * 过期游客清理由定时任务触发，逻辑删除保历史口径
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DietRecordMigrator implements GuestDataMigrator {

    private final DietRecordMapper dietRecordMapper;

    @Override
    public void migrate(Long guestId, Long formalId) {
        // 幂等：重复执行影响 0 行；逻辑删除过滤由 MyBatis-Plus 全局处理
        dietRecordMapper.update(null, Wrappers.<DietRecord>lambdaUpdate()
                .set(DietRecord::getUserId, formalId)
                .eq(DietRecord::getUserId, guestId));
        log.info("[DietRecordMigrator] 游客饮食记录归属迁移: guestId={}, formalId={}", guestId, formalId);
    }

    @Override
    public void purge(Long guestId) {
        dietRecordMapper.delete(Wrappers.<DietRecord>lambdaQuery().eq(DietRecord::getUserId, guestId));
        log.info("[DietRecordMigrator] 过期游客饮食记录清理: guestId={}", guestId);
    }
}

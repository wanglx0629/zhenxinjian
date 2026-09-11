package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.AdjustLog;
import cn.zhenxinjian.domain.po.UserMenstrual;
import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.mapper.AdjustLogMapper;
import cn.zhenxinjian.mapper.UserMenstrualMapper;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import cn.zhenxinjian.service.GuestDataMigrator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 经期设置/体重记录/调碳日志 游客迁移器
 * 登录时把游客名下三项数据归属正式用户（幂等）；
 * 经期设置每用户活跃唯一：冲突时保留正式用户已有设置，游客设置降级释放（软删），
 * 体重记录与调碳日志无唯一约束直接改归属；
 * 过期游客清理由定时任务触发，三表逻辑删除
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Taper532Migrator implements GuestDataMigrator {

    private final UserMenstrualMapper userMenstrualMapper;

    private final WeightRecordMapper weightRecordMapper;

    private final AdjustLogMapper adjustLogMapper;

    @Override
    public void migrate(Long guestId, Long formalId) {
        // 经期设置：正式用户已有活跃设置时，游客设置软删降级（保留正式）
        UserMenstrual guest = selectActive(guestId);
        if (guest != null) {
            if (selectActive(formalId) == null) {
                userMenstrualMapper.update(null, Wrappers.<UserMenstrual>lambdaUpdate()
                        .set(UserMenstrual::getUserId, formalId)
                        .eq(UserMenstrual::getId, guest.getId()));
                log.info("[Taper532Migrator] 游客经期设置归属迁移: guestId={}, formalId={}", guestId, formalId);
            } else {
                userMenstrualMapper.deleteById(guest.getId());
                log.info("[Taper532Migrator] 正式用户已有经期设置，游客设置降级释放: guestId={}, formalId={}",
                        guestId, formalId);
            }
        }

        // 体重记录/调碳日志：无跨用户唯一约束，直接改归属（幂等：重复执行影响 0 行）
        weightRecordMapper.update(null, Wrappers.<WeightRecord>lambdaUpdate()
                .set(WeightRecord::getUserId, formalId)
                .eq(WeightRecord::getUserId, guestId));
        adjustLogMapper.update(null, Wrappers.<AdjustLog>lambdaUpdate()
                .set(AdjustLog::getUserId, formalId)
                .eq(AdjustLog::getUserId, guestId));
        log.info("[Taper532Migrator] 游客体重记录与调碳日志归属迁移: guestId={}, formalId={}", guestId, formalId);
    }

    @Override
    public void purge(Long guestId) {
        userMenstrualMapper.delete(Wrappers.<UserMenstrual>lambdaQuery().eq(UserMenstrual::getUserId, guestId));
        weightRecordMapper.delete(Wrappers.<WeightRecord>lambdaQuery().eq(WeightRecord::getUserId, guestId));
        adjustLogMapper.delete(Wrappers.<AdjustLog>lambdaQuery().eq(AdjustLog::getUserId, guestId));
        log.info("[Taper532Migrator] 过期游客经期/体重/调碳数据清理: guestId={}", guestId);
    }

    /** 查当前活跃经期设置（逻辑删除由 MyBatis-Plus 全局过滤） */
    private UserMenstrual selectActive(Long userId) {
        return userMenstrualMapper.selectOne(
                Wrappers.<UserMenstrual>lambdaQuery().eq(UserMenstrual::getUserId, userId));
    }
}
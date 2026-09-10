package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.ReminderSendLog;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.mapper.ReminderSendLogMapper;
import cn.zhenxinjian.mapper.UserReminderMapper;
import cn.zhenxinjian.service.GuestDataMigrator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 三餐提醒游客迁移器
 * 登录时把游客名下提醒设置与推送日志归属正式用户（幂等 UPDATE）；
 * 冲突处理——正式用户已有设置时保留正式设置，游客设置逻辑删除；
 * 过期游客清理由定时任务触发，两表逻辑删除
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderMigrator implements GuestDataMigrator {

    private final UserReminderMapper userReminderMapper;

    private final ReminderSendLogMapper reminderSendLogMapper;

    @Override
    public void migrate(Long guestId, Long formalId) {
        // 冲突降级：正式用户已有设置时，游客设置迁入前逻辑删除（保留正式设置）
        Long formalCount = userReminderMapper.selectCount(
                Wrappers.<UserReminder>lambdaQuery().eq(UserReminder::getUserId, formalId));
        if (formalCount != null && formalCount > 0) {
            userReminderMapper.delete(Wrappers.<UserReminder>lambdaQuery()
                    .eq(UserReminder::getUserId, guestId));
            log.info("[ReminderMigrator] 正式用户已有提醒设置，游客设置逻辑删除: guestId={}, formalId={}",
                    guestId, formalId);
        } else {
            // 幂等：重复执行影响 0 行；逻辑删除过滤由 MyBatis-Plus 全局处理
            userReminderMapper.update(null, Wrappers.<UserReminder>lambdaUpdate()
                    .set(UserReminder::getUserId, formalId)
                    .eq(UserReminder::getUserId, guestId));
            log.info("[ReminderMigrator] 游客提醒设置归属迁移: guestId={}, formalId={}", guestId, formalId);
        }
        // 推送日志一并改归属（追溯完整，幂等同理）
        reminderSendLogMapper.update(null, Wrappers.<ReminderSendLog>lambdaUpdate()
                .set(ReminderSendLog::getUserId, formalId)
                .eq(ReminderSendLog::getUserId, guestId));
    }

    @Override
    public void purge(Long guestId) {
        reminderSendLogMapper.delete(Wrappers.<ReminderSendLog>lambdaQuery()
                .eq(ReminderSendLog::getUserId, guestId));
        userReminderMapper.delete(Wrappers.<UserReminder>lambdaQuery()
                .eq(UserReminder::getUserId, guestId));
        log.info("[ReminderMigrator] 过期游客提醒数据清理: guestId={}", guestId);
    }
}

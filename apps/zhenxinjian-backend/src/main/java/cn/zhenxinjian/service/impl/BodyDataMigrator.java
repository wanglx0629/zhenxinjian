package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserBodyHistory;
import cn.zhenxinjian.mapper.UserBodyHistoryMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.service.GuestDataMigrator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 身体档案游客迁移器
 * 登录时把游客名下档案/历史归属正式用户（幂等）；
 * 正式用户已有活跃档案时游客档案降级入 history，不覆盖正式用户当前值
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BodyDataMigrator implements GuestDataMigrator {

    private final UserBodyMapper userBodyMapper;

    private final UserBodyHistoryMapper userBodyHistoryMapper;

    @Override
    public void migrate(Long guestId, Long formalId) {
        // 历史快照直接改归属（幂等：重复执行影响 0 行）
        userBodyHistoryMapper.update(null, Wrappers.<UserBodyHistory>lambdaUpdate()
                .set(UserBodyHistory::getUserId, formalId)
                .eq(UserBodyHistory::getUserId, guestId));

        UserBody guestBody = selectActive(guestId);
        if (guestBody == null) {
            return;
        }
        if (selectActive(formalId) == null) {
            // 正式用户无档案：游客当前档案整体改归属
            userBodyMapper.update(null, Wrappers.<UserBody>lambdaUpdate()
                    .set(UserBody::getUserId, formalId)
                    .eq(UserBody::getId, guestBody.getId()));
            log.info("[BodyDataMigrator] 游客档案归属迁移: guestId={}, formalId={}", guestId, formalId);
        } else {
            // 冲突：游客活跃档案降级为历史快照（归属正式用户），原行逻辑删除释放活跃唯一约束
            UserBodyHistory demoted = new UserBodyHistory();
            BeanUtils.copyProperties(guestBody, demoted);
            demoted.setId(null);
            demoted.setUserBodyId(guestBody.getId());
            demoted.setUserId(formalId);
            demoted.setArchivedAt(LocalDateTime.now());
            userBodyHistoryMapper.insert(demoted);
            userBodyMapper.deleteById(guestBody.getId());
            log.info("[BodyDataMigrator] 游客档案降级入历史: guestId={}, formalId={}", guestId, formalId);
        }
    }

    @Override
    public void purge(Long guestId) {
        userBodyMapper.delete(Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, guestId));
        userBodyHistoryMapper.delete(Wrappers.<UserBodyHistory>lambdaQuery().eq(UserBodyHistory::getUserId, guestId));
        log.info("[BodyDataMigrator] 过期游客档案清理: guestId={}", guestId);
    }

    /** 查当前活跃档案（逻辑删除由 MyBatis-Plus 全局过滤） */
    private UserBody selectActive(Long userId) {
        return userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
    }
}

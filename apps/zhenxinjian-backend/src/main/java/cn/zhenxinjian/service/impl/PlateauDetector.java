package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.AdjustActionEnum;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.common.utils.SqlLimit;
import cn.zhenxinjian.domain.po.AdjustLog;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.AdjustLogVO;
import cn.zhenxinjian.mapper.AdjustLogMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 平台/恢复判定器（体重平台期判定 + 下调/恢复 + 调碳日志留痕）
 * 拆自 WeightService（DESIGN-职责-001），独立承载平台态业务口径
 * 作者: wanglx
 *
 * 口径（spec body/weight + design D5）：
 * 平台判定 = 近 7 天体重记录（≥2 条）max − min < 0.3kg；
 * 下调触发 = 未下调态且平台成立 → is_adjusted=1 + trigger_weight=当日体重 + 下调日志（单次门闩）；
 * 恢复触发 = 下调态且 trigger_weight − 当日体重 ≥ 0.3 → 清除下调态 + 恢复日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlateauDetector {

    /** 平台波动阈值 kg（max − min < 0.3） */
    private static final BigDecimal PLATEAU_DELTA = new BigDecimal("0.3");

    /** 恢复下降阈值 kg（trigger − current ≥ 0.3） */
    private static final BigDecimal RESTORE_DELTA = new BigDecimal("0.3");

    /** 平台判定窗口天数（含今日的自然日窗口，口径「最近 7 天」） */
    private static final int PLATEAU_WINDOW_DAYS = 7;

    private final WeightRecordMapper weightRecordMapper;

    private final UserBodyMapper userBodyMapper;

    private final AdjustLogMapper adjustLogMapper;

    /**
     * 平台判定：近 7 天（含今日）「每日末值」（≥2 个不同日）max − min < 0.3。
     * 同日多次称重不影响判定（每日仅取 id 最大者）。
     *
     * @param userId 当前用户ID
     * @return true=平台期；窗口内不足 2 个不同日返回 false（不判定）
     */
    public boolean isPlateau(Long userId) {
        LocalDate today = LocalDate.now();
        List<WeightRecord> dailyLast = weightRecordMapper.selectDailyLast(userId,
                today.minusDays((long) PLATEAU_WINDOW_DAYS - 1), today);
        if (dailyLast.size() < 2) {
            return false;
        }
        double max = dailyLast.stream().mapToDouble(WeightRecord::getWeight).max().orElse(0);
        double min = dailyLast.stream().mapToDouble(WeightRecord::getWeight).min().orElse(0);
        return BigDecimal.valueOf(max).subtract(BigDecimal.valueOf(min)).compareTo(PLATEAU_DELTA) < 0;
    }

    /**
     * 平台下调/恢复判定（保存体重后调用；单次门闩防连续下调）。
     * 触发/恢复参考统一为「今日末值」（同日 id 最大者）。
     *
     * @param userId 当前用户ID
     */
    public void applyPlatformAdjust(Long userId) {
        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        if (body == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        WeightRecord todayRecord = weightRecordMapper.selectDailyLast(userId, today, today).stream()
                .findFirst().orElse(null);
        if (todayRecord == null) {
            return;
        }
        double todayWeight = todayRecord.getWeight();
        boolean adjusted = body.getIsAdjusted() != null && body.getIsAdjusted() == 1;
        if (!adjusted) {
            // 未下调且平台成立 → 下调
            if (isPlateau(userId)) {
                body.setIsAdjusted(1);
                body.setTriggerWeight(todayWeight);
                body.setUpdateBy(Operators.user(userId));
                userBodyMapper.updateById(body);
                insertLog(userId, AdjustActionEnum.DOWN, todayWeight);
                log.info("[PlateauDetector] 平台下调: userId={}, triggerWeight={}", userId, todayWeight);
            }
            return;
        }
        // 下调态 → 恢复判定：较触发参考下降 ≥0.3
        Double trigger = body.getTriggerWeight();
        if (trigger != null
                && BigDecimal.valueOf(trigger).subtract(BigDecimal.valueOf(todayWeight))
                .compareTo(RESTORE_DELTA) >= 0) {
            body.setIsAdjusted(0);
            body.setTriggerWeight(null);
            body.setUpdateBy(Operators.user(userId));
            userBodyMapper.updateById(body);
            insertLog(userId, AdjustActionEnum.RESTORE, todayWeight);
            log.info("[PlateauDetector] 平台恢复: userId={}, weight={}", userId, todayWeight);
        }
    }

    /** 写调碳日志（追加写留痕） */
    private void insertLog(Long userId, AdjustActionEnum action, double triggerWeight) {
        AdjustLog logEntity = new AdjustLog();
        logEntity.setUserId(userId);
        logEntity.setAction(action.getCode());
        logEntity.setTriggerWeight(triggerWeight);
        logEntity.setStatus(1);
        logEntity.setCreateBy(Operators.user(userId));
        adjustLogMapper.insert(logEntity);
    }

    /**
     * 查询调碳日志（按时间倒序，最近 100 条）
     *
     * @param userId 当前用户ID
     * @return 日志列表
     */
    public List<AdjustLogVO> listLogs(Long userId) {
        return adjustLogMapper.selectList(
                        Wrappers.<AdjustLog>lambdaQuery()
                                .eq(AdjustLog::getUserId, userId)
                                .orderByDesc(AdjustLog::getId)
                                .last(SqlLimit.fixed(100)))
                .stream().map(this::toLogVO).collect(Collectors.toList());
    }

    /** 日志实体转视图 */
    private AdjustLogVO toLogVO(AdjustLog entity) {
        AdjustLogVO vo = new AdjustLogVO();
        vo.setId(entity.getId());
        vo.setAction(entity.getAction());
        AdjustActionEnum action = AdjustActionEnum.of(entity.getAction());
        vo.setActionName(action == null ? null : action.getDesc());
        vo.setTriggerWeight(entity.getTriggerWeight());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
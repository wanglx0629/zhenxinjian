package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.AdjustActionEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.domain.dto.WeightSaveDTO;
import cn.zhenxinjian.domain.po.AdjustLog;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.AdjustLogVO;
import cn.zhenxinjian.domain.vo.WeightRecordVO;
import cn.zhenxinjian.mapper.AdjustLogMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 体重记录与平台调碳服务（按日幂等覆盖 + 平台判定 + 下调/恢复 + 调碳日志）
 * 作者: wanglx
 *
 * 口径（spec body/weight + design D5）：
 * 平台判定 = 近 7 天体重记录（≥2 条）max − min < 0.3kg；
 * 下调触发 = 未下调态且平台成立 → is_adjusted=1 + trigger_weight=当日体重 + 下调日志（单次门闩）；
 * 恢复触发 = 下调态且 trigger_weight − 当日体重 ≥ 0.3 → 清除下调态 + 恢复日志；
 * 目标碳/热实际 −20/−80 由 Taper532Service.todayTarget 应用，本服务只持久化状态与留痕
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeightService {

    /** 体重区间 kg */
    private static final double WEIGHT_MIN = 25;

    private static final double WEIGHT_MAX = 200;

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
     * 保存体重记录：25-200 校验、未来日期拒绝、同日软删后插入（幂等覆盖）；
     * 落库后执行平台下调/恢复判定
     *
     * @param userId 当前用户ID
     * @param dto    保存入参
     * @return 保存后的记录（附平台标记）
     */
    @Transactional(rollbackFor = Exception.class)
    public WeightRecordVO save(Long userId, WeightSaveDTO dto) {
        if (dto.getWeight() == null || dto.getWeight() < WEIGHT_MIN || dto.getWeight() > WEIGHT_MAX) {
            throw new BusinessException(CommonConstant.WEIGHT_INVALID_CODE,
                    ExceptionConstant.WEIGHT_INVALID);
        }
        if (dto.getRecordDate() == null || dto.getRecordDate().isAfter(LocalDate.now())) {
            throw new BusinessException(CommonConstant.WEIGHT_INVALID_CODE,
                    ExceptionConstant.WEIGHT_INVALID);
        }
        LocalDate recordDate = dto.getRecordDate();
        double weight = BigDecimal.valueOf(dto.getWeight()).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // 同日幂等覆盖：先软删当日既有记录，再插入新记录（业务层保证每日至多一条活跃）
        weightRecordMapper.delete(Wrappers.<WeightRecord>lambdaQuery()
                .eq(WeightRecord::getUserId, userId)
                .eq(WeightRecord::getRecordDate, recordDate));

        WeightRecord record = new WeightRecord();
        record.setUserId(userId);
        record.setRecordDate(recordDate);
        record.setWeight(weight);
        record.setStatus(1);
        record.setCreateBy(Operators.user(userId));
        weightRecordMapper.insert(record);

        // 平台下调/恢复判定（以当日体重为触发参考）
        applyPlatformAdjust(userId, weight);

        WeightRecordVO vo = new WeightRecordVO();
        vo.setId(record.getId());
        vo.setRecordDate(record.getRecordDate());
        vo.setWeight(record.getWeight());
        vo.setPlateau(isPlateau(userId));
        return vo;
    }

    /**
     * 查询体重记录：按日期范围（含）过滤，按日期倒序；不传范围默认最近 30 条
     *
     * @param userId    当前用户ID
     * @param startDate 起始日期（含，可空）
     * @param endDate   结束日期（含，可空）
     * @param limit     返回条数上限（默认 30，最大 100）
     * @return 记录列表（含平台标记）
     */
    public List<WeightRecordVO> list(Long userId, LocalDate startDate, LocalDate endDate, Integer limit) {
        int size = limit == null || limit <= 0 ? 30 : Math.min(limit, 100);
        List<WeightRecord> records = weightRecordMapper.selectList(
                Wrappers.<WeightRecord>lambdaQuery()
                        .eq(WeightRecord::getUserId, userId)
                        .ge(startDate != null, WeightRecord::getRecordDate, startDate)
                        .le(endDate != null, WeightRecord::getRecordDate, endDate)
                        .orderByDesc(WeightRecord::getRecordDate)
                        .orderByDesc(WeightRecord::getId)
                        .last("LIMIT " + size));

        boolean plateau = isPlateau(userId);
        return records.stream().map(r -> {
            WeightRecordVO vo = new WeightRecordVO();
            vo.setId(r.getId());
            vo.setRecordDate(r.getRecordDate());
            vo.setWeight(r.getWeight());
            vo.setPlateau(plateau);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 删除体重记录（逻辑删除）
     *
     * @param userId 当前用户ID
     * @param id     记录ID
     */
    public void remove(Long userId, Long id) {
        WeightRecord record = id == null ? null : weightRecordMapper.selectById(id);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(CommonConstant.WEIGHT_INVALID_CODE,
                    ExceptionConstant.WEIGHT_INVALID);
        }
        weightRecordMapper.deleteById(record.getId());
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
                                .last("LIMIT 100"))
                .stream().map(this::toLogVO).collect(Collectors.toList());
    }

    /**
     * 平台判定：近 7 天（含今日）体重记录（≥2 条）max − min < 0.3
     *
     * @param userId 当前用户ID
     * @return true=平台期；窗口内不足 2 条返回 false（不判定）
     */
    public boolean isPlateau(Long userId) {
        LocalDate today = LocalDate.now();
        List<WeightRecord> records = weightRecordMapper.selectList(
                Wrappers.<WeightRecord>lambdaQuery()
                        .eq(WeightRecord::getUserId, userId)
                        .ge(WeightRecord::getRecordDate, today.minusDays((long) PLATEAU_WINDOW_DAYS - 1))
                        .le(WeightRecord::getRecordDate, today)
                        .orderByDesc(WeightRecord::getRecordDate)
                        .orderByDesc(WeightRecord::getId));
        if (records.size() < 2) {
            return false;
        }
        double max = records.stream().mapToDouble(WeightRecord::getWeight).max().orElse(0);
        double min = records.stream().mapToDouble(WeightRecord::getWeight).min().orElse(0);
        return BigDecimal.valueOf(max).subtract(BigDecimal.valueOf(min)).compareTo(PLATEAU_DELTA) < 0;
    }

    /**
     * 平台下调/恢复判定（保存/修改体重后调用；单次门闩防连续下调）
     *
     * @param userId     当前用户ID
     * @param todayWeight 当日体重 kg
     */
    private void applyPlatformAdjust(Long userId, double todayWeight) {
        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        if (body == null) {
            return;
        }
        boolean adjusted = body.getIsAdjusted() != null && body.getIsAdjusted() == 1;
        if (!adjusted) {
            // 未下调且平台成立 → 下调
            if (isPlateau(userId)) {
                body.setIsAdjusted(1);
                body.setTriggerWeight(todayWeight);
                body.setUpdateBy(Operators.user(userId));
                userBodyMapper.updateById(body);
                insertLog(userId, AdjustActionEnum.DOWN, todayWeight);
                log.info("[WeightService] 平台下调: userId={}, triggerWeight={}", userId, todayWeight);
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
            log.info("[WeightService] 平台恢复: userId={}, weight={}", userId, todayWeight);
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
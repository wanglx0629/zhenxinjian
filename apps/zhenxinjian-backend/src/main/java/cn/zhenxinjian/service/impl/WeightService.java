package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.common.utils.SqlLimit;
import cn.zhenxinjian.domain.dto.WeightSaveDTO;
import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.domain.vo.AdjustLogVO;
import cn.zhenxinjian.domain.vo.WeightRecordVO;
import cn.zhenxinjian.domain.vo.WeightTrendVO;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 体重记录服务（CRUD 编排；平台判定下沉 PlateauDetector，趋势聚合下沉 WeightAggregateService）
 * 作者: wanglx
 *
 * 口径（spec body/weight + design D5）：
 * 平台判定 = 近 7 天体重记录（≥2 条）max − min < 0.3kg；
 * 下调/恢复与趋势聚合分别由 PlateauDetector / WeightAggregateService 承载；
 * 目标碳/热实际 −20/−80 由 Taper532Service.todayTarget 应用，本服务只持久化状态与留痕
 */
@Service
@RequiredArgsConstructor
public class WeightService {

    /** 体重区间 kg */
    private static final double WEIGHT_MIN = 25;

    private static final double WEIGHT_MAX = 200;

    /** 列表查询条数上限（有界防全表拖垮；不传 limit 仍返回全部） */
    private static final int WEIGHT_LIST_LIMIT_MAX = 200;

    private final WeightRecordMapper weightRecordMapper;

    private final PlateauDetector plateauDetector;

    private final WeightAggregateService weightAggregateService;

    /**
     * 保存体重记录：25-200 校验、未来日期拒绝、同日直接插入（幂等覆盖）；
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

        // 同日允许多条共存（随时可称重），直接插入；当日末值由 MAX(id) 决定
        WeightRecord record = new WeightRecord();
        record.setUserId(userId);
        record.setRecordDate(recordDate);
        record.setWeight(weight);
        record.setStatus(1);
        record.setCreateBy(Operators.user(userId));
        weightRecordMapper.insert(record);

        // 平台下调/恢复判定（以「今日末值」为触发参考）
        plateauDetector.applyPlatformAdjust(userId);

        WeightRecordVO vo = new WeightRecordVO();
        vo.setId(record.getId());
        vo.setRecordDate(record.getRecordDate());
        vo.setWeight(record.getWeight());
        vo.setPlateau(plateauDetector.isPlateau(userId));
        return vo;
    }

    /**
     * 查询体重记录：按日期范围（含）过滤，按日期倒序、同日按 id 倒序，不做条数上限（列表展示全部）。
     *
     * @param userId    当前用户ID
     * @param startDate 起始日期（含，可空）
     * @param endDate   结束日期（含，可空）
     * @param limit     返回条数上限（仅显式传入正整数时生效，不传 = 不限制）
     * @return 记录列表（含平台标记）
     */
    public List<WeightRecordVO> list(Long userId, LocalDate startDate, LocalDate endDate, Integer limit) {
        var query = Wrappers.<WeightRecord>lambdaQuery()
                .eq(WeightRecord::getUserId, userId)
                .ge(startDate != null, WeightRecord::getRecordDate, startDate)
                .le(endDate != null, WeightRecord::getRecordDate, endDate)
                .orderByDesc(WeightRecord::getRecordDate)
                .orderByDesc(WeightRecord::getId);
        if (limit != null && limit > 0) {
            query.last(SqlLimit.bounded(limit, WEIGHT_LIST_LIMIT_MAX));
        }
        List<WeightRecord> records = weightRecordMapper.selectList(query);

        boolean plateau = plateauDetector.isPlateau(userId);
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
     * 体重趋势：按窗口取「每日末值」（时间序）并计算体重差（下沉 WeightAggregateService）。
     *
     * @param userId 当前用户ID
     * @param range  窗口 7/30/60/90/365/all；非法或空回退默认 30
     * @return 趋势（不足 2 个不同日时 points/delta 为 null）
     */
    public WeightTrendVO trend(Long userId, String range) {
        return weightAggregateService.trend(userId, range);
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
     * 查询调碳日志（按时间倒序，最近 100 条；下沉 PlateauDetector）
     *
     * @param userId 当前用户ID
     * @return 日志列表
     */
    public List<AdjustLogVO> listLogs(Long userId) {
        return plateauDetector.listLogs(userId);
    }
}
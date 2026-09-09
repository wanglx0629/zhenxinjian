package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.ActivityLevelEnum;
import cn.zhenxinjian.common.enums.DeficitOptionEnum;
import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.BodyProfileSaveDTO;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserBodyHistory;
import cn.zhenxinjian.domain.vo.BodyProfileVO;
import cn.zhenxinjian.mapper.UserBodyHistoryMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 身体档案服务（录入/修改一体，幂等覆盖；同事务归档旧值 + 重算快照）
 * 作者: wanglx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BodyProfileService {

    /** 目标体重容差：允许目标体重 ≤ 当前体重 + 0.1kg（浮点录入容错） */
    private static final double TARGET_WEIGHT_TOLERANCE = 0.1;

    /** 脂肪系数可选值 */
    private static final Set<Double> CFC_OPTIONS = Set.of(0.8, 1.0);

    /** 默认脂肪系数 */
    private static final double DEFAULT_CFC = 0.8;

    private final UserBodyMapper userBodyMapper;

    private final UserBodyHistoryMapper userBodyHistoryMapper;

    private final BodyCalcService bodyCalcService;

    /**
     * 保存身体档案（录入/修改一体）：交叉校验 → 计算 → 归档旧值 + 覆盖当前值（同事务）
     *
     * @param userId 当前用户ID
     * @param dto    档案入参
     * @return 保存后的档案与计算结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BodyProfileVO save(Long userId, BodyProfileSaveDTO dto) {
        // 交叉校验：目标体重 ≤ 当前体重 + 0.1（容差）
        if (dto.getTargetWeight() > dto.getWeight() + TARGET_WEIGHT_TOLERANCE) {
            throw new BusinessException(CommonConstant.BODY_TARGET_WEIGHT_INVALID_CODE,
                    ExceptionConstant.BODY_TARGET_WEIGHT_INVALID);
        }
        // 缺口档位（缺省默认 200；非法值拒绝）
        DeficitOptionEnum deficit = bodyCalcService.resolveDeficit(dto.getDeficit());
        if (deficit == null) {
            throw new BusinessException(CommonConstant.BODY_DEFICIT_INVALID_CODE,
                    ExceptionConstant.BODY_DEFICIT_INVALID);
        }
        // 脂肪系数（缺省默认 0.8；非法值拒绝）
        double cfc = dto.getCfc() == null ? DEFAULT_CFC : dto.getCfc();
        if (!CFC_OPTIONS.contains(cfc)) {
            throw new BusinessException(CommonConstant.BODY_CFC_INVALID_CODE,
                    ExceptionConstant.BODY_CFC_INVALID);
        }
        GenderEnum gender = GenderEnum.of(dto.getGender());
        ActivityLevelEnum level = ActivityLevelEnum.of(dto.getActivityLevel());

        BodyCalcService.CalcResult r = bodyCalcService.compute(
                gender, dto.getAge(), dto.getHeight(), dto.getWeight(),
                level.getFactor(), deficit.getCode());

        UserBody existing = selectActive(userId);
        if (existing == null) {
            UserBody fresh = new UserBody();
            applyProfile(fresh, userId, dto, level, deficit, cfc, r);
            fresh.setCreateBy(operator(userId));
            try {
                userBodyMapper.insert(fresh);
            } catch (DuplicateKeyException e) {
                // 并发首次录入：活跃唯一约束兜底，转为覆盖已有行，不抛 500
                log.warn("[BodyProfile] 并发录入转覆盖: userId={}", userId);
                UserBody concurrent = selectActive(userId);
                if (concurrent == null) {
                    throw e;
                }
                archiveAndOverwrite(concurrent, dto, level, deficit, cfc, r, userId);
            }
        } else {
            archiveAndOverwrite(existing, dto, level, deficit, cfc, r, userId);
        }
        return getCurrent(userId);
    }

    /**
     * 查询当前档案与计算结果快照；未录入返回空态（recorded=false）
     *
     * @param userId 当前用户ID
     * @return 档案 VO（含风险标记与免责声明）
     */
    public BodyProfileVO getCurrent(Long userId) {
        BodyProfileVO vo = new BodyProfileVO();
        vo.setDisclaimer(CommonConstant.HEALTH_DISCLAIMER);
        UserBody ub = selectActive(userId);
        if (ub == null) {
            vo.setRecorded(false);
            return vo;
        }
        BeanUtils.copyProperties(ub, vo);
        vo.setRecorded(true);
        vo.setLowKcalRisk(bodyCalcService.isLowKcalRisk(GenderEnum.of(ub.getGender()), ub.getTargetKcal()));
        return vo;
    }

    /** 查当前活跃档案（逻辑删除由 MyBatis-Plus 全局过滤） */
    private UserBody selectActive(Long userId) {
        return userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
    }

    /** 归档旧值入 history 并用新值覆盖当前行（同一事务内由调用方保证） */
    private void archiveAndOverwrite(UserBody existing, BodyProfileSaveDTO dto,
                                     ActivityLevelEnum level, DeficitOptionEnum deficit,
                                     double cfc, BodyCalcService.CalcResult r, Long userId) {
        UserBodyHistory history = new UserBodyHistory();
        BeanUtils.copyProperties(existing, history);
        history.setId(null);
        history.setUserBodyId(existing.getId());
        history.setArchivedAt(LocalDateTime.now());
        userBodyHistoryMapper.insert(history);

        applyProfile(existing, userId, dto, level, deficit, cfc, r);
        existing.setUpdateBy(operator(userId));
        // strictUpdateFill 仅在字段为空时填充，查询出的旧值需显式刷新
        existing.setUpdateTime(LocalDateTime.now());
        userBodyMapper.updateById(existing);
    }

    /** 将入参与计算结果写入档案行（新建/覆盖共用） */
    private void applyProfile(UserBody ub, Long userId, BodyProfileSaveDTO dto,
                              ActivityLevelEnum level, DeficitOptionEnum deficit,
                              double cfc, BodyCalcService.CalcResult r) {
        ub.setUserId(userId);
        ub.setGender(dto.getGender());
        ub.setAge(dto.getAge());
        ub.setHeight(dto.getHeight());
        ub.setWeight(dto.getWeight());
        ub.setTargetWeight(dto.getTargetWeight());
        ub.setActivityLevel(level.getCode());
        ub.setActivityFactor(level.getFactor());
        ub.setDeficit(deficit.getCode());
        ub.setCfc(cfc);
        ub.setBmr(r.bmr());
        ub.setTdee(r.tdee());
        ub.setTargetKcal(r.targetKcal());
        ub.setTargetCarb(r.carb());
        ub.setTargetProtein(r.protein());
        ub.setTargetFat(r.fat());
    }

    /** 操作者标识（审计列） */
    private String operator(Long userId) {
        return "user:" + userId;
    }
}

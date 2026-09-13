package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.domain.dto.MenstrualSaveDTO;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserMenstrual;
import cn.zhenxinjian.domain.vo.MenstrualVO;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.UserMenstrualMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 经期管理服务（每用户一条活跃记录；男性不适用；四阶段由 MenstrualCalcService 判定）
 * 作者: wanglx
 *
 * 口径（spec body/menstrual）：L 21-35 默认 28，D 3-10 默认 5；
 * 开启须填末次月经起始日且不得为未来日期；男性 applicable=false 空态不报错
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenstrualService {

    /** 周期长度 L 边界 */
    private static final int CYCLE_LEN_MIN = 21;
    private static final int CYCLE_LEN_MAX = 35;

    /** 经期天数 D 边界 */
    private static final int PERIOD_DAYS_MIN = 3;
    private static final int PERIOD_DAYS_MAX = 10;

    /** 默认周期长度 */
    private static final int DEFAULT_CYCLE_LEN = 28;

    /** 默认经期天数 */
    private static final int DEFAULT_PERIOD_DAYS = 5;

    private final UserMenstrualMapper userMenstrualMapper;

    private final UserBodyMapper userBodyMapper;

    private final MenstrualCalcService menstrualCalcService;

    /**
     * 查询经期设置（男性 applicable=false；开启后附当前阶段与上浮值）
     *
     * @param userId 当前用户ID
     * @return 经期设置视图
     */
    public MenstrualVO get(Long userId) {
        GenderEnum gender = resolveGender(userId);
        boolean applicable = gender == GenderEnum.FEMALE;
        if (!applicable) {
            MenstrualVO vo = new MenstrualVO();
            vo.setApplicable(false);
            return vo;
        }
        UserMenstrual entity = selectActive(userId);
        return toVO(entity);
    }

    /**
     * 保存经期设置（开启时校验 L/D 区间与起始日；越界 40801）
     *
     * @param userId 当前用户ID
     * @param dto    保存入参
     * @return 保存后的设置视图
     */
    @Transactional(rollbackFor = Exception.class)
    public MenstrualVO save(Long userId, MenstrualSaveDTO dto) {
        if (resolveGender(userId) != GenderEnum.FEMALE) {
            throw new BusinessException(CommonConstant.MENSTRUAL_SETTING_INVALID_CODE,
                    ExceptionConstant.MENSTRUAL_SETTING_INVALID);
        }
        int cycleLen = dto.getCycleLen() == null ? DEFAULT_CYCLE_LEN : dto.getCycleLen();
        int periodDays = dto.getPeriodDays() == null ? DEFAULT_PERIOD_DAYS : dto.getPeriodDays();
        if (cycleLen < CYCLE_LEN_MIN || cycleLen > CYCLE_LEN_MAX
                || periodDays < PERIOD_DAYS_MIN || periodDays > PERIOD_DAYS_MAX) {
            throw new BusinessException(CommonConstant.MENSTRUAL_SETTING_INVALID_CODE,
                    ExceptionConstant.MENSTRUAL_SETTING_INVALID);
        }
        int enabled = dto.getEnabled() == null ? 0 : dto.getEnabled();
        LocalDate startDate = dto.getPeriodStartDate();
        if (enabled == 1 && (startDate == null || startDate.isAfter(LocalDate.now()))) {
            throw new BusinessException(CommonConstant.MENSTRUAL_SETTING_INVALID_CODE,
                    ExceptionConstant.MENSTRUAL_SETTING_INVALID);
        }

        UserMenstrual entity = selectActive(userId);
        if (entity == null) {
            entity = new UserMenstrual();
            entity.setUserId(userId);
            entity.setCreateBy(Operators.user(userId));
            entity.setStatus(1);
        }
        entity.setEnabled(enabled);
        entity.setPeriodStartDate(startDate);
        entity.setCycleLen(cycleLen);
        entity.setPeriodDays(periodDays);
        entity.setUpdateBy(Operators.user(userId));
        if (entity.getId() == null) {
            userMenstrualMapper.insert(entity);
        } else {
            userMenstrualMapper.updateById(entity);
        }
        log.info("[MenstrualService] 经期设置保存: userId={}, enabled={}, L={}, D={}",
                userId, enabled, cycleLen, periodDays);
        return toVO(entity);
    }

    /** 查当前活跃经期设置（逻辑删除由 MyBatis-Plus 全局过滤） */
    private UserMenstrual selectActive(Long userId) {
        return userMenstrualMapper.selectOne(
                Wrappers.<UserMenstrual>lambdaQuery().eq(UserMenstrual::getUserId, userId));
    }

    /** 从身体档案取当前用户性别（未建档返回 null） */
    private GenderEnum resolveGender(Long userId) {
        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        return body == null ? null : GenderEnum.of(body.getGender());
    }

    /** 实体转视图（附当前阶段与上浮值） */
    private MenstrualVO toVO(UserMenstrual entity) {
        MenstrualVO vo = new MenstrualVO();
        vo.setApplicable(true);
        if (entity == null) {
            vo.setEnabled(0);
            vo.setCycleLen(DEFAULT_CYCLE_LEN);
            vo.setPeriodDays(DEFAULT_PERIOD_DAYS);
            return vo;
        }
        vo.setEnabled(entity.getEnabled());
        vo.setPeriodStartDate(entity.getPeriodStartDate());
        vo.setCycleLen(entity.getCycleLen());
        vo.setPeriodDays(entity.getPeriodDays());

        int enabled = entity.getEnabled() == null ? 0 : entity.getEnabled();
        MenstrualCalcService.PhaseResult phase = menstrualCalcService.resolvePhase(
                GenderEnum.FEMALE, enabled == 1, entity.getPeriodStartDate(),
                entity.getCycleLen(), entity.getPeriodDays(), LocalDate.now());
        if (phase != null) {
            vo.setPhaseKey(phase.phase().getKey());
            vo.setPhaseName(phase.phase().getDesc());
            vo.setDayIdx(phase.dayIdx());
            vo.setCarbUplift(phase.carbUplift());
            vo.setKcalUplift(phase.kcalUplift());
        }
        return vo;
    }
}
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.enums.MenstrualPhaseEnum;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserMenstrual;
import cn.zhenxinjian.domain.vo.Taper532VO;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.UserMenstrualMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 532 碳水渐降推进服务（四阶段计划卡 + 今日目标公式化分发）
 * 作者: wanglx
 *
 * 口径（spec body/taper-532 + design D2/D4）：
 * 计划卡为展示口径：阶段 1/2/3 无固定碳热调整，阶段 4 −20g/−80kcal 平台触发式；
 * 今日目标 = 基线快照 + 平台下调（−20/−80，若下调态）+ 经期上浮（若开启且当前阶段非卵泡）；
 * 蛋白脂肪 MUST 保持基线不变；克数 1 位小数、热量取整，BigDecimal 计算防漂移（G5）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Taper532Service {

    /** 平台下调碳水 g */
    private static final BigDecimal ADJUST_CARB = new BigDecimal("20");

    /** 平台下调热量 kcal */
    private static final BigDecimal ADJUST_KCAL = new BigDecimal("80");

    private final UserBodyMapper userBodyMapper;

    private final UserMenstrualMapper userMenstrualMapper;

    private final MenstrualCalcService menstrualCalcService;

    /**
     * 四阶段计划卡 + 今日目标（未建档空态：stages 空列表、today 为 null）
     *
     * @param userId 当前用户ID
     * @return 计划卡视图
     */
    public Taper532VO plan(Long userId) {
        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        if (body == null) {
            Taper532VO vo = new Taper532VO();
            vo.setStages(List.of());
            vo.setToday(null);
            return vo;
        }

        MenstrualCalcService.PhaseResult phase = resolvePhase(body);

        Taper532VO vo = new Taper532VO();
        vo.setStages(buildStages(phase));
        vo.setToday(buildToday(body, phase));
        return vo;
    }

    /**
     * 今日 532 目标公式（供 DietRecordService.summary 分发复用）
     *
     * @param userId 当前用户ID
     * @return 今日目标；未建档返回 null
     */
    public Taper532VO.TodayTarget todayTarget(Long userId) {
        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        if (body == null) {
            return null;
        }
        return buildToday(body, resolvePhase(body));
    }

    /** 四阶段卡构建（阶段 2 叠加经期说明） */
    private List<Taper532VO.StageItem> buildStages(MenstrualCalcService.PhaseResult phase) {
        List<Taper532VO.StageItem> stages = new ArrayList<>(4);
        stages.add(stage(1, "阶段 1 · 月初适应期",
                "维持标准 50%/30%/20% 固定配比，让身体适应减脂饮食，不做任何降碳调整。", 0, 0, false));
        stages.add(stage(2, "阶段 2 · 稳步减脂期（经期适配）",
                phase == null
                        ? "结合女性经期周期，按所处阶段自动上浮碳水与热量，降低减脂压力，规避水肿、乏力、暴食。"
                        : "结合女性经期周期：当前处于「" + phase.phase().getDesc() + "」第 " + phase.dayIdx() + " 天，碳水 +"
                        + phase.carbUplift() + "g / 热量 +" + phase.kcalUplift() + "kcal，蛋白脂肪不变。",
                0, 0, false));
        stages.add(stage(3, "阶段 3 · 经后高效期",
                "经期结束后稳步梯度小幅降碳，温和放大热量缺口，高效减脂。", 0, 0, false));
        stages.add(stage(4, "阶段 4 · 平台突破期",
                "针对体重停滞用户触发固定微调：碳水 −20g、总热量 −80 kcal，蛋白脂肪保持不变，平稳突破平台。",
                -20, -80, true));
        return stages;
    }

    /** 今日目标公式：基线 + 下调 + 经期上浮；蛋白脂肪不变 */
    private Taper532VO.TodayTarget buildToday(UserBody body, MenstrualCalcService.PhaseResult phase) {
        boolean adjusted = body.getIsAdjusted() != null && body.getIsAdjusted() == 1;
        int carbUplift = phase == null ? 0 : phase.carbUplift();
        int kcalUplift = phase == null ? 0 : phase.kcalUplift();

        BigDecimal carb = BigDecimal.valueOf(body.getTargetCarb() == null ? 0 : body.getTargetCarb())
                .add(adjusted ? ADJUST_CARB.negate() : BigDecimal.ZERO)
                .add(BigDecimal.valueOf(carbUplift));
        int kcal = BigDecimal.valueOf(body.getTargetKcal() == null ? 0 : body.getTargetKcal())
                .add(adjusted ? ADJUST_KCAL.negate() : BigDecimal.ZERO)
                .add(BigDecimal.valueOf(kcalUplift))
                .setScale(0, RoundingMode.HALF_UP).intValue();

        Taper532VO.TodayTarget today = new Taper532VO.TodayTarget();
        today.setCarb(round1(carb));
        today.setProtein(body.getTargetProtein());
        today.setFat(body.getTargetFat());
        today.setKcal(kcal);
        today.setAdjusted(adjusted);
        if (phase != null) {
            today.setPhaseKey(phase.phase().getKey());
            today.setPhaseName(phase.phase().getDesc());
        }
        return today;
    }

    /**
     * 解析当前经期阶段（仅女性且已开启时有效；供 DietRecordService 碳循环叠加复用）
     *
     * @param body 已查询到的当前档案
     * @return 阶段结果；非女性/未开启返回 null
     */
    public MenstrualCalcService.PhaseResult resolvePhase(UserBody body) {
        if (body.getGender() == null) {
            return null;
        }
        GenderEnum gender = GenderEnum.of(body.getGender());
        if (gender != GenderEnum.FEMALE) {
            return null;
        }
        UserMenstrual menstrual = userMenstrualMapper.selectOne(
                Wrappers.<UserMenstrual>lambdaQuery().eq(UserMenstrual::getUserId, body.getUserId()));
        if (menstrual == null) {
            return null;
        }
        int enabled = menstrual.getEnabled() == null ? 0 : menstrual.getEnabled();
        return menstrualCalcService.resolvePhase(GenderEnum.FEMALE, enabled == 1,
                menstrual.getPeriodStartDate(), menstrual.getCycleLen(), menstrual.getPeriodDays(),
                LocalDate.now());
    }

    /** 阶段项组装 */
    private Taper532VO.StageItem stage(int n, String name, String desc, int carbDelta,
                                       int kcalDelta, boolean plateauTriggered) {
        Taper532VO.StageItem item = new Taper532VO.StageItem();
        item.setN(n);
        item.setName(name);
        item.setDesc(desc);
        item.setCarbDelta(carbDelta);
        item.setKcalDelta(kcalDelta);
        item.setPlateauTriggered(plateauTriggered);
        return item;
    }

    /** 克数 1 位小数 HALF_UP */
    private static double round1(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.enums.MenstrualPhaseEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 经期四阶段计算引擎（无状态；后端唯一真源，口径与 calculator.ts.menstrualPhase 对拍）
 * 作者: wanglx
 *
 * 口径（spec body/menstrual）：
 * dayIdx = (today − startDate 整数天数 mod L) + 1（今天在起始日之前 → 按第 1 天）
 *   dayIdx ≤ D                 → 经期   menstrual
 *   D < dayIdx ≤ L−14          → 卵泡期 follicular
 *   L−14 < dayIdx ≤ L−12       → 排卵期 ovulation
 *   dayIdx > L−12              → 黄体期 luteal
 * 未开启 / 非女性 / 起始日缺失 → 空态（null）
 */
@Service
public class MenstrualCalcService {

    /**
     * 经期判定结果（阶段 + 上浮值；空态为 null）
     */
    public record PhaseResult(int dayIdx, MenstrualPhaseEnum phase) {

        public int carbUplift() {
            return phase.getCarbUplift();
        }

        public int kcalUplift() {
            return phase.getKcalUplift();
        }
    }

    /**
     * 判定当前所处经期阶段（跨模式通用）
     *
     * @param gender         性别（男 → 空态）
     * @param enabled        是否开启经期管理（未开启 → 空态）
     * @param startDate      末次月经起始日（空 → 空态；未来起始日按第 1 天）
     * @param cycleLen       周期长度 L
     * @param periodDays     经期天数 D
     * @param today          推算基准日（服务端 LocalDate.now()；测试可注入）
     * @return 阶段结果；空态返回 null
     */
    public PhaseResult resolvePhase(GenderEnum gender, boolean enabled, LocalDate startDate,
                                    int cycleLen, int periodDays, LocalDate today) {
        if (gender != GenderEnum.FEMALE || !enabled || startDate == null) {
            return null;
        }
        long diff = ChronoUnit.DAYS.between(startDate, today);
        if (diff < 0) {
            diff = 0;
        }
        int dayIdx = (int) (diff % cycleLen) + 1;

        MenstrualPhaseEnum phase;
        if (dayIdx <= periodDays) {
            phase = MenstrualPhaseEnum.MENSTRUAL;
        } else if (dayIdx <= cycleLen - 14) {
            phase = MenstrualPhaseEnum.FOLLICULAR;
        } else if (dayIdx <= cycleLen - 12) {
            phase = MenstrualPhaseEnum.OVULATION;
        } else {
            phase = MenstrualPhaseEnum.LUTEAL;
        }
        return new PhaseResult(dayIdx, phase);
    }
}
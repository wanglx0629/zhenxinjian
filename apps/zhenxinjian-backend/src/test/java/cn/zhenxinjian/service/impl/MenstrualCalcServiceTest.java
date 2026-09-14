package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.enums.MenstrualPhaseEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 经期四阶段计算引擎单元测试（金标对拍）
 * 作者: wanglx
 */
class MenstrualCalcServiceTest {

    private final MenstrualCalcService service = new MenstrualCalcService();

    /** 金标：周期 28 天、经期 5 天、第 3 天 → 经期 menstrual */
    @Test
    void resolvePhase_day3inPeriod_menstrual() {
        LocalDate start = LocalDate.now().minusDays(2);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.MENSTRUAL, r.phase());
        assertEquals(3, r.dayIdx());
    }

    /** 金标：周期 28 天、经期 5 天、第 8 天 → 卵泡期 follicular */
    @Test
    void resolvePhase_day8_follicular() {
        LocalDate start = LocalDate.now().minusDays(7);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.FOLLICULAR, r.phase());
        assertEquals(8, r.dayIdx());
    }

    /** 金标：周期 28 天、经期 5 天、第 15 天 → 排卵期 ovulation */
    @Test
    void resolvePhase_day15_ovulation() {
        LocalDate start = LocalDate.now().minusDays(14);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.OVULATION, r.phase());
        assertEquals(15, r.dayIdx());
    }

    /** 金标：周期 28 天、经期 5 天、第 17 天 → 黄体期 luteal */
    @Test
    void resolvePhase_day17_luteal() {
        LocalDate start = LocalDate.now().minusDays(16);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.LUTEAL, r.phase());
        assertEquals(17, r.dayIdx());
    }

    /** 场景：男性 → 空态 */
    @Test
    void resolvePhase_male_returnsNull() {
        var r = service.resolvePhase(GenderEnum.MALE, true, LocalDate.now(), 28, 5, LocalDate.now());
        assertNull(r);
    }

    /** 场景：未开启经期管理 → 空态 */
    @Test
    void resolvePhase_notEnabled_returnsNull() {
        var r = service.resolvePhase(GenderEnum.FEMALE, false, LocalDate.now(), 28, 5, LocalDate.now());
        assertNull(r);
    }

    /** 场景：起始日缺失 → 空态 */
    @Test
    void resolvePhase_nullStartDate_returnsNull() {
        var r = service.resolvePhase(GenderEnum.FEMALE, true, null, 28, 5, LocalDate.now());
        assertNull(r);
    }

    /** 场景：起始日在未来 → 按 dayIdx=1 处理（经期首日） */
    @Test
    void resolvePhase_futureStartDate_treatedAsDay1() {
        LocalDate future = LocalDate.now().plusDays(5);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, future, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(1, r.dayIdx());
        assertEquals(MenstrualPhaseEnum.MENSTRUAL, r.phase());
    }

    /** 场景：周期边界 21 天验证 dayIdx 计算 */
    @Test
    void resolvePhase_cycleLen21_rollingWorks() {
        LocalDate start = LocalDate.now().minusDays(21); // 正好一个周期前，dayIdx = 1
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 21, 3, LocalDate.now());

        assertNotNull(r);
        assertEquals(1, r.dayIdx());
    }

    /** 场景：经期天数边界 D=5，第 5 天为经期 */
    @Test
    void resolvePhase_day5endOfPeriod_menstrual() {
        LocalDate start = LocalDate.now().minusDays(4);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.MENSTRUAL, r.phase());
    }

    /** 场景：经期天数边界 D=5，第 6 天进入卵泡期 */
    @Test
    void resolvePhase_day6AfterPeriod_follicular() {
        LocalDate start = LocalDate.now().minusDays(5);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.FOLLICULAR, r.phase());
    }

    /** 场景：carbUplift / kcalUplift 取自 MenstrualPhaseEnum */
    @Test
    void phaseResult_uplifts_fromEnum() {
        LocalDate start = LocalDate.now().minusDays(2);
        var r = service.resolvePhase(GenderEnum.FEMALE, true, start, 28, 5, LocalDate.now());

        assertNotNull(r);
        assertEquals(MenstrualPhaseEnum.MENSTRUAL.getCarbUplift(), r.carbUplift());
        assertEquals(MenstrualPhaseEnum.MENSTRUAL.getKcalUplift(), r.kcalUplift());
    }
}
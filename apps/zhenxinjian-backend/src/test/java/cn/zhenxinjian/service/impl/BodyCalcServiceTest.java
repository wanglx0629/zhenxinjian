package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.ActivityLevelEnum;
import cn.zhenxinjian.common.enums.DeficitOptionEnum;
import cn.zhenxinjian.common.enums.GenderEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BodyCalcService 单元测试（移植前端 48 项对拍验算基准核心用例）
 * 验算基准（业务规则速查 §验算）：女 30岁/162cm/55kg/1.375/缺口200
 * → BMR 1252、TDEE 1721、基准热量 1521、碳水 190.1 / 蛋白 114.1 / 脂肪 33.8
 * 作者: wanglx
 */
class BodyCalcServiceTest {

    private final BodyCalcService service = new BodyCalcService();

    @Test
    void femaleBaselineShouldMatchGoldenSample() {
        BodyCalcService.CalcResult r = service.compute(
                GenderEnum.FEMALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), DeficitOptionEnum.GENTLE.getCode());

        assertEquals(1252, r.bmr(), "BMR 验算基准");
        assertEquals(1721, r.tdee(), "TDEE 验算基准");
        assertEquals(1521, r.targetKcal(), "基准热量验算基准");
        assertEquals(190.1, r.carb(), 0.1, "碳水验算基准");
        assertEquals(114.1, r.protein(), 0.1, "蛋白验算基准");
        assertEquals(33.8, r.fat(), 0.1, "脂肪验算基准");
        assertFalse(r.lowKcalRisk(), "1521 不应触发女 1200 下限");
    }

    @Test
    void maleBmrShouldBe166HigherThanFemale() {
        BodyCalcService.CalcResult male = service.compute(
                GenderEnum.MALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), DeficitOptionEnum.GENTLE.getCode());
        BodyCalcService.CalcResult female = service.compute(
                GenderEnum.FEMALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), DeficitOptionEnum.GENTLE.getCode());

        assertEquals(166, male.bmr() - female.bmr(), "同参男女 BMR 差值 = +5 −(−161)");
    }

    @Test
    void deficitShouldScaleMacrosProportionally() {
        BodyCalcService.CalcResult gentle = service.compute(
                GenderEnum.FEMALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), 200);
        BodyCalcService.CalcResult aggressive = service.compute(
                GenderEnum.FEMALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), 500);

        assertEquals(1721 - 200, gentle.targetKcal());
        assertEquals(1721 - 500, aggressive.targetKcal());
        assertTrue(aggressive.carb() < gentle.carb(), "缺口加大碳水等比收缩");
        assertTrue(aggressive.protein() < gentle.protein(), "缺口加大蛋白等比收缩");
        assertTrue(aggressive.fat() < gentle.fat(), "缺口加大脂肪等比收缩");
    }

    @Test
    void customDeficitShouldApply() {
        BodyCalcService.CalcResult r = service.compute(
                GenderEnum.FEMALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), 400);
        assertEquals(1321, r.targetKcal(), "TDEE 1721 − 400 = 1321");
    }

    @Test
    void lowKcalRiskShouldTriggerBelowThreshold() {
        assertTrue(service.isLowKcalRisk(GenderEnum.FEMALE, 1150), "女 <1200 触发风险");
        assertFalse(service.isLowKcalRisk(GenderEnum.FEMALE, 1200), "女 =1200 不触发");
        assertTrue(service.isLowKcalRisk(GenderEnum.MALE, 1499), "男 <1500 触发风险");
        assertFalse(service.isLowKcalRisk(GenderEnum.MALE, 1600), "男 1600 不触发");
    }

    @Test
    void resolveDeficitShouldDefaultAndValidate() {
        assertEquals(DeficitOptionEnum.GENTLE, service.resolveDeficit(null), "缺省默认 200");
        assertEquals(DeficitOptionEnum.AGGRESSIVE, service.resolveDeficit(500));
        assertNull(service.resolveDeficit(250), "非法档位返回 null 由上层拒绝");
    }

    @Test
    void roundingShouldKeepOneDecimalForMacros() {
        // 基准热量 1521 → 190.125 / 114.075 / 33.8 的 HALF_UP 表现
        BodyCalcService.CalcResult r = service.compute(
                GenderEnum.FEMALE, 30, 162, 55,
                ActivityLevelEnum.LIGHT.getFactor(), 200);
        assertEquals(190.1, r.carb(), 0.0001);
        assertEquals(114.1, r.protein(), 0.0001);
        assertEquals(33.8, r.fat(), 0.0001);
    }
}

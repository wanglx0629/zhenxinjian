package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.DietModeEnum;
import cn.zhenxinjian.common.enums.DietRecordSourceEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.enums.MealTypeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.DietRecordCreateDTO;
import cn.zhenxinjian.domain.dto.DietRecordUpdateDTO;
import cn.zhenxinjian.domain.po.CarbCycleDay;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.vo.DietDayVO;
import cn.zhenxinjian.domain.vo.DietRecordVO;
import cn.zhenxinjian.domain.vo.DietSummaryVO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.FoodMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 饮食记录服务（三类来源新增/编辑/软删 + 按日分组查询 + 当日累计对照目标快照）
 * 作者: wanglx
 *
 * 口径: 实际摄入 = 每 100g 快照值 × 克数 ÷ 100，克数 1 位小数、热量取整（BigDecimal 计算防漂移）；
 * 编辑食物来源记录按快照重算，不回查食物表（design D3）；
 * 手动输入能量守恒 |碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%（不变量 I11）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietRecordService {

    /** 份量克数上限 */
    private static final BigDecimal AMOUNT_MAX = new BigDecimal("5000");

    /** 手动输入单项宏量上限 g */
    private static final BigDecimal MANUAL_MACRO_MAX = new BigDecimal("2000");

    /** 手动输入能量上限 kcal */
    private static final int MANUAL_KCAL_MAX = 20000;

    /** 能量守恒允许偏差比例（10%） */
    private static final BigDecimal KCAL_TOLERANCE_RATIO = new BigDecimal("0.10");

    /** 守恒计算系数：碳水/蛋白 4、脂肪 9 */
    private static final BigDecimal CARB_PROTEIN_FACTOR = new BigDecimal("4");
    private static final BigDecimal FAT_FACTOR = new BigDecimal("9");

    /** 每 100g 换算基数 */
    private static final BigDecimal PER_100G = new BigDecimal("100");

    private final DietRecordMapper dietRecordMapper;

    private final FoodMapper foodMapper;

    private final UserBodyMapper userBodyMapper;

    private final CyclePlanService cyclePlanService;

    /**
     * 新增饮食记录
     *
     * @param userId 当前用户ID
     * @param dto    入参（Bean Validation 已做结构校验，此处为业务兜底）
     * @return 保存后的记录
     */
    public DietRecordVO create(Long userId, DietRecordCreateDTO dto) {
        MealTypeEnum meal = resolveMeal(dto.getMealType());
        DietRecordSourceEnum source = resolveSource(dto.getSource());
        LocalDate date = resolveDate(dto.getRecordDate());

        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(date);
        record.setMealType(meal.getCode());
        record.setRemark(dto.getRemark());
        record.setCreateBy(operator(userId));

        if (DietRecordSourceEnum.MANUAL == source) {
            applyManual(record, dto.getName(), dto.getCarb(), dto.getProtein(), dto.getFat(), dto.getKcal());
        } else {
            applyFoodSource(userId, record, dto.getFoodId(), dto.getAmountG());
        }
        dietRecordMapper.insert(record);
        return toVO(dietRecordMapper.selectById(record.getId()));
    }

    /**
     * 编辑饮食记录（食物来源仅餐别/份量/备注，按快照重算；手动输入可改名称与三宏热量）
     *
     * @param userId 当前用户ID
     * @param id     记录ID
     * @param dto    入参
     * @return 更新后的记录
     */
    public DietRecordVO update(Long userId, Long id, DietRecordUpdateDTO dto) {
        DietRecord record = selectOwn(userId, id);
        MealTypeEnum meal = resolveMeal(dto.getMealType());
        record.setMealType(meal.getCode());
        record.setRemark(dto.getRemark());
        record.setUpdateBy(operator(userId));

        if (DietRecordSourceEnum.MANUAL.getCode().equals(record.getSource())) {
            applyManual(record, dto.getName(), dto.getCarb(), dto.getProtein(), dto.getFat(), dto.getKcal());
        } else {
            BigDecimal amount = validateAmount(dto.getAmountG());
            record.setAmountG(amount.doubleValue());
            applyIntakeFromSnapshot(record, amount);
        }
        dietRecordMapper.updateById(record);
        return toVO(dietRecordMapper.selectById(record.getId()));
    }

    /**
     * 删除饮食记录（逻辑删除，越权/不存在统一 40504）
     *
     * @param userId 当前用户ID
     * @param id     记录ID
     */
    public void remove(Long userId, Long id) {
        DietRecord record = selectOwn(userId, id);
        dietRecordMapper.deleteById(record.getId());
    }

    /**
     * 按日查询（餐别四组 + 小计 + 当日合计；空日返回空分组）
     *
     * @param userId 当前用户ID
     * @param date   查询日期（空默认当日；未来日期 40505）
     */
    public DietDayVO listByDate(Long userId, LocalDate date) {
        LocalDate queryDate = resolveDate(date);
        List<DietRecord> records = dietRecordMapper.selectList(
                Wrappers.<DietRecord>lambdaQuery()
                        .eq(DietRecord::getUserId, userId)
                        .eq(DietRecord::getRecordDate, queryDate)
                        .orderByAsc(DietRecord::getCreateTime)
                        .orderByAsc(DietRecord::getId));

        DietDayVO vo = new DietDayVO();
        vo.setDate(queryDate);
        List<DietDayVO.MealGroup> groups = new ArrayList<>();
        double totalCarb = 0;
        double totalProtein = 0;
        double totalFat = 0;
        int totalKcal = 0;
        for (MealTypeEnum meal : MealTypeEnum.values()) {
            List<DietRecord> mealRecords = records.stream()
                    .filter(r -> meal.getCode().equals(r.getMealType()))
                    .collect(Collectors.toList());
            DietDayVO.MealGroup group = new DietDayVO.MealGroup();
            group.setMealType(meal.getCode());
            group.setMealName(meal.getDesc());
            group.setRecords(mealRecords.stream().map(this::toVO).collect(Collectors.toList()));
            double carb = 0;
            double protein = 0;
            double fat = 0;
            int kcal = 0;
            for (DietRecord r : mealRecords) {
                carb += r.getCarbG();
                protein += r.getProteinG();
                fat += r.getFatG();
                kcal += r.getKcal();
            }
            group.setCarb(round1(carb));
            group.setProtein(round1(protein));
            group.setFat(round1(fat));
            group.setKcal(kcal);
            groups.add(group);
            totalCarb += carb;
            totalProtein += protein;
            totalFat += fat;
            totalKcal += kcal;
        }
        vo.setMeals(groups);
        vo.setTotalCarb(round1(totalCarb));
        vo.setTotalProtein(round1(totalProtein));
        vo.setTotalFat(round1(totalFat));
        vo.setTotalKcal(totalKcal);
        return vo;
    }

    /**
     * 当日累计与目标进度（未建档空态：recorded=false，目标与达成率为空）
     * 目标来源按模式分发（carb-cycle design §4.1）：532 取档案快照；碳循环取进行中周期当日日型目标，
     * 无周期/当日不在周期内同未建档空态口径（D3 复用 recorded=false）
     *
     * @param userId 当前用户ID
     * @param date   查询日期（空默认当日；未来日期 40505）
     */
    public DietSummaryVO summary(Long userId, LocalDate date) {
        LocalDate queryDate = resolveDate(date);
        Map<String, Object> sums = dietRecordMapper.selectMaps(
                new QueryWrapper<DietRecord>()
                        .select("COALESCE(SUM(carb_g),0) AS carb",
                                "COALESCE(SUM(protein_g),0) AS protein",
                                "COALESCE(SUM(fat_g),0) AS fat",
                                "COALESCE(SUM(kcal),0) AS kcal")
                        .eq("user_id", userId)
                        .eq("record_date", queryDate))
                .get(0);

        DietSummaryVO vo = new DietSummaryVO();
        vo.setDate(queryDate);
        double carb = toDouble(sums.get("carb"));
        double protein = toDouble(sums.get("protein"));
        double fat = toDouble(sums.get("fat"));
        int kcal = (int) Math.round(toDouble(sums.get("kcal")));
        vo.setCarbActual(round1(carb));
        vo.setProteinActual(round1(protein));
        vo.setFatActual(round1(fat));
        vo.setKcalActual(kcal);

        UserBody body = userBodyMapper.selectOne(
                Wrappers.<UserBody>lambdaQuery().eq(UserBody::getUserId, userId));
        if (body == null) {
            vo.setRecorded(false);
            vo.setMode(DietModeEnum.TAPER_532.getCode());
            return vo;
        }
        Integer mode = body.getMode() == null ? DietModeEnum.TAPER_532.getCode() : body.getMode();
        vo.setMode(mode);

        if (DietModeEnum.CARB_CYCLE.getCode().equals(mode)) {
            CarbCycleDay day = cyclePlanService.findActiveDay(userId, queryDate);
            if (day == null) {
                vo.setRecorded(false);
                return vo;
            }
            vo.setRecorded(true);
            Double carbTarget = day.getCarbG() == null ? null : day.getCarbG().doubleValue();
            Double proteinTarget = day.getProteinG() == null ? null : day.getProteinG().doubleValue();
            Double fatTarget = day.getFatG() == null ? null : day.getFatG().doubleValue();
            vo.setCarbTarget(carbTarget);
            vo.setProteinTarget(proteinTarget);
            vo.setFatTarget(fatTarget);
            vo.setKcalTarget(day.getKcal());
            vo.setCarbRate(rate(carb, carbTarget));
            vo.setProteinRate(rate(protein, proteinTarget));
            vo.setFatRate(rate(fat, fatTarget));
            vo.setKcalRate(rate(kcal, day.getKcal() == null ? null : day.getKcal().doubleValue()));
            return vo;
        }

        vo.setRecorded(true);
        vo.setCarbTarget(body.getTargetCarb());
        vo.setProteinTarget(body.getTargetProtein());
        vo.setFatTarget(body.getTargetFat());
        vo.setKcalTarget(body.getTargetKcal());
        vo.setCarbRate(rate(carb, body.getTargetCarb()));
        vo.setProteinRate(rate(protein, body.getTargetProtein()));
        vo.setFatRate(rate(fat, body.getTargetFat()));
        vo.setKcalRate(rate(kcal, body.getTargetKcal() == null ? null : body.getTargetKcal().doubleValue()));
        return vo;
    }

    /** 食物来源落库：查食物→归属校验→按克数换算→落快照+摄入（40506/40501） */
    private void applyFoodSource(Long userId, DietRecord record, Long foodId, BigDecimal amountG) {
        if (foodId == null) {
            throw new BusinessException(CommonConstant.DIET_FOOD_INVALID_CODE,
                    ExceptionConstant.DIET_FOOD_INVALID);
        }
        Food food = foodMapper.selectById(foodId);
        if (food == null) {
            throw new BusinessException(CommonConstant.DIET_FOOD_INVALID_CODE,
                    ExceptionConstant.DIET_FOOD_INVALID);
        }
        // 来源归属：内置全局可用；自定义仅本人可用（他人自定义按不存在处理，不泄露）
        if (FoodSourceEnum.BUILT_IN.getCode().equals(food.getSource())) {
            record.setSource(DietRecordSourceEnum.BUILT_IN_FOOD.getCode());
        } else if (userId.equals(food.getUserId())) {
            record.setSource(DietRecordSourceEnum.CUSTOM_FOOD.getCode());
        } else {
            throw new BusinessException(CommonConstant.DIET_FOOD_INVALID_CODE,
                    ExceptionConstant.DIET_FOOD_INVALID);
        }
        BigDecimal amount = validateAmount(amountG);
        record.setFoodId(food.getId());
        record.setFoodName(food.getName());
        record.setCarb100g(food.getCarb());
        record.setProtein100g(food.getProtein());
        record.setFat100g(food.getFat());
        record.setKcal100g(food.getKcal());
        record.setAmountG(amount.doubleValue());
        applyIntakeFromSnapshot(record, amount);
    }

    /** 手动输入落库：必填齐全 + 区间 + 能量守恒 ±10%（40502/40503） */
    private void applyManual(DietRecord record, String name, BigDecimal carb, BigDecimal protein,
                             BigDecimal fat, Integer kcal) {
        if (name == null || name.trim().isEmpty() || carb == null || protein == null
                || fat == null || kcal == null) {
            throw new BusinessException(CommonConstant.DIET_MACRO_INVALID_CODE,
                    ExceptionConstant.DIET_MACRO_INVALID);
        }
        if (carb.signum() < 0 || protein.signum() < 0 || fat.signum() < 0
                || carb.compareTo(MANUAL_MACRO_MAX) > 0
                || protein.compareTo(MANUAL_MACRO_MAX) > 0
                || fat.compareTo(MANUAL_MACRO_MAX) > 0
                || kcal < 0 || kcal > MANUAL_KCAL_MAX) {
            throw new BusinessException(CommonConstant.DIET_MACRO_INVALID_CODE,
                    ExceptionConstant.DIET_MACRO_INVALID);
        }
        BigDecimal expected = carb.multiply(CARB_PROTEIN_FACTOR)
                .add(protein.multiply(CARB_PROTEIN_FACTOR))
                .add(fat.multiply(FAT_FACTOR));
        BigDecimal diff = expected.subtract(BigDecimal.valueOf(kcal)).abs();
        BigDecimal base = BigDecimal.valueOf(Math.max(kcal, 1));
        if (diff.divide(base, 4, RoundingMode.HALF_UP).compareTo(KCAL_TOLERANCE_RATIO) > 0) {
            throw new BusinessException(CommonConstant.DIET_KCAL_MISMATCH_CODE,
                    ExceptionConstant.DIET_KCAL_MISMATCH);
        }
        record.setSource(DietRecordSourceEnum.MANUAL.getCode());
        record.setFoodName(name.trim());
        record.setAmountG(1.0);
        record.setCarbG(round1(carb));
        record.setProteinG(round1(protein));
        record.setFatG(round1(fat));
        record.setKcal(kcal);
    }

    /** 按快照每 100g 值 × 克数 ÷ 100 重算实际摄入（克数 1 位小数、热量取整） */
    private void applyIntakeFromSnapshot(DietRecord record, BigDecimal amount) {
        BigDecimal ratio = amount.divide(PER_100G, 4, RoundingMode.HALF_UP);
        record.setCarbG(round1(record.getCarb100g().multiply(ratio)));
        record.setProteinG(round1(record.getProtein100g().multiply(ratio)));
        record.setFatG(round1(record.getFat100g().multiply(ratio)));
        record.setKcal(BigDecimal.valueOf(record.getKcal100g()).multiply(ratio)
                .setScale(0, RoundingMode.HALF_UP).intValue());
    }

    /** 份量克数校验：必填、>0、≤5000（40501） */
    private BigDecimal validateAmount(BigDecimal amountG) {
        if (amountG == null || amountG.signum() <= 0 || amountG.compareTo(AMOUNT_MAX) > 0) {
            throw new BusinessException(CommonConstant.DIET_AMOUNT_INVALID_CODE,
                    ExceptionConstant.DIET_AMOUNT_INVALID);
        }
        return amountG;
    }

    /** 取本人记录；不存在/他人记录统一 40504（不泄露存在性） */
    private DietRecord selectOwn(Long userId, Long id) {
        DietRecord record = id == null ? null : dietRecordMapper.selectById(id);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(CommonConstant.DIET_RECORD_NOT_FOUND_CODE,
                    ExceptionConstant.DIET_RECORD_NOT_FOUND);
        }
        return record;
    }

    /** 餐别解析（40507） */
    private MealTypeEnum resolveMeal(Integer mealType) {
        MealTypeEnum meal = MealTypeEnum.of(mealType);
        if (meal == null) {
            throw new BusinessException(CommonConstant.DIET_MEAL_TYPE_INVALID_CODE,
                    ExceptionConstant.DIET_MEAL_TYPE_INVALID);
        }
        return meal;
    }

    /** 来源解析（40507） */
    private DietRecordSourceEnum resolveSource(Integer source) {
        DietRecordSourceEnum value = DietRecordSourceEnum.of(source);
        if (value == null) {
            throw new BusinessException(CommonConstant.DIET_MEAL_TYPE_INVALID_CODE,
                    ExceptionConstant.DIET_MEAL_TYPE_INVALID);
        }
        return value;
    }

    /** 日期解析：空默认当日；未来日期 40505（以服务端日期为准） */
    private LocalDate resolveDate(LocalDate date) {
        LocalDate value = date == null ? LocalDate.now() : date;
        if (value.isAfter(LocalDate.now())) {
            throw new BusinessException(CommonConstant.DIET_FUTURE_DATE_CODE,
                    ExceptionConstant.DIET_FUTURE_DATE);
        }
        return value;
    }

    /** 达成率%（1位小数；目标为空或 ≤0 返回 null） */
    private Double rate(double actual, Double target) {
        if (target == null || target <= 0) {
            return null;
        }
        return round1(actual * 100 / target);
    }

    /** Number → double 安全转换（SUM 聚合可能返回 BigDecimal/Double/Long） */
    private double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }

    /** 1 位小数四舍五入 */
    private static double round1(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** 1 位小数四舍五入 */
    private static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** PO → VO */
    private DietRecordVO toVO(DietRecord record) {
        DietRecordVO vo = new DietRecordVO();
        BeanUtils.copyProperties(record, vo);
        MealTypeEnum meal = MealTypeEnum.of(record.getMealType());
        vo.setMealName(meal == null ? null : meal.getDesc());
        return vo;
    }

    /** 操作者标识（审计列） */
    private String operator(Long userId) {
        return "user:" + userId;
    }
}

package cn.zhenxinjian.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.FoodAuditActionEnum;
import cn.zhenxinjian.common.enums.FoodAuditStatusEnum;
import cn.zhenxinjian.common.enums.FoodCategoryEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.common.utils.MacroConsistencyValidator;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.common.utils.SqlLimit;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodMapper;
import cn.zhenxinjian.service.StorageService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义食物服务（共建投稿状态机 + 软删 + 归属隔离）
 * 新增 → source=2/audit_status=0/submitTime + SUBMIT/AI_CHECK 流水；
 * 编辑仅限本人且状态 0/2（已通过转公共只读 40407），编辑 2 → 回 0、清驳回原因、RESUBMIT；
 * 宏量区间与能量守恒后端兜底校验（40402/40403），kcal 为入库基准、缺省由 kj÷4.184 换算
 * 作者: wanglx
 *
 * 守恒口径: |碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%（业务不变量 I11）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomFoodService {

    /** 能量上限 kcal/100g（与建表口径一致） */
    private static final int KCAL_MAX = MacroConsistencyValidator.PER_100G_KCAL_MAX;

    /** 能量上限 kJ/100g（kcal×4.184 换算上限，与 DTO 校验口径一致） */
    private static final int KJ_MAX = 3800;

    /** 千焦→千卡换算系数 */
    private static final BigDecimal KJ_PER_KCAL = new BigDecimal("4.184");

    /** 常用单份克数区间 */
    private static final BigDecimal SERVING_MIN = new BigDecimal("5");
    private static final BigDecimal SERVING_MAX = new BigDecimal("1000");

    /** 自定义食物默认分类（18 加工调理食品及其他类，共建典型为自制复合食物） */
    private static final FoodCategoryEnum DEFAULT_CATEGORY = FoodCategoryEnum.PROCESSED;

    private final FoodMapper foodMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final FoodAuditAiService foodAuditAiService;
    private final FoodAuditLogService foodAuditLogService;
    private final StorageService storageService;

    /**
     * 新增/编辑自定义食物（id 为空新增，非空编辑）
     *
     * @param userId 当前用户ID
     * @param dto    入参（Bean Validation 已做结构校验，此处为业务兜底）
     * @return 保存后的食物
     */
    public FoodVO save(Long userId, CustomFoodSaveDTO dto) {
        // 内容安全：名称/别名敏感词统一拦截
        sensitiveWordFilter.check(dto.getName());
        sensitiveWordFilter.check(dto.getAlias());
        resolveEnergy(dto);
        validateMacro(dto);
        FoodCategoryEnum category = resolveCategory(dto.getCategoryCode());
        if (dto.getId() == null) {
            return create(userId, dto, category);
        }
        return update(userId, dto, category);
    }

    /**
     * 软删除自定义食物（永不物理删除，被饮食记录引用后历史完整保留）
     * 仅本人且状态 0/2 可删；已通过(1)转公共只读
     *
     * @param userId 当前用户ID
     * @param foodId 食物ID
     */
    public void remove(Long userId, Long foodId) {
        Food food = selectEditableCustom(userId, foodId);
        foodMapper.deleteById(food.getId());
    }

    /**
     * 我的自定义食物列表（仅本人活跃数据，0/1/2 全返回，上限 200 条有界查询防无限积累）
     *
     * @param userId 当前用户ID
     */
    public List<FoodVO> listMine(Long userId) {
        List<Food> foods = foodMapper.selectList(
                Wrappers.<Food>lambdaQuery()
                        .eq(Food::getSource, FoodSourceEnum.CUSTOM.getCode())
                        .eq(Food::getUserId, userId)
                        .orderByDesc(Food::getId)
                        .last(SqlLimit.fixed(CommonConstant.CUSTOM_FOOD_MINE_LIMIT)));
        return foods.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 新增：source=2/audit_status=0/submitTime + AI 打标 + SUBMIT/AI_CHECK 流水；名称归属唯一冲突转 40401 */
    private FoodVO create(Long userId, CustomFoodSaveDTO dto, FoodCategoryEnum category) {
        Food food = new Food();
        food.setUserId(userId);
        food.setSource(FoodSourceEnum.CUSTOM.getCode());
        food.setAuditStatus(FoodAuditStatusEnum.PENDING.getCode());
        food.setSubmitTime(LocalDateTime.now());
        food.setCreateBy(Operators.user(userId));
        applyValues(food, dto, category);
        FoodAuditAiService.AiCheckResult ai = runAiCheck(food);
        try {
            foodMapper.insert(food);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CommonConstant.FOOD_NAME_DUPLICATE_CODE,
                    ExceptionConstant.FOOD_NAME_DUPLICATE);
        }
        appendSubmitLogs(food, userId, FoodAuditActionEnum.SUBMIT, null, ai);
        return toVO(foodMapper.selectById(food.getId()));
    }

    /** 编辑：仅限本人状态 0/2；状态回 0、清驳回原因、刷新提交时间、重跑 AI、RESUBMIT/AI_CHECK 流水 */
    private FoodVO update(Long userId, CustomFoodSaveDTO dto, FoodCategoryEnum category) {
        Food food = selectEditableCustom(userId, dto.getId());
        String snapshotBefore = foodAuditLogService.snapshot(food);
        applyValues(food, dto, category);
        food.setAuditStatus(FoodAuditStatusEnum.PENDING.getCode());
        // 重提清空当前驳回原因（完整历史在 food_audit_log）
        food.setAuditRemark(null);
        food.setSubmitTime(LocalDateTime.now());
        FoodAuditAiService.AiCheckResult ai = runAiCheck(food);
        food.setUpdateBy(Operators.user(userId));
        try {
            foodMapper.updateById(food);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CommonConstant.FOOD_NAME_DUPLICATE_CODE,
                    ExceptionConstant.FOOD_NAME_DUPLICATE);
        }
        appendSubmitLogs(food, userId, FoodAuditActionEnum.RESUBMIT, snapshotBefore, ai);
        return toVO(foodMapper.selectById(food.getId()));
    }

    /**
     * 取本人可编辑（状态 0/2）的活跃自定义食物
     * 不存在/内置返回 40404，他人食物返回 40405，已通过转公共返回 40407
     */
    private Food selectEditableCustom(Long userId, Long foodId) {
        Food food = selectOwnCustom(userId, foodId);
        if (!FoodAuditStatusEnum.PENDING.getCode().equals(food.getAuditStatus())
                && !FoodAuditStatusEnum.REJECTED.getCode().equals(food.getAuditStatus())) {
            // 已通过(1)转公共的食物原作者只读，仅管理员可维护
            throw new BusinessException(CommonConstant.FOOD_PUBLIC_READONLY_CODE,
                    ExceptionConstant.FOOD_PUBLIC_READONLY);
        }
        return food;
    }

    /**
     * 取本人活跃自定义食物；不存在/内置返回 40404，他人食物返回 40405
     */
    private Food selectOwnCustom(Long userId, Long foodId) {
        Food food = foodMapper.selectById(foodId);
        if (food == null || FoodSourceEnum.BUILT_IN.getCode().equals(food.getSource())) {
            throw new BusinessException(CommonConstant.FOOD_NOT_FOUND_CODE,
                    ExceptionConstant.FOOD_NOT_FOUND);
        }
        if (!userId.equals(food.getUserId())) {
            throw new BusinessException(CommonConstant.FOOD_NOT_OWNER_CODE,
                    ExceptionConstant.FOOD_NOT_OWNER);
        }
        return food;
    }

    /** AI 打标（异常已由 FoodAuditAiService 内部降级 none，不阻塞投稿），结论随食物落库 */
    private FoodAuditAiService.AiCheckResult runAiCheck(Food food) {
        FoodAuditAiService.AiCheckResult ai = foodAuditAiService.audit(food);
        food.setAiVerdict(ai.verdict().getCode());
        food.setAiSuggestion(ai.suggestion());
        return ai;
    }

    /** 写 SUBMIT/RESUBMIT + AI_CHECK 双流水；留痕尽力而为不阻塞用户投稿（流水只追加） */
    private void appendSubmitLogs(Food food, Long operatorId, FoodAuditActionEnum action,
                                  String snapshotBefore, FoodAuditAiService.AiCheckResult ai) {
        try {
            String snapshotAfter = foodAuditLogService.snapshot(food);
            foodAuditLogService.record(food.getId(), action, operatorId,
                    null, null, null, snapshotBefore, snapshotAfter);
            foodAuditLogService.record(food.getId(), FoodAuditActionEnum.AI_CHECK,
                    FoodAuditLogService.SYSTEM_OPERATOR_ID, ai.verdict(), ai.suggestion(),
                    null, null, snapshotAfter);
        } catch (Exception e) {
            log.warn("审核流水写入失败（不阻塞投稿）: foodId={}, action={}", food.getId(), action.getCode(), e);
        }
    }

    /** 能量口径：kcal 与 kj 至少一项非空；kcal 缺省按 kj÷4.184 四舍五入换算（kcal 为入库基准） */
    private void resolveEnergy(CustomFoodSaveDTO dto) {
        if (dto.getKcal() == null) {
            if (dto.getKj() == null) {
                throw new BusinessException(CommonConstant.FOOD_MACRO_INVALID_CODE,
                        ExceptionConstant.FOOD_MACRO_INVALID);
            }
            dto.setKcal(BigDecimal.valueOf(dto.getKj())
                    .divide(KJ_PER_KCAL, 0, RoundingMode.HALF_UP).intValue());
        }
        if (dto.getKj() != null && (dto.getKj() < 0 || dto.getKj() > KJ_MAX)) {
            throw new BusinessException(CommonConstant.FOOD_MACRO_INVALID_CODE,
                    ExceptionConstant.FOOD_MACRO_INVALID);
        }
    }

    /** 业务兜底校验：宏量非负且 ≤100、能量 0-900、单份克数 5-1000 + 能量守恒 ±10% */
    private void validateMacro(CustomFoodSaveDTO dto) {
        BigDecimal carb = dto.getCarb();
        BigDecimal protein = dto.getProtein();
        BigDecimal fat = dto.getFat();
        if (carb.signum() < 0 || protein.signum() < 0 || fat.signum() < 0
                || carb.compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || protein.compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || fat.compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || dto.getKcal() < 0 || dto.getKcal() > KCAL_MAX
                || (dto.getServing() != null
                        && (dto.getServing().compareTo(SERVING_MIN) < 0
                                || dto.getServing().compareTo(SERVING_MAX) > 0))) {
            throw new BusinessException(CommonConstant.FOOD_MACRO_INVALID_CODE,
                    ExceptionConstant.FOOD_MACRO_INVALID);
        }
        if (!MacroConsistencyValidator.withinTolerance(carb, protein, fat, dto.getKcal())) {
            throw new BusinessException(CommonConstant.FOOD_KCAL_MISMATCH_CODE,
                    ExceptionConstant.FOOD_KCAL_MISMATCH);
        }
    }

    /** 分类编号解析（缺省默认 10；非法拒绝） */
    private FoodCategoryEnum resolveCategory(String categoryCode) {
        if (categoryCode == null || categoryCode.isEmpty()) {
            return DEFAULT_CATEGORY;
        }
        return FoodCategoryEnum.of(categoryCode)
                .orElseThrow(() -> new BusinessException(CommonConstant.FOOD_MACRO_INVALID_CODE,
                        "分类编号非法，仅支持01-10"));
    }

    /** 写入业务字段（名称/别名/分类/营养值/单份克数/千焦/单位/图片） */
    private void applyValues(Food food, CustomFoodSaveDTO dto, FoodCategoryEnum category) {
        food.setName(dto.getName().trim());
        food.setAlias(dto.getAlias() == null ? "" : dto.getAlias().trim());
        food.setCategoryCode(category.getCode());
        food.setCategoryName(category.getDesc());
        food.setCarb(dto.getCarb());
        food.setProtein(dto.getProtein());
        food.setFat(dto.getFat());
        food.setKcal(dto.getKcal());
        food.setKj(dto.getKj());
        food.setServing(dto.getServing() == null ? new BigDecimal("100") : dto.getServing());
        food.setUnit(StrUtil.trimToNull(dto.getUnit()));
        food.setImage(StrUtil.trimToNull(dto.getImage()));
    }

    /** PO → VO */
    private FoodVO toVO(Food food) {
        FoodVO vo = new FoodVO();
        BeanUtils.copyProperties(food, vo);
        // 共建图存相对 objectKey → 出参解析为公网地址（存相对、展示绝对；绝对 URL 原样透传）
        vo.setImage(storageService.publicUrl(vo.getImage()));
        return vo;
    }
}

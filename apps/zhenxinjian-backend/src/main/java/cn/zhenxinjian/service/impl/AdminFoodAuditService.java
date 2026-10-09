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
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.FoodAuditDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.mapper.FoodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 管理端共建食物审核服务（APPROVE/REJECT/ADMIN_FIX 状态机 + 已通过后维护入口）
 * 仅 source=2 可审/可维护（40915）；审核目标限状态 0/2（已通过走停用/软删维护入口）；
 * REJECT 必填原因（40916）；ADMIN_FIX 空字段沿用原值、合并后整体校验（40917）；三动作均写流水
 * 作者: wanglx
 *
 * 守恒口径: |碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%（业务不变量 I11）
 */
@Service
@RequiredArgsConstructor
public class AdminFoodAuditService {

    /** 能量上限 kcal/100g（与建表口径一致） */
    private static final int KCAL_MAX = MacroConsistencyValidator.PER_100G_KCAL_MAX;

    /** 能量上限 kJ/100g（kcal×4.184 换算上限，与 DTO 校验口径一致） */
    private static final int KJ_MAX = 3800;

    private final FoodMapper foodMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final FoodAuditLogService foodAuditLogService;

    /**
     * 审核共建食物（状态 0/2 → 1 通过 / 2 驳回；ADMIN_FIX 按修正值落库后通过）
     *
     * @param id  食物ID
     * @param dto 审核动作入参（action 必填合法；REJECT 必填 remark；ADMIN_FIX 必填 fix）
     */
    public void audit(Long id, FoodAuditDTO dto) {
        FoodAuditActionEnum action = FoodAuditActionEnum.of(dto.getAction());
        if (action == null || action == FoodAuditActionEnum.SUBMIT
                || action == FoodAuditActionEnum.RESUBMIT || action == FoodAuditActionEnum.AI_CHECK) {
            // Bean Validation 已拦截非法动作；此处兜底防御（用户/系统动作不进审核入口）
            throw new BusinessException(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE,
                    ExceptionConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID);
        }
        Food food = requireAuditableCustom(id);
        String snapshotBefore = foodAuditLogService.snapshot(food);
        switch (action) {
            case APPROVE -> approve(food, dto.getRemark(), snapshotBefore);
            case REJECT -> reject(food, requireRemark(dto.getRemark()), snapshotBefore);
            case ADMIN_FIX -> adminFix(food, dto, snapshotBefore);
            default -> throw new BusinessException(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE,
                    ExceptionConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID);
        }
    }

    /**
     * 停用/启用已通过(1)的共建食物（明确维护入口；基础食物走内置接口）
     *
     * @param id     食物ID
     * @param status 目标状态：0停用 1启用
     */
    public void changeCustomStatus(Long id, Integer status) {
        Food food = requireMaintainableCustom(id);
        food.setStatus(status);
        food.setUpdateBy(operator());
        foodMapper.updateById(food);
    }

    /**
     * 软删已通过(1)的共建食物（逻辑删除，饮食记录引用不断链）
     *
     * @param id 食物ID
     */
    public void removeCustom(Long id) {
        requireMaintainableCustom(id);
        foodMapper.deleteById(id);
    }

    // ==================== 审核动作 ====================

    /** 通过：转公共（audit_status=1），清驳回原因 */
    private void approve(Food food, String remark, String snapshotBefore) {
        food.setAuditStatus(FoodAuditStatusEnum.APPROVED.getCode());
        food.setAuditRemark(null);
        food.setUpdateBy(operator());
        foodMapper.updateById(food);
        appendLog(food, FoodAuditActionEnum.APPROVE, remark, snapshotBefore);
    }

    /** 驳回：退回私有可改（audit_status=2），驳回原因落列并留痕 */
    private void reject(Food food, String remark, String snapshotBefore) {
        food.setAuditStatus(FoodAuditStatusEnum.REJECTED.getCode());
        food.setAuditRemark(remark);
        food.setUpdateBy(operator());
        foodMapper.updateById(food);
        appendLog(food, FoodAuditActionEnum.REJECT, remark, snapshotBefore);
    }

    /** 修正后通过：fix 空字段沿用原值，合并后整体校验（40917），按修正值落库转公共 */
    private void adminFix(Food food, FoodAuditDTO dto, String snapshotBefore) {
        FoodAuditDTO.Fix fix = dto.getFix();
        if (fix == null) {
            throw new BusinessException(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE,
                    ExceptionConstant.ADMIN_FOOD_FIX_INVALID);
        }
        mergeFix(food, fix);
        validateFixed(food);
        food.setAuditStatus(FoodAuditStatusEnum.APPROVED.getCode());
        food.setAuditRemark(null);
        food.setUpdateBy(operator());
        foodMapper.updateById(food);
        appendLog(food, FoodAuditActionEnum.ADMIN_FIX, dto.getRemark(), snapshotBefore);
    }

    /** 合并修正值（空字段沿用原值；名称/别名过敏感词，分类须为合法枚举） */
    private void mergeFix(Food food, FoodAuditDTO.Fix fix) {
        if (fix.getName() != null) {
            sensitiveWordFilter.check(fix.getName());
            food.setName(fix.getName().trim());
        }
        if (fix.getAlias() != null) {
            sensitiveWordFilter.check(fix.getAlias());
            food.setAlias(fix.getAlias().trim());
        }
        if (fix.getCategoryCode() != null) {
            FoodCategoryEnum category = FoodCategoryEnum.of(fix.getCategoryCode().trim())
                    .orElseThrow(() -> new BusinessException(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE,
                            ExceptionConstant.ADMIN_FOOD_FIX_INVALID));
            food.setCategoryCode(category.getCode());
            food.setCategoryName(category.getDesc());
        }
        if (fix.getCarb() != null) {
            food.setCarb(fix.getCarb());
        }
        if (fix.getProtein() != null) {
            food.setProtein(fix.getProtein());
        }
        if (fix.getFat() != null) {
            food.setFat(fix.getFat());
        }
        if (fix.getKcal() != null) {
            food.setKcal(fix.getKcal());
        }
        if (fix.getKj() != null) {
            food.setKj(fix.getKj());
        }
        if (fix.getUnit() != null) {
            food.setUnit(StrUtil.trimToNull(fix.getUnit()));
        }
        if (fix.getServing() != null) {
            food.setServing(fix.getServing());
        }
    }

    /** 合并后整体校验：宏量 0-100、能量 0-900、千焦 0-3800、单份 5-1000 + 守恒 ±10%（40917） */
    private void validateFixed(Food food) {
        boolean invalid = food.getCarb().signum() < 0
                || food.getProtein().signum() < 0
                || food.getFat().signum() < 0
                || food.getCarb().compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || food.getProtein().compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || food.getFat().compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || food.getKcal() < 0 || food.getKcal() > KCAL_MAX
                || (food.getKj() != null && (food.getKj() < 0 || food.getKj() > KJ_MAX))
                || (food.getServing() != null
                        && (food.getServing().compareTo(new BigDecimal("5")) < 0
                                || food.getServing().compareTo(new BigDecimal("1000")) > 0))
                || !MacroConsistencyValidator.withinTolerance(
                        food.getCarb(), food.getProtein(), food.getFat(), food.getKcal());
        if (invalid) {
            throw new BusinessException(CommonConstant.ADMIN_FOOD_FIX_INVALID_CODE,
                    ExceptionConstant.ADMIN_FOOD_FIX_INVALID);
        }
    }

    // ==================== 目标与留痕 ====================

    /** 取可审核的共建食物：不存在 40404；非共建 40915；已通过(1)不再是审核目标 40915 */
    private Food requireAuditableCustom(Long id) {
        Food food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(CommonConstant.FOOD_NOT_FOUND_CODE,
                    ExceptionConstant.FOOD_NOT_FOUND);
        }
        if (!FoodSourceEnum.CUSTOM.getCode().equals(food.getSource())
                || FoodAuditStatusEnum.APPROVED.getCode().equals(food.getAuditStatus())
                || FoodAuditStatusEnum.NOT_REQUIRED.getCode().equals(food.getAuditStatus())) {
            throw new BusinessException(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE,
                    ExceptionConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID);
        }
        return food;
    }

    /** 取可维护（停用/软删）的已通过共建食物：不存在 40404；非共建/未通过 40915 */
    private Food requireMaintainableCustom(Long id) {
        Food food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(CommonConstant.FOOD_NOT_FOUND_CODE,
                    ExceptionConstant.FOOD_NOT_FOUND);
        }
        if (!FoodSourceEnum.CUSTOM.getCode().equals(food.getSource())
                || !FoodAuditStatusEnum.APPROVED.getCode().equals(food.getAuditStatus())) {
            throw new BusinessException(CommonConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID_CODE,
                    ExceptionConstant.ADMIN_FOOD_AUDIT_TARGET_INVALID);
        }
        return food;
    }

    /** 驳回原因必填（40916） */
    private String requireRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            throw new BusinessException(CommonConstant.ADMIN_FOOD_REJECT_REASON_REQUIRED_CODE,
                    ExceptionConstant.ADMIN_FOOD_REJECT_REASON_REQUIRED);
        }
        return remark.trim();
    }

    /** 审核动作留痕（管理员动作必写流水，失败即报错让管理员感知重试） */
    private void appendLog(Food food, FoodAuditActionEnum action, String remark, String snapshotBefore) {
        foodAuditLogService.record(food.getId(), action, operatorId(),
                null, null, remark, snapshotBefore, foodAuditLogService.snapshot(food));
    }

    /** 当前管理员 ID（无上下文兜底 0=系统） */
    private Long operatorId() {
        LoginUserVO user = UserContext.get();
        return user == null || user.getId() == null ? 0L : user.getId();
    }

    /** 操作者标识（审计列，当前管理员用户名） */
    private String operator() {
        LoginUserVO user = UserContext.get();
        return user != null && StringUtils.hasText(user.getUsername())
                ? user.getUsername() : CommonConstant.CREATE_BY_SYSTEM;
    }
}

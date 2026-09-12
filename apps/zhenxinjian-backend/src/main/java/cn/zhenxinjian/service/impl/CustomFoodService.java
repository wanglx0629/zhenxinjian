package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.FoodCategoryEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.MacroConsistencyValidator;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义食物服务（新增/编辑一体 + 软删 + 归属隔离；宏量区间与能量守恒后端兜底校验）
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

    /** 常用单份克数区间 */
    private static final BigDecimal SERVING_MIN = new BigDecimal("5");
    private static final BigDecimal SERVING_MAX = new BigDecimal("1000");

    /** 自定义食物默认分类（10 油脂·调味·饮品） */
    private static final FoodCategoryEnum DEFAULT_CATEGORY = FoodCategoryEnum.OIL_CONDIMENT_DRINK;

    private final FoodMapper foodMapper;

    /**
     * 新增/编辑自定义食物（id 为空新增，非空编辑）
     *
     * @param userId 当前用户ID
     * @param dto    入参（Bean Validation 已做区间校验，此处为业务兜底）
     * @return 保存后的食物
     */
    public FoodVO save(Long userId, CustomFoodSaveDTO dto) {
        validateMacro(dto);
        FoodCategoryEnum category = resolveCategory(dto.getCategoryCode());
        if (dto.getId() == null) {
            return create(userId, dto, category);
        }
        return update(userId, dto, category);
    }

    /**
     * 软删除自定义食物（永不物理删除，被饮食记录引用后历史完整保留）
     *
     * @param userId 当前用户ID
     * @param foodId 食物ID
     */
    public void remove(Long userId, Long foodId) {
        Food food = selectOwnCustom(userId, foodId);
        foodMapper.deleteById(food.getId());
    }

    /**
     * 我的自定义食物列表（仅本人活跃数据）
     *
     * @param userId 当前用户ID
     */
    public List<FoodVO> listMine(Long userId) {
        List<Food> foods = foodMapper.selectList(
                Wrappers.<Food>lambdaQuery()
                        .eq(Food::getSource, FoodSourceEnum.CUSTOM.getCode())
                        .eq(Food::getUserId, userId)
                        .orderByDesc(Food::getId));
        return foods.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 新增：名称归属唯一，冲突转 40401 不抛 500 */
    private FoodVO create(Long userId, CustomFoodSaveDTO dto, FoodCategoryEnum category) {
        Food food = new Food();
        food.setUserId(userId);
        food.setSource(FoodSourceEnum.CUSTOM.getCode());
        food.setCreateBy(operator(userId));
        applyValues(food, dto, category);
        try {
            foodMapper.insert(food);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CommonConstant.FOOD_NAME_DUPLICATE_CODE,
                    ExceptionConstant.FOOD_NAME_DUPLICATE);
        }
        return toVO(foodMapper.selectById(food.getId()));
    }

    /** 编辑：仅限本人自定义食物；内置拒绝 40404，他人拒绝 40405 */
    private FoodVO update(Long userId, CustomFoodSaveDTO dto, FoodCategoryEnum category) {
        Food food = selectOwnCustom(userId, dto.getId());
        applyValues(food, dto, category);
        food.setUpdateBy(operator(userId));
        try {
            foodMapper.updateById(food);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CommonConstant.FOOD_NAME_DUPLICATE_CODE,
                    ExceptionConstant.FOOD_NAME_DUPLICATE);
        }
        return toVO(foodMapper.selectById(food.getId()));
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
        FoodCategoryEnum category = FoodCategoryEnum.of(categoryCode);
        if (category == null) {
            throw new BusinessException(CommonConstant.FOOD_MACRO_INVALID_CODE,
                    "分类编号非法，仅支持01-10");
        }
        return category;
    }

    /** 写入业务字段（名称/别名/分类/营养值/单份克数） */
    private void applyValues(Food food, CustomFoodSaveDTO dto, FoodCategoryEnum category) {
        food.setName(dto.getName().trim());
        food.setAlias(dto.getAlias() == null ? "" : dto.getAlias().trim());
        food.setCategoryCode(category.getCode());
        food.setCategoryName(category.getDesc());
        food.setCarb(dto.getCarb());
        food.setProtein(dto.getProtein());
        food.setFat(dto.getFat());
        food.setKcal(dto.getKcal());
        food.setServing(dto.getServing() == null ? new BigDecimal("100") : dto.getServing());
    }

    /** PO → VO */
    private FoodVO toVO(Food food) {
        FoodVO vo = new FoodVO();
        BeanUtils.copyProperties(food, vo);
        return vo;
    }

    /** 操作者标识（审计列） */
    private String operator(Long userId) {
        return "user:" + userId;
    }
}

package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.constant.FoodHotConstant;
import cn.zhenxinjian.common.enums.FoodCategoryEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.query.FoodSearchQuery;
import cn.zhenxinjian.domain.vo.FoodCalcVO;
import cn.zhenxinjian.domain.vo.FoodCategoryVO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 食物库查询服务（搜索/分类/热门/详情/试算；可见范围 = 全部内置 + 当前用户自定义）
 * 作者: wanglx
 */
@Service
@RequiredArgsConstructor
public class FoodService {

    /** 换算基数：营养值口径为每 100g */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final FoodMapper foodMapper;

    /**
     * 食物搜索：关键字匹配名称或别名（任一命中），可叠加分类筛选，分页（单页 ≤50）
     *
     * @param userId 当前用户ID（自定义食物归属过滤）
     * @param query  查询条件
     * @return 分页结果；空关键字且未选分类时返回空集（不报错）
     */
    public IPage<FoodVO> search(Long userId, FoodSearchQuery query) {
        long size = Math.min(query.safeSize(), CommonConstant.FOOD_SEARCH_MAX_SIZE);
        Page<Food> page = new Page<>(query.safePage(), size);
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        String categoryCode = query.getCategoryCode() == null ? "" : query.getCategoryCode().trim();
        if (keyword.isEmpty() && categoryCode.isEmpty()) {
            Page<FoodVO> empty = new Page<>(query.safePage(), size);
            empty.setRecords(new ArrayList<>());
            empty.setTotal(0L);
            return empty;
        }
        IPage<Food> result = foodMapper.selectPage(page,
                Wrappers.<Food>lambdaQuery()
                        .and(w -> w
                                .or().eq(Food::getSource, FoodSourceEnum.BUILT_IN.getCode())
                                .or(o -> o.eq(Food::getSource, FoodSourceEnum.CUSTOM.getCode())
                                        .eq(Food::getUserId, userId)))
                        .eq(!categoryCode.isEmpty(), Food::getCategoryCode, categoryCode)
                        .and(!keyword.isEmpty(),
                                w -> w.like(Food::getName, keyword).or().like(Food::getAlias, keyword))
                        .orderByAsc(Food::getCode).orderByDesc(Food::getId));
        return result.convert(this::toVO);
    }

    /**
     * 分类列表：10 大分类，顺序固定 01–10
     */
    public List<FoodCategoryVO> categories() {
        List<FoodCategoryVO> list = new ArrayList<>();
        for (FoodCategoryEnum category : FoodCategoryEnum.values()) {
            FoodCategoryVO vo = new FoodCategoryVO();
            vo.setCode(category.getCode());
            vo.setName(category.getDesc());
            list.add(vo);
        }
        return list;
    }

    /**
     * 热门食物：静态编号清单按 code 查库，保持清单顺序
     */
    public List<FoodVO> hot() {
        List<Food> foods = foodMapper.selectList(
                Wrappers.<Food>lambdaQuery()
                        .eq(Food::getSource, FoodSourceEnum.BUILT_IN.getCode())
                        .in(Food::getCode, FoodHotConstant.HOT_FOOD_CODES));
        Map<String, Food> byCode = foods.stream()
                .collect(Collectors.toMap(Food::getCode, Function.identity(), (a, b) -> a));
        List<FoodVO> list = new ArrayList<>();
        for (String code : FoodHotConstant.HOT_FOOD_CODES) {
            Food food = byCode.get(code);
            if (food != null) {
                list.add(toVO(food));
            }
        }
        return list;
    }

    /**
     * 食物详情：内置食物全员可见；自定义食物仅本人可见
     *
     * @param userId 当前用户ID
     * @param foodId 食物ID
     * @return 食物详情
     */
    public FoodVO detail(Long userId, Long foodId) {
        Food food = selectVisible(userId, foodId);
        return toVO(food);
    }

    /**
     * 份量试算：每 100g 值 × 克数 ÷ 100，克数限 1–10000
     *
     * @param userId 当前用户ID
     * @param foodId 食物ID
     * @param grams  试算克数
     * @return 试算结果（宏量保留 2 位小数，能量取整）
     */
    public FoodCalcVO calc(Long userId, Long foodId, Integer grams) {
        if (grams == null || grams < CommonConstant.FOOD_CALC_MIN_GRAMS
                || grams > CommonConstant.FOOD_CALC_MAX_GRAMS) {
            throw new BusinessException(CommonConstant.FOOD_GRAMS_INVALID_CODE,
                    ExceptionConstant.FOOD_GRAMS_INVALID);
        }
        Food food = selectVisible(userId, foodId);
        BigDecimal ratio = new BigDecimal(grams).divide(HUNDRED, 6, RoundingMode.HALF_UP);
        FoodCalcVO vo = new FoodCalcVO();
        vo.setFoodId(foodId);
        vo.setGrams(new BigDecimal(grams).setScale(1, RoundingMode.HALF_UP));
        vo.setCarb(food.getCarb().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        vo.setProtein(food.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        vo.setFat(food.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        vo.setKcal(new BigDecimal(food.getKcal()).multiply(ratio)
                .setScale(0, RoundingMode.HALF_UP).intValue());
        return vo;
    }

    /** 查可见食物（内置全员 / 自定义本人），不存在或不可见返回 40404 */
    private Food selectVisible(Long userId, Long foodId) {
        Food food = foodMapper.selectById(foodId);
        if (food == null) {
            throw new BusinessException(CommonConstant.FOOD_NOT_FOUND_CODE,
                    ExceptionConstant.FOOD_NOT_FOUND);
        }
        if (FoodSourceEnum.CUSTOM.getCode().equals(food.getSource())
                && !food.getUserId().equals(userId)) {
            throw new BusinessException(CommonConstant.FOOD_NOT_FOUND_CODE,
                    ExceptionConstant.FOOD_NOT_FOUND);
        }
        return food;
    }

    /** PO → VO */
    private FoodVO toVO(Food food) {
        FoodVO vo = new FoodVO();
        BeanUtils.copyProperties(food, vo);
        return vo;
    }
}

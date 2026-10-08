package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.utils.MacroConsistencyValidator;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.domain.dto.AdminFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.FoodImage;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.query.AdminFoodQuery;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.mapper.FoodImageMapper;
import cn.zhenxinjian.mapper.FoodMapper;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.StorageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端食物库维护服务（仅内置食物可写；自定义食物只读）
 * 作者: wanglx
 *
 * 口径：新增内置 code 从 F201 起顺延（查 max(code) 数值部分 +1，无内置则用 201）；
 * 宏量 0-100、热量与 4/4/9 换算偏差 ±10% 校验复用 C 端口径（40402/40403）；
 * 同名（内置活跃范围内）拒收 40903；编辑/删除/停用仅 source=1，否则 40404
 */
@Service
@RequiredArgsConstructor
public class AdminFoodService extends ServiceImpl<FoodMapper, Food> {

    /** 内置食物编号起始（F001-F200 为初始化数据） */
    private static final int BUILTIN_CODE_START = 201;

    private final FoodImageMapper foodImageMapper;
    private final UserMapper userMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final StorageService storageService;

    /**
     * 分页查询（名称/别名模糊 + 分类/来源/状态/审核状态/AI 结论筛选；共建行带提交人昵称）
     */
    public IPage<FoodVO> page(AdminFoodQuery query) {
        IPage<Food> page = page(query.toPage(), Wrappers.<Food>lambdaQuery()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Food::getName, query.getKeyword())
                        .or()
                        .like(Food::getAlias, query.getKeyword()))
                .eq(StringUtils.hasText(query.getCategoryCode()), Food::getCategoryCode, query.getCategoryCode())
                .eq(query.getSource() != null, Food::getSource, query.getSource())
                .eq(query.getStatus() != null, Food::getStatus, query.getStatus())
                .eq(query.getAuditStatus() != null, Food::getAuditStatus, query.getAuditStatus())
                .eq(StringUtils.hasText(query.getAiVerdict()), Food::getAiVerdict, query.getAiVerdict())
                .orderByAsc(Food::getId));
        Map<Long, String> nicknameMap = loadSubmitterNicknames(page.getRecords());
        IPage<FoodVO> voPage = page.convert(food -> toVO(food, nicknameMap));
        fillImages(voPage.getRecords());
        return voPage;
    }

    /**
     * 新增内置食物
     *
     * @param dto 入参（同名内置活跃拒收 40903；宏量/守恒 40402/40403）
     */
    public void create(AdminFoodSaveDTO dto) {
        // 内容安全：名称/别名敏感词统一拦截
        sensitiveWordFilter.check(dto.getName());
        sensitiveWordFilter.check(dto.getAlias());
        checkDuplicateName(dto.getName(), null);
        validateMacro(dto);
        Food food = new Food();
        food.setCode(nextCode());
        food.setSource(FoodSourceEnum.BUILT_IN.getCode());
        food.setStatus(1);
        food.setCreateBy(operator());
        applyValues(food, dto);
        save(food);
    }

    /**
     * 编辑内置食物（自定义食物拒收 40404；同名排除自身）
     */
    public void update(Long id, AdminFoodSaveDTO dto) {
        Food food = requireBuiltin(id);
        // 内容安全：名称/别名敏感词统一拦截
        sensitiveWordFilter.check(dto.getName());
        sensitiveWordFilter.check(dto.getAlias());
        checkDuplicateName(dto.getName(), id);
        validateMacro(dto);
        applyValues(food, dto);
        food.setUpdateBy(operator());
        updateById(food);
    }

    /**
     * 软删内置食物（自定义食物拒收 40404）
     */
    public void delete(Long id) {
        requireBuiltin(id);
        removeById(id);
    }

    /**
     * 停用/启用内置食物（status 仅允许 0/1，Controller 层 Bean Validation 兜底）
     */
    public void changeStatus(Long id, Integer status) {
        Food food = requireBuiltin(id);
        food.setStatus(status);
        food.setUpdateBy(operator());
        updateById(food);
    }

    /** 取内置食物；不存在/自定义返回 40404 */
    private Food requireBuiltin(Long id) {
        Food food = getById(id);
        if (food == null || !FoodSourceEnum.BUILT_IN.getCode().equals(food.getSource())) {
            throw new BusinessException(CommonConstant.FOOD_NOT_FOUND_CODE,
                    ExceptionConstant.FOOD_NOT_FOUND);
        }
        return food;
    }

    /** 同名（内置活跃范围内）拒收 40903；excludeId 用于编辑时排除自身 */
    private void checkDuplicateName(String name, Long excludeId) {
        Long count = count(Wrappers.<Food>lambdaQuery()
                .eq(Food::getName, name.trim())
                .eq(Food::getSource, FoodSourceEnum.BUILT_IN.getCode())
                .ne(excludeId != null, Food::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(CommonConstant.ADMIN_FOOD_CONFLICT_CODE,
                    ExceptionConstant.ADMIN_FOOD_CONFLICT);
        }
    }

    /** 业务兜底校验：宏量非负且 ≤100 + 能量守恒 ±10%（与 C 端同口径） */
    private void validateMacro(AdminFoodSaveDTO dto) {
        BigDecimal carb = dto.getCarb();
        BigDecimal protein = dto.getProtein();
        BigDecimal fat = dto.getFat();
        if (carb.signum() < 0 || protein.signum() < 0 || fat.signum() < 0
                || carb.compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || protein.compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0
                || fat.compareTo(MacroConsistencyValidator.PER_100G_MACRO_MAX) > 0) {
            throw new BusinessException(CommonConstant.FOOD_MACRO_INVALID_CODE,
                    ExceptionConstant.FOOD_MACRO_INVALID);
        }
        if (!MacroConsistencyValidator.withinTolerance(carb, protein, fat, dto.getKcal())) {
            throw new BusinessException(CommonConstant.FOOD_KCAL_MISMATCH_CODE,
                    ExceptionConstant.FOOD_KCAL_MISMATCH);
        }
    }

    /** 内置 code 顺延：查 max(code) 数值部分 +1，无内置则从 F201 起 */
    private String nextCode() {
        Food max = getOne(Wrappers.<Food>lambdaQuery()
                .select(Food::getCode)
                .likeRight(Food::getCode, "F")
                .last("ORDER BY LENGTH(code) DESC, code DESC LIMIT 1"), false);
        int next = BUILTIN_CODE_START;
        if (max != null && StringUtils.hasText(max.getCode()) && max.getCode().length() > 1) {
            try {
                next = Math.max(next, Integer.parseInt(max.getCode().substring(1)) + 1);
            } catch (NumberFormatException ignored) {
                // code 非纯数字时回落起始值
            }
        }
        return "F" + next;
    }

    /** 写入业务字段 */
    private void applyValues(Food food, AdminFoodSaveDTO dto) {
        food.setName(dto.getName().trim());
        food.setAlias(dto.getAlias() == null ? "" : dto.getAlias().trim());
        food.setCategoryCode(dto.getCategoryCode());
        food.setCategoryName(dto.getCategoryName());
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

    /** PO → VO（共建行带提交人昵称兜底「用户#ID」；基础食物 submitterName 为空） */
    private FoodVO toVO(Food food, Map<Long, String> nicknameMap) {
        FoodVO vo = toVO(food);
        if (FoodSourceEnum.CUSTOM.getCode().equals(food.getSource()) && food.getUserId() != null) {
            vo.setSubmitterName(nicknameMap.getOrDefault(food.getUserId(), "用户#" + food.getUserId()));
        }
        return vo;
    }

    /** 批量加载本页共建食物提交人昵称（避免 N+1） */
    private Map<Long, String> loadSubmitterNicknames(List<Food> foods) {
        Set<Long> ids = foods.stream()
                .filter(f -> FoodSourceEnum.CUSTOM.getCode().equals(f.getSource()))
                .map(Food::getUserId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    /** 批量填充图片 URL（一次查询防 N+1；foods.image 共建用户上传图优先，空值回退 food_images 预热图） */
    private void fillImages(List<FoodVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<String, String> urlByCode = loadWarmImages(list);
        for (FoodVO vo : list) {
            // foods.image（共建用户上传）优先；仅空值回退内置预热图，避免覆盖
            if (!StringUtils.hasText(vo.getImage())) {
                vo.setImage(urlByCode.get(vo.getCode()));
            }
            // 共建图存相对 objectKey → 按当前存储配置解析为公网地址；预热图等绝对 URL 原样透传
            vo.setImage(storageService.publicUrl(vo.getImage()));
        }
    }

    /** 内置预热图映射（一码一图，无内置编码时返回空集不查库） */
    private Map<String, String> loadWarmImages(List<FoodVO> list) {
        List<String> codes = list.stream()
                .map(FoodVO::getCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            // 空码场景返回可容忍 null 键的 HashMap：共建食物 code 为空时 get(null) 不得抛 NPE
            return new HashMap<>();
        }
        return foodImageMapper.selectList(Wrappers.<FoodImage>lambdaQuery()
                        .select(FoodImage::getFoodCode, FoodImage::getUrl)
                        .in(FoodImage::getFoodCode, codes)
                        .eq(FoodImage::getStatus, 1))
                .stream()
                .filter(img -> StringUtils.hasText(img.getUrl()))
                .collect(Collectors.toMap(FoodImage::getFoodCode, FoodImage::getUrl, (a, b) -> a));
    }

    /** 操作者标识（审计列，当前管理员用户名） */
    private String operator() {
        LoginUserVO user = UserContext.get();
        return user != null && StringUtils.hasText(user.getUsername())
                ? user.getUsername() : CommonConstant.CREATE_BY_SYSTEM;
    }
}

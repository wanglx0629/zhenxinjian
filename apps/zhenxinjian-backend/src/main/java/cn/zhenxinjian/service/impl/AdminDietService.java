package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.query.AdminDietRecordQuery;
import cn.zhenxinjian.domain.vo.AdminDietRecordVO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端饮食记录查看服务（只读：不开放改/删用户业务数据）
 * 作者: wanglx
 */
@Service
@RequiredArgsConstructor
public class AdminDietService {

    private final DietRecordMapper dietRecordMapper;
    private final UserMapper userMapper;

    /**
     * 分页查询（用户关键词 + 日期区间 + 餐别筛选）
     *
     * @param query 查询条件（startDate &gt; endDate 时 40902）
     * @return 分页结果（昵称批量组装，用户已删显示「用户#id」）
     */
    public IPage<AdminDietRecordVO> page(AdminDietRecordQuery query) {
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new BusinessException(CommonConstant.STATS_PARAM_INVALID_CODE,
                    ExceptionConstant.STATS_PARAM_INVALID);
        }
        List<Long> userIds = resolveUserIds(query.getUserKeyword());
        // 关键词有输入但无命中用户 → 直接返回空页
        if (StringUtils.hasText(query.getUserKeyword()) && userIds.isEmpty()) {
            return new Page<AdminDietRecordVO>(query.safePage(), query.safeSize(), 0);
        }
        IPage<DietRecord> page = dietRecordMapper.selectPage(query.toPage(),
                Wrappers.<DietRecord>lambdaQuery()
                        .in(userIds != null && !userIds.isEmpty(), DietRecord::getUserId, userIds)
                        .ge(query.getStartDate() != null, DietRecord::getRecordDate, query.getStartDate())
                        .le(query.getEndDate() != null, DietRecord::getRecordDate, query.getEndDate())
                        .eq(query.getMealType() != null, DietRecord::getMealType, query.getMealType())
                        .orderByDesc(DietRecord::getRecordDate)
                        .orderByDesc(DietRecord::getId));
        Map<Long, String> nicknameMap = loadNicknames(page.getRecords());
        return page.convert(record -> toVO(record, nicknameMap));
    }

    /** 解析用户关键词：纯数字按 ID 精确，否则昵称模糊取 ID 集；无关键词返回 null（不过滤） */
    private List<Long> resolveUserIds(String userKeyword) {
        if (!StringUtils.hasText(userKeyword)) {
            return null;
        }
        String keyword = userKeyword.trim();
        if (keyword.chars().allMatch(Character::isDigit)) {
            return List.of(Long.parseLong(keyword));
        }
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .select(User::getId)
                        .like(User::getNickname, keyword)
                        .last("LIMIT " + CommonConstant.MAX_PAGE_SIZE))
                .stream().map(User::getId).collect(Collectors.toList());
    }

    /** 批量加载本页涉及用户昵称（避免 N+1） */
    private Map<Long, String> loadNicknames(List<DietRecord> records) {
        Set<Long> ids = records.stream().map(DietRecord::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    /** PO → VO（快照列映射 + 昵称兜底） */
    private AdminDietRecordVO toVO(DietRecord record, Map<Long, String> nicknameMap) {
        AdminDietRecordVO vo = new AdminDietRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setUserNickname(nicknameMap.getOrDefault(record.getUserId(), "用户#" + record.getUserId()));
        vo.setRecordDate(record.getRecordDate());
        vo.setMealType(record.getMealType());
        vo.setFoodName(record.getFoodName());
        vo.setAmount(record.getAmountG());
        vo.setCarb(record.getCarbG());
        vo.setProtein(record.getProteinG());
        vo.setFat(record.getFatG());
        vo.setKcal(record.getKcal());
        vo.setSource(record.getSource());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}

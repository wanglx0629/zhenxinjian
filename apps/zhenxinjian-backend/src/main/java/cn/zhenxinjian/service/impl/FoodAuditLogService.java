package cn.zhenxinjian.service.impl;

import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.enums.FoodAiVerdictEnum;
import cn.zhenxinjian.common.enums.FoodAuditActionEnum;
import cn.zhenxinjian.common.utils.Operators;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.FoodAuditLog;
import cn.zhenxinjian.mapper.FoodAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 食物共建审核流水服务（只追加写留痕，业务上永不修改/删除）
 * 统一封装 SUBMIT/AI_CHECK/APPROVE/REJECT/RESUBMIT/ADMIN_FIX 六类动作的流水写入；
 * AI_CHECK 由系统触发，operatorId 固定 0，verdict/suggestion 作快照字段占位
 * 作者: wanglx
 */
@Service
@RequiredArgsConstructor
public class FoodAuditLogService {

    /** AI/系统操作人 ID（food_audit_log.operator_id = 0 表示系统动作） */
    public static final long SYSTEM_OPERATOR_ID = 0L;

    private final FoodAuditLogMapper foodAuditLogMapper;

    /**
     * 追加一条审核流水（不吞异常，由调用方决定流水失败是否阻塞业务）
     *
     * @param foodId        食物ID
     * @param action        审核动作
     * @param operatorId    操作人ID（AI_CHECK 传 0）
     * @param aiVerdict     AI 结论快照（仅 AI_CHECK 有值）
     * @param aiSuggestion  AI 建议快照（仅 AI_CHECK 有值）
     * @param remark        备注/驳回原因
     * @param snapshotBefore 变更前关键字段 JSON（新增动作为 null）
     * @param snapshotAfter  变更后关键字段 JSON
     */
    public void record(Long foodId, FoodAuditActionEnum action, Long operatorId,
                       FoodAiVerdictEnum aiVerdict, String aiSuggestion, String remark,
                       String snapshotBefore, String snapshotAfter) {
        FoodAuditLog auditLog = new FoodAuditLog();
        auditLog.setFoodId(foodId);
        auditLog.setAction(action.getCode());
        auditLog.setOperatorId(operatorId);
        auditLog.setAiVerdict(aiVerdict == null ? null : aiVerdict.getCode());
        auditLog.setAiSuggestion(aiSuggestion);
        auditLog.setRemark(remark);
        auditLog.setSnapshotBefore(snapshotBefore);
        auditLog.setSnapshotAfter(snapshotAfter);
        auditLog.setStatus(1);
        auditLog.setCreateBy(operatorId == null || operatorId <= SYSTEM_OPERATOR_ID
                ? "system" : Operators.user(operatorId));
        foodAuditLogMapper.insert(auditLog);
    }

    /**
     * 关键字段快照（名称/分类/三宏/能量 kcal+kj/单位/图片）→ JSON 文本
     *
     * @param food 食物实体；null 返回 null（新增动作无前值）
     */
    public String snapshot(Food food) {
        if (food == null) {
            return null;
        }
        Map<String, Object> fields = new LinkedHashMap<>(9);
        fields.put("name", food.getName());
        fields.put("categoryCode", food.getCategoryCode());
        fields.put("carb", food.getCarb());
        fields.put("protein", food.getProtein());
        fields.put("fat", food.getFat());
        fields.put("kcal", food.getKcal());
        fields.put("kj", food.getKj());
        fields.put("unit", food.getUnit());
        fields.put("image", food.getImage());
        return JSONUtil.toJsonStr(fields);
    }
}

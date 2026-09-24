package cn.zhenxinjian.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食物共建审核流水实体（只追加写留痕，业务上永不删除；action 见 FoodAuditActionEnum）
 * 作者: wanglx
 */
@Data
@TableName("food_audit_log")
@Schema(description = "食物共建审核流水实体")
public class FoodAuditLog implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "食物ID（关联 foods.id，不物理删，留痕）")
    private Long foodId;

    @Schema(description = "审核动作：SUBMIT/AI_CHECK/APPROVE/REJECT/RESUBMIT/ADMIN_FIX")
    private String action;

    @Schema(description = "操作人ID（用户或管理员；AI_CHECK 为系统 0）")
    private Long operatorId;

    @Schema(description = "AI结论快照：pass/suspect/reject/none")
    private String aiVerdict;

    @Schema(description = "AI建议快照")
    private String aiSuggestion;

    @Schema(description = "备注/驳回原因")
    private String remark;

    @Schema(description = "变更前关键字段JSON（名称/分类/三宏/kcal/kj/unit/image）")
    private String snapshotBefore;

    @Schema(description = "变更后关键字段JSON")
    private String snapshotAfter;

    @Schema(description = "状态：0停用 1有效（流水恒有效）")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间（动作发生时刻）")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "软删除:0未删 1已删（流水业务上永不删除）")
    private Integer deleteFlag;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}

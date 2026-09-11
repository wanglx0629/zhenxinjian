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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户经期设置实体（每用户活跃唯一由生成列兜底；男性/未开启为空态由服务层判定）
 * 作者: wanglx
 */
@Data
@TableName("user_menstrual")
@Schema(description = "用户经期设置实体")
public class UserMenstrual implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属用户ID（关联 users.id，含游客）")
    private Long userId;

    @Schema(description = "是否开启经期管理：0关 1开")
    private Integer enabled;

    @Schema(description = "末次月经起始日（开启时必填，不得为未来日期）")
    private LocalDate periodStartDate;

    @Schema(description = "周期长度 L（21-35，默认28）")
    private Integer cycleLen;

    @Schema(description = "经期天数 D（3-10，默认5）")
    private Integer periodDays;

    @Schema(description = "状态:0停用 1有效")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "软删除:0未删 1已删")
    private Integer deleteFlag;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}
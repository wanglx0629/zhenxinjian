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
 * 提醒推送日志实体（仅记录真实下发尝试，当日该餐别成功日志存在即不再推送）
 * 作者: wanglx
 */
@Data
@TableName("reminder_send_log")
@Schema(description = "提醒推送日志实体")
public class ReminderSendLog implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属用户ID")
    private Long userId;

    @Schema(description = "提醒日期")
    private LocalDate remindDate;

    @Schema(description = "餐别：1早 2午 3晚（加餐不参与提醒）")
    private Integer mealType;

    @Schema(description = "推送结果：1成功 2失败")
    private Integer sendStatus;

    @Schema(description = "失败原因（微信返回 errcode/errmsg）")
    private String failReason;

    @Schema(description = "使用的订阅消息模板ID")
    private String templateId;

    @Schema(description = "状态：0停用 1有效")
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

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
 * 用户三餐提醒设置实体（总开关+三餐开关/时间+订阅额度，每用户活跃唯一由生成列兜底）
 * 作者: wanglx
 */
@Data
@TableName("user_reminders")
@Schema(description = "用户三餐提醒设置实体")
public class UserReminder implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属用户ID（含游客）")
    private Long userId;

    @Schema(description = "总开关：0关 1开")
    private Integer masterSwitch;

    @Schema(description = "早餐提醒开关：0关 1开")
    private Integer breakfastSwitch;

    @Schema(description = "早餐提醒时间 HH:mm")
    private String breakfastTime;

    @Schema(description = "午餐提醒开关：0关 1开")
    private Integer lunchSwitch;

    @Schema(description = "午餐提醒时间 HH:mm")
    private String lunchTime;

    @Schema(description = "晚餐提醒开关：0关 1开")
    private Integer dinnerSwitch;

    @Schema(description = "晚餐提醒时间 HH:mm")
    private String dinnerTime;

    @Schema(description = "订阅消息剩余额度（授权+1 推送成功-1）")
    private Integer subscribeCredit;

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

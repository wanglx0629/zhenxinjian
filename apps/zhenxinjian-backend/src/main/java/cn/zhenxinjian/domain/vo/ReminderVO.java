package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 提醒设置视图对象（附订阅额度与模板ID，供前端引导授权）
 * 作者: wanglx
 */
@Data
@Schema(description = "提醒设置视图对象")
public class ReminderVO implements Serializable {

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

    @Schema(description = "订阅消息剩余额度（0 且存在开启项时前端引导重新授权）")
    private Integer subscribeCredit;

    @Schema(description = "订阅消息模板ID（wx.requestSubscribeMessage 入参；未配置为空串）")
    private String templateId;
}

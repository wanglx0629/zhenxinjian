package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调碳日志视图（追加写留痕，历史可查）
 * 作者: wanglx
 */
@Data
@Schema(description = "调碳日志视图")
public class AdjustLogVO implements Serializable {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "动作：1下调 2恢复")
    private Integer action;

    @Schema(description = "动作名称：下调/恢复")
    private String actionName;

    @Schema(description = "触发当日体重 kg")
    private Double triggerWeight;

    @Schema(description = "动作发生时间")
    private LocalDateTime createTime;
}
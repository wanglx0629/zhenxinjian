package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 经期设置视图（男性 applicable=false 空态；开启后附当前阶段与上浮值）
 * 作者: wanglx
 */
@Data
@Schema(description = "经期设置视图")
public class MenstrualVO implements Serializable {

    @Schema(description = "是否适用（女性 true；男性 false，界面隐藏经期相关内容）")
    private Boolean applicable;

    @Schema(description = "是否开启经期管理：0关 1开")
    private Integer enabled;

    @Schema(description = "末次月经起始日")
    private LocalDate periodStartDate;

    @Schema(description = "周期长度 L（21-35）")
    private Integer cycleLen;

    @Schema(description = "经期天数 D（3-10）")
    private Integer periodDays;

    @Schema(description = "当前阶段键：menstrual/follicular/ovulation/luteal（未开启/空态为空）")
    private String phaseKey;

    @Schema(description = "当前阶段名称（经期/卵泡期/排卵期/黄体期）")
    private String phaseName;

    @Schema(description = "当前阶段日序 dayIdx（1..L，仅开启时有值）")
    private Integer dayIdx;

    @Schema(description = "碳水上浮 g（仅开启时有值）")
    private Integer carbUplift;

    @Schema(description = "热量上浮 kcal（仅开启时有值）")
    private Integer kcalUplift;
}
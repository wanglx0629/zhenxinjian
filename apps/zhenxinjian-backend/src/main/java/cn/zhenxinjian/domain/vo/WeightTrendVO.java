package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 体重趋势视图：窗口内每日末值点（时间序）+ 体重差
 * 作者: wanglx
 */
@Data
@Schema(description = "体重趋势")
public class WeightTrendVO {

    @Schema(description = "每日末值点（按日期升序，每日一点）")
    private List<Point> points;

    @Schema(description = "体重差 kg = 最早日末值 − 最新日末值（正=下降，负=上升）")
    private Double delta;

    @Schema(description = "窗口起始日期")
    private String startDate;

    @Schema(description = "窗口结束日期")
    private String endDate;

    /**
     * 趋势点
     */
    @Data
    @Schema(description = "体重趋势点")
    public static class Point {

        @Schema(description = "记录日期 YYYY-MM-DD")
        private String date;

        @Schema(description = "体重 kg（当日末值）")
        private Double weight;

        @Schema(description = "当日是否处于平台期")
        private Boolean plateau;
    }
}

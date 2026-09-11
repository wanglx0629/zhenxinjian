package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 每日活跃 VO（趋势折线数据源）
 * 作者: wanglx
 */
@Data
@Schema(description = "每日活跃")
public class DailyActiveVO {

    @Schema(description = "统计日期")
    private LocalDate statDate;

    @Schema(description = "去重活跃用户")
    private Integer dau;

    @Schema(description = "其中游客数")
    private Integer guestDau;

    @Schema(description = "当日新增注册用户")
    private Integer newUser;
}

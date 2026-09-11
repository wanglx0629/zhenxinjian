package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据看板总览 VO（今日实时 + 累计）
 * 作者: wanglx
 */
@Data
@Schema(description = "数据看板总览")
public class StatsOverviewVO {

    @Schema(description = "今日 DAU（实时）")
    private Integer todayDau;

    @Schema(description = "昨日 DAU（聚合表）")
    private Integer yesterdayDau;

    @Schema(description = "近30天 MAU（实时）")
    private Integer mau;

    @Schema(description = "累计注册用户")
    private Long totalUsers;

    @Schema(description = "累计饮食记录")
    private Long totalDietRecords;
}

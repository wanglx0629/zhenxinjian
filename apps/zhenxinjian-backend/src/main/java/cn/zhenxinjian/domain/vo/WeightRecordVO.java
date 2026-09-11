package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 体重记录视图（统一 kg；kg/斤换算由前端按用户单位处理）
 * 作者: wanglx
 */
@Data
@Schema(description = "体重记录视图")
public class WeightRecordVO implements Serializable {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "体重 kg")
    private Double weight;

    @Schema(description = "是否平台期（按近 7 天波动判定）")
    private Boolean plateau;
}
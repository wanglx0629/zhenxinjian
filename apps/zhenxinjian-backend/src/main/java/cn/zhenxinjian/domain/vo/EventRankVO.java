package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 功能事件排行 VO
 * 作者: wanglx
 */
@Data
@Schema(description = "功能事件排行")
public class EventRankVO {

    @Schema(description = "事件码")
    private String eventCode;

    @Schema(description = "事件中文名")
    private String eventName;

    @Schema(description = "事件总次数")
    private Long pv;

    @Schema(description = "去重用户数")
    private Long uv;
}

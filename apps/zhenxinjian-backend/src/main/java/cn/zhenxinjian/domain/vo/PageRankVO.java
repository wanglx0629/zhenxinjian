package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 页面访问排行 VO
 * 作者: wanglx
 */
@Data
@Schema(description = "页面访问排行")
public class PageRankVO {

    @Schema(description = "页面路径")
    private String page;

    @Schema(description = "访问次数")
    private Long pv;

    @Schema(description = "去重用户数")
    private Long uv;
}

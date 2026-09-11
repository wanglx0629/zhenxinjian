package cn.zhenxinjian.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 埋点事件单条入参
 * 作者: wanglx
 */
@Data
@Schema(description = "埋点事件单条入参")
public class TrackEventItemDTO {

    @NotBlank(message = "事件码不能为空")
    @Size(max = 64, message = "事件码过长")
    @Schema(description = "事件码（TrackEventEnum 白名单）")
    private String eventCode;

    @Size(max = 64, message = "页面路径过长")
    @Schema(description = "页面路径（PV/点击类填写）")
    private String page;

    @Schema(description = "扩展字段（关键词/餐别/食物ID等，序列化超512截断）")
    private Map<String, Object> extra;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "端上发生时间（仅记录，不参与统计）")
    private LocalDateTime clientTime;
}

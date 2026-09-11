package cn.zhenxinjian.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 埋点事件批量入参（单批≤50）
 * 作者: wanglx
 */
@Data
@Schema(description = "埋点事件批量入参")
public class TrackEventBatchDTO {

    @NotEmpty(message = "事件列表不能为空")
    @Size(max = 50, message = "单批上报不能超过50条")
    @Valid
    @Schema(description = "事件列表（单批≤50条）")
    private List<TrackEventItemDTO> events;
}

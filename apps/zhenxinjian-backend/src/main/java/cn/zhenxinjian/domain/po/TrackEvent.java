package cn.zhenxinjian.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 埋点明细实体（批量上报落库；统计口径以 create_time 为准）
 * 作者: wanglx
 */
@Data
@TableName("track_event")
@Schema(description = "埋点明细实体")
public class TrackEvent implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID（未登录为空）")
    private Long userId;

    @Schema(description = "用户类型 WECHAT/GUEST（冗余，聚合免JOIN）")
    private String userType;

    @Schema(description = "事件码（TrackEventEnum）")
    private String eventCode;

    @Schema(description = "事件中文名（冗余）")
    private String eventName;

    @Schema(description = "页面路径（PV/点击类填写）")
    private String page;

    @Schema(description = "扩展JSON（关键词/餐别/食物ID等）")
    private String extraJson;

    @Schema(description = "端上发生时间（仅记录，不参与统计）")
    private LocalDateTime clientTime;

    @Schema(description = "状态：0停用 1有效")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间（统计口径时间）")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "软删除:0未删 1已删")
    private Integer deleteFlag;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}

package cn.zhenxinjian.domain.query;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 管理端饮食记录分页查询条件（只读）
 * 作者: wanglx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端饮食记录分页查询")
public class AdminDietRecordQuery extends PageQuery {

    @Size(max = CommonConstant.MAX_KEYWORD_LENGTH, message = ExceptionConstant.KEYWORD_TOO_LONG)
    @Schema(description = "用户关键词（纯数字按用户ID精确，否则昵称模糊）")
    private String userKeyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "记录日期起（yyyy-MM-dd）")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "记录日期止（yyyy-MM-dd）")
    private LocalDate endDate;

    @Min(value = 1, message = ExceptionConstant.DIET_MEAL_TYPE_INVALID)
    @Max(value = 4, message = ExceptionConstant.DIET_MEAL_TYPE_INVALID)
    @Schema(description = "餐别：1早餐 2午餐 3晚餐 4加餐，缺省全部")
    private Integer mealType;
}

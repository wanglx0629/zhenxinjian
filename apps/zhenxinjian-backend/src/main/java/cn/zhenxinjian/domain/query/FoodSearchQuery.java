package cn.zhenxinjian.domain.query;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 食物库分页查询条件（关键字+分类筛选，单页上限 50 由服务端收紧）
 * 作者: wanglx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "食物库分页查询")
public class FoodSearchQuery extends PageQuery {

    @Size(max = CommonConstant.MAX_KEYWORD_LENGTH, message = ExceptionConstant.KEYWORD_TOO_LONG)
    @Schema(description = "关键词（名称/别名模糊，空则返回空集）")
    private String keyword;

    @Size(max = 2, message = "分类编号非法")
    @Schema(description = "分类编号：01-10，缺省全部")
    private String categoryCode;
}

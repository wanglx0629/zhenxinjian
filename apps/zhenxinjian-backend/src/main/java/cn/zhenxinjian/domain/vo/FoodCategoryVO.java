package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 食物分类响应 VO（分类列表项）
 * 作者: wanglx
 */
@Data
@Schema(description = "食物分类响应")
public class FoodCategoryVO {

    @Schema(description = "分类编号：01-10")
    private String code;

    @Schema(description = "分类名称")
    private String name;
}

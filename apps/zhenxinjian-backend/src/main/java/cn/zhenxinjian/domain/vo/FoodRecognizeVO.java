package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 食物拍照识别候选项（每 100g 基准；kcal 由后端按 4/4/9 由宏量重算）
 * 作者: wanglx
 */
@Data
@Schema(description = "食物识别候选项")
public class FoodRecognizeVO {

    @Schema(description = "食物名称（中文，≤20 字）", example = "白米饭")
    private String name;

    @Schema(description = "每 100g 碳水（g）", example = "25.9")
    private Double carb;

    @Schema(description = "每 100g 蛋白质（g）", example = "2.6")
    private Double protein;

    @Schema(description = "每 100g 脂肪（g）", example = "0.3")
    private Double fat;

    @Schema(description = "每 100g 热量（kcal，= 碳水×4+蛋白质×4+脂肪×9）", example = "116.7")
    private Double kcal;
}

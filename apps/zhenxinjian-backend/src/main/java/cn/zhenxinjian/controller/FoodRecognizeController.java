package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.vo.FoodRecognizeVO;
import cn.zhenxinjian.service.impl.AiRecognizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 食物拍照识别控制器（登录态含游客可用）
 * 作者: wanglx
 */
@Tag(name = "食物识别")
@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class FoodRecognizeController {

    private final AiRecognizeService aiRecognizeService;

    @Operation(summary = "拍照识别食物", description = "上传 jpg/jpeg/png/webp（≤10MB），返回候选食物及每 100g 宏量（kcal 按 4/4/9 重算）")
    @PostMapping("/recognize")
    public Result<List<FoodRecognizeVO>> recognize(@RequestPart("file") MultipartFile file) {
        return Result.ok(aiRecognizeService.recognize(UserContext.getUserId(), file));
    }
}

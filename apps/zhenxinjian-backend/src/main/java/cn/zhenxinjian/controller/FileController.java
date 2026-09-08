package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.vo.FileUploadVO;
import cn.zhenxinjian.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储控制器（MinIO + OSS 保底）
 * 作者: wanglx
 */
@Tag(name = "文件存储")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class FileController {

    private final StorageService storageService;

    @Operation(summary = "上传文件（MinIO优先，OSS保底）")
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(storageService.upload(file));
    }

    @Operation(summary = "删除文件")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public Result<Void> delete(@RequestParam String objectKey) {
        storageService.delete(objectKey);
        return Result.ok();
    }
}

package cn.zhenxinjian.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传响应
 * 作者: wanglx
 */
@Data
@Schema(description = "文件上传响应")
public class FileUploadVO {

    @Schema(description = "访问URL")
    private String url;

    @Schema(description = "对象Key")
    private String objectKey;

    @Schema(description = "存储提供方: minio/oss")
    private String provider;

    @Schema(description = "原始文件名")
    private String originalFilename;
}

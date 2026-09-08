package cn.zhenxinjian.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhenxinjian.common.constant.StorageConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.vo.FileUploadVO;
import cn.zhenxinjian.service.StorageService;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 对象存储 Service 实现
 * MinIO 优先，失败自动切换 OSS 保底
 * 作者: wanglx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final ZhenxinjianProperties zhenxinjianProperties;
    private final ObjectProvider<MinioClient> minioClientProvider;
    private final ObjectProvider<OSS> ossClientProvider;

    @Override
    public FileUploadVO upload(MultipartFile file) {
        validateFile(file);
        String objectKey = buildObjectKey(file.getOriginalFilename());
        String contentType = StrUtil.blankToDefault(file.getContentType(), "application/octet-stream");

        if (zhenxinjianProperties.getStorage().getMinio().isEnabled()) {
            try {
                return uploadToMinio(file, objectKey, contentType);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("MinIO 上传失败，尝试 OSS 保底: type={}, msg={}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }

        if (zhenxinjianProperties.getStorage().getOss().isEnabled()) {
            try {
                return uploadToOss(file, objectKey, contentType);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("OSS 上传失败: type={}, msg={}", e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        throw new BusinessException(ExceptionConstant.FILE_UPLOAD_FAIL);
    }

    @Override
    public void delete(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException(ExceptionConstant.FILE_KEY_REQUIRED);
        }
        boolean deleted = false;
        if (zhenxinjianProperties.getStorage().getMinio().isEnabled()) {
            try {
                deleteFromMinio(objectKey);
                deleted = true;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("MinIO 删除失败，尝试 OSS 保底: type={}, msg={}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }
        if (!deleted && zhenxinjianProperties.getStorage().getOss().isEnabled()) {
            try {
                deleteFromOss(objectKey);
                deleted = true;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("OSS 删除失败: type={}, msg={}", e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        if (!deleted) {
            throw new BusinessException(ExceptionConstant.FILE_DELETE_FAIL);
        }
    }

    /**
     * 校验上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ExceptionConstant.FILE_EMPTY);
        }
        long maxBytes = zhenxinjianProperties.getStorage().getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ExceptionConstant.FILE_TOO_LARGE);
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        List<String> allowed = zhenxinjianProperties.getStorage().getAllowedExtensions();
        if (StrUtil.isBlank(ext) || allowed == null || allowed.stream()
                .noneMatch(item -> item.equalsIgnoreCase(ext))) {
            throw new BusinessException(ExceptionConstant.FILE_TYPE_NOT_ALLOWED);
        }
    }

    /**
     * 生成对象 Key
     */
    private String buildObjectKey(String originalFilename) {
        String prefix = StrUtil.blankToDefault(zhenxinjianProperties.getStorage().getPathPrefix(), "upload/");
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String ext = FileUtil.extName(originalFilename).toLowerCase(Locale.ROOT);
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        return prefix + datePath + "/" + IdUtil.simpleUUID() + "." + ext;
    }

    /**
     * 上传到 MinIO
     */
    private FileUploadVO uploadToMinio(MultipartFile file, String objectKey, String contentType) throws Exception {
        MinioClient client = minioClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ExceptionConstant.FILE_UPLOAD_FAIL);
        }
        ZhenxinjianProperties.Storage.Minio minio = zhenxinjianProperties.getStorage().getMinio();
        ensureMinioBucket(client, minio.getBucket());
        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(minio.getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        }
        FileUploadVO vo = new FileUploadVO();
        vo.setUrl(buildMinioUrl(minio, objectKey));
        vo.setObjectKey(objectKey);
        vo.setProvider(StorageConstant.PROVIDER_MINIO);
        vo.setOriginalFilename(file.getOriginalFilename());
        return vo;
    }

    /**
     * 上传到 OSS 保底
     */
    private FileUploadVO uploadToOss(MultipartFile file, String objectKey, String contentType) throws Exception {
        OSS client = ossClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ExceptionConstant.FILE_UPLOAD_FAIL);
        }
        ZhenxinjianProperties.Storage.Oss oss = zhenxinjianProperties.getStorage().getOss();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(contentType);
        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(oss.getBucket(), objectKey, inputStream, metadata);
        }
        FileUploadVO vo = new FileUploadVO();
        vo.setUrl(buildOssUrl(oss, objectKey));
        vo.setObjectKey(objectKey);
        vo.setProvider(StorageConstant.PROVIDER_OSS);
        vo.setOriginalFilename(file.getOriginalFilename());
        return vo;
    }

    /**
     * 从 MinIO 删除
     */
    private void deleteFromMinio(String objectKey) throws Exception {
        MinioClient client = minioClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ExceptionConstant.FILE_DELETE_FAIL);
        }
        String bucket = zhenxinjianProperties.getStorage().getMinio().getBucket();
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    }

    /**
     * 从 OSS 删除
     */
    private void deleteFromOss(String objectKey) {
        OSS client = ossClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ExceptionConstant.FILE_DELETE_FAIL);
        }
        String bucket = zhenxinjianProperties.getStorage().getOss().getBucket();
        client.deleteObject(bucket, objectKey);
    }

    /**
     * 确保 MinIO 桶存在
     */
    private void ensureMinioBucket(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    /**
     * 构建 MinIO 访问地址
     */
    private String buildMinioUrl(ZhenxinjianProperties.Storage.Minio minio, String objectKey) {
        String endpoint = StrUtil.removeSuffix(minio.getEndpoint(), "/");
        return endpoint + "/" + minio.getBucket() + "/" + objectKey;
    }

    /**
     * 构建 OSS 访问地址
     */
    private String buildOssUrl(ZhenxinjianProperties.Storage.Oss oss, String objectKey) {
        if (StrUtil.isNotBlank(oss.getCustomDomain())) {
            String domain = StrUtil.removeSuffix(oss.getCustomDomain(), "/");
            return domain + "/" + objectKey;
        }
        return "https://" + oss.getBucket() + "." + oss.getEndpoint() + "/" + objectKey;
    }
}

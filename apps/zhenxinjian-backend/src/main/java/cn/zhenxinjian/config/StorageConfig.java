package cn.zhenxinjian.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储客户端配置（MinIO + OSS）
 * 作者: luote (luote) - https://luote996.cn
 */
@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final ZhenxinjianProperties luoteProperties;

    @Bean
    @ConditionalOnProperty(prefix = "zhenxinjian.storage.minio", name = "enabled", havingValue = "true")
    public MinioClient minioClient() {
        ZhenxinjianProperties.Storage.Minio minio = luoteProperties.getStorage().getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "zhenxinjian.storage.oss", name = "enabled", havingValue = "true")
    public OSS ossClient() {
        ZhenxinjianProperties.Storage.Oss oss = luoteProperties.getStorage().getOss();
        return new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
    }
}

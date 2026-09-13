package cn.zhenxinjian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义配置属性
 * 作者: wanglx
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhenxinjian")
public class ZhenxinjianProperties {

    private Jwt jwt = new Jwt();
    private Redis redis = new Redis();
    private Cache cache = new Cache();
    private Security security = new Security();
    private Storage storage = new Storage();

    @Data
    public static class Jwt {
        /** JWT 密钥 */
        private String secret;
        /** 过期时间（分钟） */
        private long expireMinutes;
        /** 续期阈值（分钟，剩余时间少于此值则续期） */
        private long refreshThresholdMinutes;
    }

    @Data
    public static class Redis {
        /** Token 缓存前缀 */
        private String tokenPrefix;
        /** 验证码缓存前缀 */
        private String captchaPrefix;
        /** 登录失败计数前缀 */
        private String loginFailPrefix;
        /** 登录试错上限（达到后锁定，默认 10） */
        private int loginFailMax = 10;
        /** 登录失败计数 TTL（分钟，默认 15） */
        private long loginFailTtlMinutes = 15;
        /** 埋点限流计数前缀 */
        private String trackRatePrefix;
        /** 埋点上报限流阈值（批次/分钟/维度，默认 30；端上正常水位 ≤6 批次/分钟，5 倍余量） */
        private int trackRateLimitPerMinute = 30;
    }

    @Data
    public static class Cache {
        /** 是否启用启动缓存预热 */
        private boolean warmupEnabled = true;
        /** 单条缓存最大值（字节），防止大 key */
        private int maxValueBytes = 51200;
    }

    @Data
    public static class Security {
        /** 白名单 URL */
        private List<String> permitUrls;
        /** CORS 允许来源（生产环境禁止 *） */
        private List<String> corsAllowedOrigins;
    }

    @Data
    public static class Storage {
        /** 对象路径前缀 */
        private String pathPrefix;
        /** 单文件最大体积（MB） */
        private long maxSizeMb;
        /** 允许上传的扩展名 */
        private List<String> allowedExtensions;
        /** MinIO 配置 */
        private Minio minio = new Minio();
        /** 阿里云 OSS 保底配置 */
        private Oss oss = new Oss();

        @Data
        public static class Minio {
            /** 是否启用 */
            private boolean enabled;
            /** 服务地址 */
            private String endpoint;
            /** AccessKey */
            private String accessKey;
            /** SecretKey */
            private String secretKey;
            /** 存储桶 */
            private String bucket;
        }

        @Data
        public static class Oss {
            /** 是否启用（MinIO 失败时保底） */
            private boolean enabled;
            /** Endpoint，如 oss-cn-hangzhou.aliyuncs.com */
            private String endpoint;
            /** AccessKeyId */
            private String accessKeyId;
            /** AccessKeySecret */
            private String accessKeySecret;
            /** 存储桶 */
            private String bucket;
            /** 自定义访问域名（CDN），为空则使用默认域名 */
            private String customDomain;
        }
    }
}

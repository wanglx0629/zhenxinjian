package cn.zhenxinjian.common.cache;

import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

/**
 * 缓存大 key 校验
 * 作者: wanglx
 */
@Component
@RequiredArgsConstructor
public class CacheValueGuard {

    private final ZhenxinjianProperties zhenxinjianProperties;

    /**
     * 校验缓存值体积，避免大 key 打满 Redis。
     * 以 JDK 序列化字节数估算，与 JetCache valueEncoder=java 实际编码对齐。
     */
    public void check(Object value) {
        if (value == null) {
            return;
        }
        int maxBytes = zhenxinjianProperties.getCache().getMaxValueBytes();
        byte[] bytes = SerializationUtils.serialize(value);
        if (bytes.length > maxBytes) {
            throw new BusinessException(ExceptionConstant.CACHE_VALUE_TOO_LARGE);
        }
    }
}
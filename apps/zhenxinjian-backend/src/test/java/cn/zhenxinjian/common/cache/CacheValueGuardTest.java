package cn.zhenxinjian.common.cache;

import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 缓存大 key 校验单元测试
 * 作者: wanglx
 */
class CacheValueGuardTest {

    private ZhenxinjianProperties propertiesWith(int maxBytes) {
        ZhenxinjianProperties properties = new ZhenxinjianProperties();
        properties.getCache().setMaxValueBytes(maxBytes);
        return properties;
    }

    /** 空值不走体积校验 */
    @Test
    void nullValue_isIgnored() {
        assertDoesNotThrow(() -> new CacheValueGuard(propertiesWith(16)).check(null));
    }

    /** 正常小对象放行 */
    @Test
    void smallValue_passes() {
        assertDoesNotThrow(() -> new CacheValueGuard(propertiesWith(51200)).check("ok"));
    }

    /** 超大值触发拦截（JDK 序列化字节与 valueEncoder=java 对齐） */
    @Test
    void oversizedValue_throws() {
        CacheValueGuard guard = new CacheValueGuard(propertiesWith(16));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.check("x".repeat(128)));
        assertEquals(ExceptionConstant.CACHE_VALUE_TOO_LARGE, ex.getMessage());
    }
}
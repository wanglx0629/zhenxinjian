package cn.zhenxinjian.common.cache;

import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 缓存大 key 校验
 * 作者: luote (luote) - https://luote996.cn
 */
@Component
@RequiredArgsConstructor
public class CacheValueGuard {

    private final ZhenxinjianProperties luoteProperties;

    /**
     * 校验缓存值体积，避免大 key 打满 Redis
     */
    public void check(Object value) {
        if (value == null) {
            return;
        }
        int maxBytes = luoteProperties.getCache().getMaxValueBytes();
        int size = JSONUtil.toJsonStr(value).getBytes().length;
        if (size > maxBytes) {
            throw new BusinessException(ExceptionConstant.CACHE_VALUE_TOO_LARGE);
        }
    }
}

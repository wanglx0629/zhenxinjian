package cn.zhenxinjian.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * CORS 允许来源解析公共工具（env 逗号分隔单元素 → 扁平列表；不过滤，语义由各调用方裁定）
 * 作者: wanglx
 */
public final class CorsOrigins {

    private CorsOrigins() {
    }

    /**
     * 解析配置值为扁平来源列表
     *
     * @param origins 原始配置值（支持单元素逗号分隔形式）
     * @return 扁平列表（空入参返回空列表；元素原样保留，含空白/"*"，由调用方按需过滤或检测）
     */
    public static List<String> parse(List<String> origins) {
        if (CollUtil.isEmpty(origins)) {
            return List.of();
        }
        if (origins.size() == 1 && StrUtil.contains(origins.get(0), ',')) {
            return StrUtil.splitTrim(origins.get(0), ',');
        }
        return origins;
    }
}

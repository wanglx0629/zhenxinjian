package cn.zhenxinjian.common.utils;

/**
 * LIMIT 片段安全构建工具：收敛 .last("LIMIT " + var) 字符串拼接，统一有界封顶防无界 selectList
 * 作者: wanglx
 */
public final class SqlLimit {

    private SqlLimit() {
    }

    /**
     * 有界 LIMIT：limit 为空或 ≤0 返回空串（语义同「不限制」）；否则按 cap 封顶后返回 "LIMIT n"。
     *
     * @param limit 请求条数（可空）
     * @param cap   条数上界（>0）
     * @return 形如 "LIMIT 50" 的 SQL 片段；不限制时返回空串
     */
    public static String bounded(Integer limit, int cap) {
        if (limit == null || limit <= 0) {
            return "";
        }
        return "LIMIT " + Math.min(limit, cap);
    }

    /**
     * 固定 LIMIT：受控常量/已校验 int 场景（仅接受整数，字符串注入路径被类型系统阻断）。
     *
     * @param n 条数（非正返回空串）
     */
    public static String fixed(long n) {
        return n <= 0 ? "" : "LIMIT " + n;
    }
}
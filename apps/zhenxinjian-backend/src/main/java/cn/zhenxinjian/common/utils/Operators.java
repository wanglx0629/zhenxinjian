package cn.zhenxinjian.common.utils;

/**
 * 审计列操作者标识公共工具（create_by / update_by 统一口径）
 * 作者: wanglx
 */
public final class Operators {

    private Operators() {
    }

    /** 用户操作者标识（"user:{userId}"） */
    public static String user(Long userId) {
        return "user:" + userId;
    }
}

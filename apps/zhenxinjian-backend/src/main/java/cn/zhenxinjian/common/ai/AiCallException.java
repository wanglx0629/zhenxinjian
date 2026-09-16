package cn.zhenxinjian.common.ai;

import lombok.Getter;

/**
 * AI 模型调用异常（携带 HTTP 状态码与「是否可转移」标记）
 * 可转移错误（429/5xx/404/408/网络超时/结构异常）允许故障转移到备用模型；
 * 不可转移错误（401/403/400/422 及其余 4xx）立即失败，不切换、不熔断。
 * 作者: wanglx
 */
@Getter
public class AiCallException extends RuntimeException {

    /** 上游 HTTP 状态码（网络/结构异常无法取码时为 0） */
    private final int httpStatus;

    /** 是否可转移到备用模型 */
    private final boolean transferable;

    public AiCallException(String message, int httpStatus, boolean transferable) {
        super(message);
        this.httpStatus = httpStatus;
        this.transferable = transferable;
    }

    public AiCallException(String message, Throwable cause, int httpStatus, boolean transferable) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.transferable = transferable;
    }

    /**
     * 按 HTTP 状态码分类：429/5xx/404/408 可转移；401/403/400/422 及其余 4xx 不可转移
     */
    public static boolean isTransferableStatus(int httpStatus) {
        if (httpStatus == 429 || httpStatus == 404 || httpStatus == 408) {
            return true;
        }
        return httpStatus >= 500 && httpStatus < 600;
    }
}

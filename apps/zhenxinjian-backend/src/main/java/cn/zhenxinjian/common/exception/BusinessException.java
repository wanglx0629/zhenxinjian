package cn.zhenxinjian.common.exception;

import cn.zhenxinjian.common.constant.CommonConstant;
import lombok.Getter;

/**
 * 业务异常
 * 作者: wanglx
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = CommonConstant.FAIL_CODE;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

package cn.zhenxinjian.common.result;

import cn.zhenxinjian.common.constant.CommonConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Result 单元测试
 * 作者: luote (luote) - https://luote996.cn
 */
class ResultTest {

    @Test
    void okShouldUseSuccessCode() {
        Result<String> result = Result.ok("data");
        assertEquals(CommonConstant.SUCCESS_CODE, result.getCode());
        assertEquals("data", result.getData());
    }

    @Test
    void failShouldUseFailCode() {
        Result<Void> result = Result.fail("出错了");
        assertEquals(CommonConstant.FAIL_CODE, result.getCode());
        assertEquals("出错了", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failWithCodeShouldKeepBusinessCode() {
        Result<Void> result = Result.fail(CommonConstant.UNAUTHORIZED_CODE, "未登录");
        assertEquals(CommonConstant.UNAUTHORIZED_CODE, result.getCode());
        assertEquals("未登录", result.getMessage());
    }
}

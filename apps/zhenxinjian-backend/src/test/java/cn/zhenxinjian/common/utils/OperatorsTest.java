package cn.zhenxinjian.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Operators 审计操作者标识工具单元测试
 * 覆盖：user 前缀拼接、null 透传
 * 作者: wanglx
 */
class OperatorsTest {

    @Test
    void userShouldPrefixUserIdWithUserNamespace() {
        assertEquals("user:1024", Operators.user(1024L));
    }

    @Test
    void userShouldHandleZeroAndNull() {
        assertEquals("user:0", Operators.user(0L));
        assertEquals("user:null", Operators.user(null));
    }
}

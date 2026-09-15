package cn.zhenxinjian.common.sensitive;

import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 敏感词过滤组件单元测试（内置命中/追加/排除/开关/空白/大小写/refresh/fail-open）
 * 作者: wanglx
 */
class SensitiveWordFilterTest {

    private ConfigService configService;
    private SensitiveWordFilter filter;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        stubConfig(true, List.of(), List.of());
        filter = new SensitiveWordFilter(configService);
        filter.refresh();
    }

    /** 开关 + 追加词 + 排除词桩 */
    private void stubConfig(boolean enabled, List<String> extra, List<String> exclude) {
        when(configService.getBoolean(
                eq(ProjectConfigKeyConstant.SENSITIVE_FILTER_ENABLED), anyBoolean()))
                .thenReturn(enabled);
        when(configService.getStringList(ProjectConfigKeyConstant.SENSITIVE_FILTER_EXTRA_WORDS))
                .thenReturn(extra);
        when(configService.getStringList(ProjectConfigKeyConstant.SENSITIVE_FILTER_EXCLUDE_WORDS))
                .thenReturn(exclude);
    }

    /** 场景：内置词库命中（check 抛统一业务异常） */
    @Test
    void check_builtinHit_throws() {
        assertThrows(BusinessException.class, () -> filter.check("这是赌博网站快来"));
    }

    /** 场景：正常内容不命中 */
    @Test
    void contains_normalContent_false() {
        assertFalse(filter.contains("今天吃鸡胸肉沙拉"));
    }

    /** 场景：追加词生效（配置后命中） */
    @Test
    void refresh_extraWordAdded_hits() {
        stubConfig(true, List.of("违禁水果刀"), List.of());
        filter.refresh();
        assertTrue(filter.contains("出售违禁水果刀"));
    }

    /** 场景：排除词豁免（内置词被差集移除；用独立成词的 pk10 验证，避免同前缀短词干扰） */
    @Test
    void refresh_excludeWordGranted_passes() {
        stubConfig(true, List.of(), List.of("pk10"));
        filter.refresh();
        assertFalse(filter.contains("PK10 玩吗"));
    }

    /** 场景：开关关闭 → 全部放行 */
    @Test
    void check_switchOff_passes() {
        stubConfig(false, List.of(), List.of());
        filter.refresh();
        assertFalse(filter.contains("赌博网站"));
        assertDoesNotThrow(() -> filter.check("赌博网站"));
    }

    /** 场景：空白/纯符号文本放行 */
    @Test
    void check_blankText_passes() {
        assertDoesNotThrow(() -> filter.check(null));
        assertDoesNotThrow(() -> filter.check("   "));
        assertFalse(filter.contains(""));
    }

    /** 场景：大小写不敏感（词库小写，文本大写命中） */
    @Test
    void contains_caseInsensitive_true() {
        assertTrue(filter.contains("PK10 玩吗"));
    }

    /** 场景：refresh 后词集按新配置重建 */
    @Test
    void refresh_rebuildsWordSet() {
        stubConfig(true, List.of("新追加词"), List.of());
        filter.refresh();
        assertTrue(filter.contains("新追加词"));

        stubConfig(true, List.of(), List.of());
        filter.refresh();
        assertFalse(filter.contains("新追加词"));
    }

    /** 场景：配置读取失败 → fail-open 放行不抛错 */
    @Test
    void refresh_configFailure_failsOpen() {
        ConfigService broken = mock(ConfigService.class);
        when(broken.getBoolean(anyString(), anyBoolean())).thenThrow(new RuntimeException("db down"));
        SensitiveWordFilter brokenFilter = new SensitiveWordFilter(broken);

        assertDoesNotThrow(brokenFilter::refresh);
        assertFalse(brokenFilter.contains("赌博网站"));
        assertDoesNotThrow(() -> brokenFilter.check("赌博网站"));
    }
}

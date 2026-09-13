package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.mapper.TrackEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 埋点事件服务单元测试（毒性隔离：非法码剔除返回、合法码落库；未登录允许；extra 截断；
 * 限流：阈值内放行 / 超限 429 / 窗口过期恢复 / 维度选择）
 * 作者: wanglx
 */
class TrackEventServiceTest {

    private TrackEventMapper trackEventMapper;
    private RedisUtils redisUtils;
    private TrackEventService trackEventService;

    @BeforeEach
    void setUp() {
        trackEventMapper = mock(TrackEventMapper.class);
        redisUtils = mock(RedisUtils.class);
        // 真实属性对象：trackRateLimitPerMinute 默认 30
        trackEventService = new TrackEventService(trackEventMapper, redisUtils, new ZhenxinjianProperties());
        when(redisUtils.incrementTrackRate(anyString())).thenReturn(1L);
    }

    private TrackEventItemDTO item(String code) {
        TrackEventItemDTO dto = new TrackEventItemDTO();
        dto.setEventCode(code);
        return dto;
    }

    /** 场景：合法事件批量落库，event_name 取枚举 desc，无剔除码 */
    @Test
    void saveBatch_validEvents_insertWithEnumName() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("page_view")));

        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch);

        assertTrue(rejected.isEmpty());
        verify(trackEventMapper).insert(anyList());
    }

    /** 场景：混批（合法+非法）→ 毒性隔离：合法落库、非法码返回，不再整批拒收 */
    @Test
    void saveBatch_mixedBatch_poisonIsolated() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("hack_event")));

        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch);

        assertEquals(List.of("hack_event"), rejected);
        verify(trackEventMapper).insert(org.mockito.ArgumentMatchers.<List<TrackEvent>>argThat(
                list -> list.size() == 1 && "record_add".equals(list.get(0).getEventCode())));
    }

    /** 场景：全部非法 → 一条不落库，剔除码去重保序返回 */
    @Test
    void saveBatch_allInvalid_skipInsertAndReturnCodes() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("hack_a"), item("hack_b"), item("hack_a")));

        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch);

        assertEquals(List.of("hack_a", "hack_b"), rejected);
        verify(trackEventMapper, never()).insert(anyList());
    }

    /** 场景：未登录上报（userId/userType 为空）→ 允许落库 */
    @Test
    void saveBatch_nullUser_allowed() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("login_guest")));

        List<String> rejected = trackEventService.saveBatch(null, null, "127.0.0.1", batch);

        assertTrue(rejected.isEmpty());
        verify(trackEventMapper).insert(anyList());
    }

    /** 场景：extra 超 512 字符 → 截断落库不报错 */
    @Test
    void saveBatch_extraTooLong_truncated() {
        TrackEventItemDTO item = item("food_search");
        item.setExtra(java.util.Map.of("keyword", "x".repeat(600)));
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item));

        trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch);

        verify(trackEventMapper).insert(org.mockito.ArgumentMatchers.<List<TrackEvent>>argThat(
                list -> list.size() == 1
                        && list.get(0).getExtraJson() != null
                        && list.get(0).getExtraJson().length() <= CommonConstant.TRACK_EXTRA_MAX_LENGTH));
    }

    /** 场景：窗口内计数超阈值（>30）→ 抛 429 不落库（端上按可重试失败整批保留） */
    @Test
    void saveBatch_overLimit_throws429() {
        when(redisUtils.incrementTrackRate(anyString())).thenReturn(31L);
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("page_view")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch));

        assertEquals(CommonConstant.TOO_MANY_REQUESTS_CODE, ex.getCode());
        verify(trackEventMapper, never()).insert(anyList());
    }

    /** 场景：窗口过期 key 消失计数从 1 重计 → 恢复放行（上次超限不影响下一窗口） */
    @Test
    void saveBatch_windowExpired_recovers() {
        when(redisUtils.incrementTrackRate(anyString())).thenReturn(31L).thenReturn(1L);
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("page_view")));

        assertThrows(BusinessException.class,
                () -> trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch));
        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", "127.0.0.1", batch);

        assertTrue(rejected.isEmpty());
        verify(trackEventMapper).insert(anyList());
    }

    /** 场景：登录用户按 u:{userId} 维度计数（同 IP 多用户互不挤占） */
    @Test
    void saveBatch_loggedIn_usesUserDimension() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("page_view")));

        trackEventService.saveBatch(42L, "WECHAT", "10.0.0.1", batch);

        verify(redisUtils).incrementTrackRate("u:42");
    }

    /** 场景：未登录按 ip:{clientIp} 维度计数（游客/引导页上报） */
    @Test
    void saveBatch_guest_usesIpDimension() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("login_guest")));

        trackEventService.saveBatch(null, null, "203.0.113.8", batch);

        verify(redisUtils).incrementTrackRate("ip:203.0.113.8");
    }
}

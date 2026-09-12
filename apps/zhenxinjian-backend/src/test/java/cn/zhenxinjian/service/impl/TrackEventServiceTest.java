package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.mapper.TrackEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 埋点事件服务单元测试（毒性隔离：非法码剔除返回、合法码落库；未登录允许；extra 截断）
 * 作者: wanglx
 */
class TrackEventServiceTest {

    private TrackEventMapper trackEventMapper;
    private TrackEventService trackEventService;

    @BeforeEach
    void setUp() {
        trackEventMapper = mock(TrackEventMapper.class);
        trackEventService = new TrackEventService(trackEventMapper);
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

        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", batch);

        assertTrue(rejected.isEmpty());
        verify(trackEventMapper).insert(anyList());
    }

    /** 场景：混批（合法+非法）→ 毒性隔离：合法落库、非法码返回，不再整批拒收 */
    @Test
    void saveBatch_mixedBatch_poisonIsolated() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("hack_event")));

        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", batch);

        assertEquals(List.of("hack_event"), rejected);
        verify(trackEventMapper).insert(org.mockito.ArgumentMatchers.<List<TrackEvent>>argThat(
                list -> list.size() == 1 && "record_add".equals(list.get(0).getEventCode())));
    }

    /** 场景：全部非法 → 一条不落库，剔除码去重保序返回 */
    @Test
    void saveBatch_allInvalid_skipInsertAndReturnCodes() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("hack_a"), item("hack_b"), item("hack_a")));

        List<String> rejected = trackEventService.saveBatch(1L, "WECHAT", batch);

        assertEquals(List.of("hack_a", "hack_b"), rejected);
        verify(trackEventMapper, never()).insert(anyList());
    }

    /** 场景：未登录上报（userId/userType 为空）→ 允许落库 */
    @Test
    void saveBatch_nullUser_allowed() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("login_guest")));

        List<String> rejected = trackEventService.saveBatch(null, null, batch);

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

        trackEventService.saveBatch(1L, "WECHAT", batch);

        verify(trackEventMapper).insert(org.mockito.ArgumentMatchers.<List<TrackEvent>>argThat(
                list -> list.size() == 1
                        && list.get(0).getExtraJson() != null
                        && list.get(0).getExtraJson().length() <= CommonConstant.TRACK_EXTRA_MAX_LENGTH));
    }
}

package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.mapper.TrackEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 埋点事件服务单元测试（白名单整批校验 40901 / 未登录允许 / 合法批量落库）
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

    /** 场景：合法事件批量落库，event_name 取枚举 desc */
    @Test
    void saveBatch_validEvents_insertWithEnumName() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("page_view")));

        trackEventService.saveBatch(1L, "WECHAT", batch);

        verify(trackEventMapper).insert(anyList());
    }

    /** 场景：任一事件码非法 → 整批拒收 40901，一条都不落库 */
    @Test
    void saveBatch_invalidCode_rejectWholeBatch40901() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("hack_event")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> trackEventService.saveBatch(1L, "WECHAT", batch));

        assertEquals(CommonConstant.TRACK_EVENT_INVALID_CODE, ex.getCode());
        verify(trackEventMapper, never()).insert(anyList());
    }

    /** 场景：未登录上报（userId/userType 为空）→ 允许落库 */
    @Test
    void saveBatch_nullUser_allowed() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("login_guest")));

        assertDoesNotThrow(() -> trackEventService.saveBatch(null, null, batch));

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

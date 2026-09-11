package cn.zhenxinjian.service.impl;

import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.TrackEventEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.mapper.TrackEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 埋点事件服务（白名单校验 + 批量落库；埋点天然允许少量重复，不做严格幂等）
 * 作者: wanglx
 *
 * 口径：eventCode 白名单整批校验，任一非法整批拒收 40901；extra 序列化超 512 截断；
 * userId/userType 可为空（未登录引导页上报）；统计时间一律以 create_time 为准
 */
@Service
@RequiredArgsConstructor
public class TrackEventService {

    private final TrackEventMapper trackEventMapper;

    /**
     * 批量保存埋点事件
     *
     * @param userId   当前用户ID（未登录为 null）
     * @param userType 用户类型 WECHAT/GUEST（未登录为 null）
     * @param dto      批量事件入参
     */
    public void saveBatch(Long userId, String userType, TrackEventBatchDTO dto) {
        List<TrackEvent> entities = new ArrayList<>(dto.getEvents().size());
        for (TrackEventItemDTO item : dto.getEvents()) {
            TrackEventEnum event = TrackEventEnum.of(item.getEventCode());
            if (event == null) {
                throw new BusinessException(CommonConstant.TRACK_EVENT_INVALID_CODE,
                        ExceptionConstant.TRACK_EVENT_INVALID);
            }
            TrackEvent entity = new TrackEvent();
            entity.setUserId(userId);
            entity.setUserType(userType);
            entity.setEventCode(event.getCode());
            entity.setEventName(event.getDesc());
            entity.setPage(item.getPage());
            entity.setExtraJson(toExtraJson(item));
            entity.setClientTime(item.getClientTime());
            entity.setStatus(1);
            entity.setCreateBy(userId != null ? String.valueOf(userId) : CommonConstant.CREATE_BY_SYSTEM);
            entities.add(entity);
        }
        trackEventMapper.insert(entities);
    }

    /** extra 序列化并限长 512 */
    private String toExtraJson(TrackEventItemDTO item) {
        if (item.getExtra() == null || item.getExtra().isEmpty()) {
            return null;
        }
        String json = JSONUtil.toJsonStr(item.getExtra());
        if (json.length() > CommonConstant.TRACK_EXTRA_MAX_LENGTH) {
            return json.substring(0, CommonConstant.TRACK_EXTRA_MAX_LENGTH);
        }
        return json;
    }
}

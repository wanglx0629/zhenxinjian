package cn.zhenxinjian.service.impl;

import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.enums.TrackEventEnum;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.mapper.TrackEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 埋点事件服务（白名单校验 + 毒性隔离批量落库；埋点天然允许少量重复，不做严格幂等）
 * 作者: wanglx
 *
 * 口径：eventCode 白名单逐条校验——非法条目剔除不入库（毒消息隔离，永久失败），合法条目正常落库，
 * 接口恒 200 并在返回值携带被剔除的非法事件码（去重保序），由端上永久剔除不再重试；
 * 网络/5xx 类失败属可重试失败，端上整批保留。extra 序列化超 512 截断；
 * userId/userType 可为空（未登录引导页上报）；统计时间一律以 create_time 为准
 */
@Service
@RequiredArgsConstructor
public class TrackEventService {

    private final TrackEventMapper trackEventMapper;

    /**
     * 批量保存埋点事件（毒性隔离：非法事件码剔除，合法事件落库）
     *
     * @param userId   当前用户ID（未登录为 null）
     * @param userType 用户类型 WECHAT/GUEST（未登录为 null）
     * @param dto      批量事件入参
     * @return 被剔除的非法事件码列表（去重保序；空列表 = 全部接收）
     */
    public List<String> saveBatch(Long userId, String userType, TrackEventBatchDTO dto) {
        List<TrackEvent> entities = new ArrayList<>(dto.getEvents().size());
        Set<String> rejectedCodes = new LinkedHashSet<>();
        for (TrackEventItemDTO item : dto.getEvents()) {
            TrackEventEnum event = TrackEventEnum.of(item.getEventCode());
            if (event == null) {
                // 毒条目隔离：永久失败，不入库不阻塞同批合法事件
                rejectedCodes.add(item.getEventCode());
                continue;
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
        if (!entities.isEmpty()) {
            trackEventMapper.insert(entities);
        }
        return List.copyOf(rejectedCodes);
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

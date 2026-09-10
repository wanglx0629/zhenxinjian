package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.ReminderSendStatusEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.WxMaConfiguration;
import cn.zhenxinjian.domain.dto.ReminderSaveDTO;
import cn.zhenxinjian.domain.po.ReminderSendLog;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.domain.vo.ReminderVO;
import cn.zhenxinjian.mapper.ReminderSendLogMapper;
import cn.zhenxinjian.mapper.UserReminderMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * 三餐提醒设置服务（每用户活跃唯一；首次查询默认值落库；订阅授权上报幂等）
 * 作者: wanglx
 *
 * 口径：默认全开 08:30/12:00/18:30（MVP v1 §提醒）；时间须 24 小时制 HH:mm，非法抛 40701；
 * 关闭总开关保留三餐既有配置（design D1 整体保存但值全量覆盖，保留语义由前端回读值提交保证——
 * 本服务按提交值整体覆盖，前端关闭总开关时回传既有三餐值即可）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    /** 提醒时间格式：24 小时制 HH:mm */
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    /** 默认早餐时间 */
    private static final String DEFAULT_BREAKFAST_TIME = "08:30";

    /** 默认午餐时间 */
    private static final String DEFAULT_LUNCH_TIME = "12:00";

    /** 默认晚餐时间 */
    private static final String DEFAULT_DINNER_TIME = "18:30";

    /** 订阅上报幂等键前缀（同分钟内重复上报只记一次额度） */
    private static final String SUBSCRIBE_REPORT_KEY_PREFIX = "zhenxinjian:reminder:subscribe:";

    /** 订阅上报幂等窗口 */
    private static final Duration SUBSCRIBE_REPORT_IDEMPOTENT_TTL = Duration.ofMinutes(1);

    private final UserReminderMapper userReminderMapper;

    private final ReminderSendLogMapper reminderSendLogMapper;

    private final WxMaConfiguration.WxMaProperties wxMaProperties;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 查询当前用户提醒设置（无记录按默认值落库）
     *
     * @param userId 当前用户ID
     * @return 设置视图（附订阅额度与模板ID）
     */
    public ReminderVO getOrCreate(Long userId) {
        return toVO(getOrCreateEntity(userId));
    }

    /**
     * 整体保存提醒设置（总开关 + 三餐开关/时间）
     *
     * @param userId 当前用户ID
     * @param dto    保存入参
     * @return 保存后的设置视图
     */
    public ReminderVO save(Long userId, ReminderSaveDTO dto) {
        checkTime(dto.getBreakfastTime());
        checkTime(dto.getLunchTime());
        checkTime(dto.getDinnerTime());
        UserReminder entity = getOrCreateEntity(userId);
        entity.setMasterSwitch(dto.getMasterSwitch());
        entity.setBreakfastSwitch(dto.getBreakfastSwitch());
        entity.setBreakfastTime(dto.getBreakfastTime());
        entity.setLunchSwitch(dto.getLunchSwitch());
        entity.setLunchTime(dto.getLunchTime());
        entity.setDinnerSwitch(dto.getDinnerSwitch());
        entity.setDinnerTime(dto.getDinnerTime());
        userReminderMapper.updateById(entity);
        log.info("[ReminderService] 提醒设置保存: userId={}, master={}", userId, dto.getMasterSwitch());
        return toVO(entity);
    }

    /**
     * 订阅消息授权上报：额度 +1（同分钟内重复上报幂等去重，防前端重复调用刷额度）
     *
     * @param userId 当前用户ID
     */
    public void reportSubscribe(Long userId) {
        String key = SUBSCRIBE_REPORT_KEY_PREFIX + userId;
        Boolean firstReport = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", SUBSCRIBE_REPORT_IDEMPOTENT_TTL);
        if (Boolean.FALSE.equals(firstReport)) {
            log.info("[ReminderService] 同分钟重复上报，幂等跳过: userId={}", userId);
            return;
        }
        getOrCreateEntity(userId);
        userReminderMapper.update(null, Wrappers.<UserReminder>lambdaUpdate()
                .setSql("subscribe_credit = subscribe_credit + 1")
                .eq(UserReminder::getUserId, userId));
        log.info("[ReminderService] 订阅授权上报，额度+1: userId={}", userId);
    }

    /**
     * 推送成功回调：同事务扣额度 + 写成功日志（额度条件更新防并发扣穿）
     *
     * @param userId     用户ID
     * @param mealType   餐别（1早 2午 3晚）
     * @param remindDate 提醒日期
     * @param templateId 使用的模板ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void onPushSuccess(Long userId, Integer mealType, LocalDate remindDate, String templateId) {
        userReminderMapper.update(null, Wrappers.<UserReminder>lambdaUpdate()
                .setSql("subscribe_credit = subscribe_credit - 1")
                .eq(UserReminder::getUserId, userId)
                .gt(UserReminder::getSubscribeCredit, 0));
        insertLog(userId, mealType, remindDate, ReminderSendStatusEnum.SUCCESS, null, templateId);
    }

    /**
     * 推送失败回调：仅写失败日志（不扣额度、不当次重试，PRD 静默降级口径）
     *
     * @param userId     用户ID
     * @param mealType   餐别（1早 2午 3晚）
     * @param remindDate 提醒日期
     * @param templateId 使用的模板ID
     * @param failReason 失败原因（errcode/errmsg，超长截断）
     */
    public void onPushFail(Long userId, Integer mealType, LocalDate remindDate, String templateId, String failReason) {
        insertLog(userId, mealType, remindDate, ReminderSendStatusEnum.FAIL, failReason, templateId);
    }

    /**
     * 写推送日志（仅真实下发尝试才调用；跳过场景不写日志）
     *
     * @param userId     用户ID
     * @param mealType   餐别
     * @param remindDate 提醒日期
     * @param sendStatus 推送结果枚举
     * @param failReason 失败原因（成功时传 null）
     * @param templateId 模板ID
     */
    private void insertLog(Long userId, Integer mealType, LocalDate remindDate,
                           ReminderSendStatusEnum sendStatus, String failReason, String templateId) {
        ReminderSendLog entity = new ReminderSendLog();
        entity.setUserId(userId);
        entity.setRemindDate(remindDate);
        entity.setMealType(mealType);
        entity.setSendStatus(sendStatus.getCode());
        entity.setFailReason(failReason != null && failReason.length() > 255
                ? failReason.substring(0, 255) : failReason);
        entity.setTemplateId(templateId);
        entity.setStatus(1);
        reminderSendLogMapper.insert(entity);
    }

    /**
     * 读取用户设置实体，不存在时按默认值落库（全开关开 + 默认时间 + 额度 0）
     *
     * @param userId 用户ID
     * @return 设置实体（保证非空）
     */
    private UserReminder getOrCreateEntity(Long userId) {
        UserReminder entity = userReminderMapper.selectOne(
                Wrappers.<UserReminder>lambdaQuery().eq(UserReminder::getUserId, userId));
        if (entity == null) {
            entity = new UserReminder();
            entity.setUserId(userId);
            entity.setMasterSwitch(1);
            entity.setBreakfastSwitch(1);
            entity.setBreakfastTime(DEFAULT_BREAKFAST_TIME);
            entity.setLunchSwitch(1);
            entity.setLunchTime(DEFAULT_LUNCH_TIME);
            entity.setDinnerSwitch(1);
            entity.setDinnerTime(DEFAULT_DINNER_TIME);
            entity.setSubscribeCredit(0);
            entity.setStatus(1);
            userReminderMapper.insert(entity);
            log.info("[ReminderService] 首次查询默认值落库: userId={}", userId);
        }
        return entity;
    }

    /**
     * 校验提醒时间格式（null/空白/非 HH:mm 统一 40701）
     *
     * @param time 提醒时间
     */
    private void checkTime(String time) {
        if (time == null || !TIME_PATTERN.matcher(time).matches()) {
            throw new BusinessException(CommonConstant.REMINDER_TIME_INVALID_CODE,
                    ExceptionConstant.REMINDER_TIME_INVALID);
        }
    }

    /**
     * 实体转视图（附订阅额度与模板ID）
     *
     * @param entity 设置实体
     * @return 设置视图
     */
    private ReminderVO toVO(UserReminder entity) {
        ReminderVO vo = new ReminderVO();
        vo.setMasterSwitch(entity.getMasterSwitch());
        vo.setBreakfastSwitch(entity.getBreakfastSwitch());
        vo.setBreakfastTime(entity.getBreakfastTime());
        vo.setLunchSwitch(entity.getLunchSwitch());
        vo.setLunchTime(entity.getLunchTime());
        vo.setDinnerSwitch(entity.getDinnerSwitch());
        vo.setDinnerTime(entity.getDinnerTime());
        vo.setSubscribeCredit(entity.getSubscribeCredit());
        String templateId = wxMaProperties.getRemindTemplateId();
        vo.setTemplateId(templateId == null ? "" : templateId);
        return vo;
    }
}

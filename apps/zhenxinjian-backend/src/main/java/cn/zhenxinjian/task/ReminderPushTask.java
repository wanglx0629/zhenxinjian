package cn.zhenxinjian.task;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.zhenxinjian.common.enums.MealTypeEnum;
import cn.zhenxinjian.common.enums.ReminderSendStatusEnum;
import cn.zhenxinjian.config.WxMaConfiguration;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.ReminderSendLog;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.ReminderSendLogMapper;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.mapper.UserReminderMapper;
import cn.zhenxinjian.service.impl.ReminderService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 三餐提醒定时推送任务
 * 分钟级扫描到点设置，按 D4 判定链（单次 → 已记录 → openid → 额度）下发订阅消息
 * 作者: wanglx
 *
 * 口径：同用户同日同餐别至多一条成功推送（I12）；已记录该餐跳过；游客（无 openid）不推送；
 * 额度 0 / 模板未配置 / 微信服务不可用静默空转（PRD §2.9 已知限制，不打扰）；失败仅写日志不重试。
 * 一期单实例部署，多实例时需 ShedLock
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderPushTask {

    /** 单次扫描条数上限（避免无上限 selectList） */
    private static final int SCAN_LIMIT = 500;

    /** 命中比较格式：与库内 CHAR(5) 一致 HH:mm */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** 订阅消息跳转页（点击卡片进首页记录） */
    private static final String MESSAGE_PAGE = "pages/home/index";

    /** 提醒文案后缀（thing1：X餐记录提醒） */
    private static final String MESSAGE_TITLE_SUFFIX = "记录提醒";

    /** 提醒引导文案（thing2） */
    private static final String MESSAGE_GUIDE = "记得记录这一餐，保持数据连续";

    private final UserReminderMapper userReminderMapper;

    private final ReminderSendLogMapper reminderSendLogMapper;

    private final DietRecordMapper dietRecordMapper;

    private final UserMapper userMapper;

    private final ReminderService reminderService;

    private final WxMaConfiguration.WxMaProperties wxMaProperties;

    private final ObjectProvider<WxMaService> wxMaServiceProvider;

    /**
     * 分钟级扫描（fixedDelay 对齐分钟边界不敏感，匹配以 HH:mm 字符串为准）
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void pushDueReminders() {
        String templateId = wxMaProperties.getRemindTemplateId();
        if (!StringUtils.hasText(templateId)) {
            // 模板未配置任务空转
            return;
        }
        WxMaService wxMaService = wxMaServiceProvider.getIfAvailable();
        if (wxMaService == null) {
            // 微信服务未配置（无 appid）空转
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String hhmm = now.format(TIME_FORMATTER);
        LocalDate today = now.toLocalDate();
        List<UserReminder> dueList = userReminderMapper.selectList(Wrappers.<UserReminder>lambdaQuery()
                .eq(UserReminder::getMasterSwitch, 1)
                .and(wrapper -> wrapper
                        .and(b -> b.eq(UserReminder::getBreakfastSwitch, 1)
                                .eq(UserReminder::getBreakfastTime, hhmm))
                        .or(l -> l.eq(UserReminder::getLunchSwitch, 1)
                                .eq(UserReminder::getLunchTime, hhmm))
                        .or(d -> d.eq(UserReminder::getDinnerSwitch, 1)
                                .eq(UserReminder::getDinnerTime, hhmm)))
                .last("LIMIT " + SCAN_LIMIT));
        if (dueList.isEmpty()) {
            return;
        }
        for (UserReminder reminder : dueList) {
            Integer mealType = hitMealType(reminder, hhmm);
            if (mealType == null) {
                continue;
            }
            try {
                processOne(reminder, mealType, today, templateId, wxMaService);
            } catch (Exception e) {
                log.error("[ReminderPushTask] 单条推送处理异常: userId={}, mealType={}",
                        reminder.getUserId(), mealType, e);
            }
        }
    }

    /**
     * 判定当前时间命中的餐别（开关开且时间一致）
     *
     * @param reminder 提醒设置
     * @param hhmm     当前时间 HH:mm
     * @return 餐别编码；未命中返回 null
     */
    private Integer hitMealType(UserReminder reminder, String hhmm) {
        if (Integer.valueOf(1).equals(reminder.getBreakfastSwitch()) && hhmm.equals(reminder.getBreakfastTime())) {
            return MealTypeEnum.BREAKFAST.getCode();
        }
        if (Integer.valueOf(1).equals(reminder.getLunchSwitch()) && hhmm.equals(reminder.getLunchTime())) {
            return MealTypeEnum.LUNCH.getCode();
        }
        if (Integer.valueOf(1).equals(reminder.getDinnerSwitch()) && hhmm.equals(reminder.getDinnerTime())) {
            return MealTypeEnum.DINNER.getCode();
        }
        return null;
    }

    /**
     * 单条推送判定链（D4 顺序）与下发
     *
     * @param reminder    提醒设置
     * @param mealType    命中餐别
     * @param today       当日
     * @param templateId  订阅模板ID
     * @param wxMaService 微信服务
     */
    private void processOne(UserReminder reminder, Integer mealType, LocalDate today,
                            String templateId, WxMaService wxMaService) {
        Long userId = reminder.getUserId();
        // 1. 当日该餐别已成功推送过 → 跳过（I12 单次）
        Long sentCount = reminderSendLogMapper.selectCount(Wrappers.<ReminderSendLog>lambdaQuery()
                .eq(ReminderSendLog::getUserId, userId)
                .eq(ReminderSendLog::getRemindDate, today)
                .eq(ReminderSendLog::getMealType, mealType)
                .eq(ReminderSendLog::getSendStatus, ReminderSendStatusEnum.SUCCESS.getCode()));
        if (sentCount != null && sentCount > 0) {
            return;
        }
        // 2. 当日该餐别已有饮食记录 → 跳过（已记录不重复）
        Long recordCount = dietRecordMapper.selectCount(Wrappers.<DietRecord>lambdaQuery()
                .eq(DietRecord::getUserId, userId)
                .eq(DietRecord::getRecordDate, today)
                .eq(DietRecord::getMealType, mealType));
        if (recordCount != null && recordCount > 0) {
            return;
        }
        // 3. 游客（无 openid）不推送
        User user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getWechatOpenid())) {
            return;
        }
        // 4. 订阅额度耗尽 → 静默跳过
        if (reminder.getSubscribeCredit() == null || reminder.getSubscribeCredit() <= 0) {
            return;
        }
        // 下发订阅消息
        String mealDesc = MealTypeEnum.of(mealType).getDesc();
        WxMaSubscribeMessage message = new WxMaSubscribeMessage()
                .setToUser(user.getWechatOpenid())
                .setTemplateId(templateId)
                .setPage(MESSAGE_PAGE)
                .setMiniprogramState(wxMaProperties.getRemindMiniprogramState())
                .addData(new WxMaSubscribeMessage.MsgData("thing1", mealDesc + MESSAGE_TITLE_SUFFIX))
                .addData(new WxMaSubscribeMessage.MsgData("thing2", MESSAGE_GUIDE));
        try {
            wxMaService.getSubscribeService().sendSubscribeMsg(message);
            reminderService.onPushSuccess(userId, mealType, today, templateId);
            log.info("[ReminderPushTask] 订阅消息推送成功: userId={}, mealType={}", userId, mealType);
        } catch (WxErrorException e) {
            String reason = "errcode=" + e.getError().getErrorCode() + ", errmsg=" + e.getError().getErrorMsg();
            reminderService.onPushFail(userId, mealType, today, templateId, reason);
            log.warn("[ReminderPushTask] 订阅消息推送失败（静默降级）: userId={}, mealType={}, reason={}",
                    userId, mealType, reason);
        }
    }
}

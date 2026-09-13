package cn.zhenxinjian.task;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.zhenxinjian.common.enums.MealTypeEnum;
import cn.zhenxinjian.config.WxMaConfiguration;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.service.UserService;
import cn.zhenxinjian.service.impl.DietRecordService;
import cn.zhenxinjian.service.impl.ReminderService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 三餐提醒定时推送任务
 * 分钟级扫描窗口内到点设置，按 D4 判定链（单次 → 已记录 → openid → 额度）下发订阅消息
 * 作者: wanglx
 *
 * 口径：同用户同到点日同餐别至多一条成功推送（I12，按实际到点分钟归属日期记账，窗口跨零点不重复下发）；
 * 已记录该餐跳过；游客（无 openid）不推送；
 * 额度 0 / 模板未配置 / 微信服务不可用静默空转（PRD §2.9 已知限制，不打扰）；
 * 漏发补偿：匹配窗口 [当前-2, 当前] 三分钟，调度延迟/重启跨过分钟在窗口期内补发；
 * 失败写日志并随窗口内后续扫描自然重试（至多 3 次有界），窗口滑出不再补发（PRD 静默降级）。
 * 外呼经 reminderPushExecutor 限并发异步下发（调度池见 ScheduleConfig），微信侧变慢不阻塞统计/清理任务。
 * 判定查询一律经 service 层（ReminderService/DietRecordService/UserService），任务只编排不裸写 Mapper。
 * 一期单实例部署，多实例时需 ShedLock
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderPushTask {

    /** 单次扫描条数上限（避免无上限 selectList） */
    private static final int SCAN_LIMIT = 500;

    /** 漏发补偿回看分钟数：匹配窗口 [当前-N, 当前]，调度延迟/重启跨过分钟在窗口内补发 */
    private static final int COMPENSATION_LOOKBACK_MINUTES = 2;

    /** 命中比较格式：与库内 CHAR(5) 一致 HH:mm */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** 订阅消息跳转页（点击卡片进首页记录） */
    private static final String MESSAGE_PAGE = "pages/home/index";

    /** 提醒文案后缀（thing1：X餐记录提醒） */
    private static final String MESSAGE_TITLE_SUFFIX = "记录提醒";

    /** 提醒引导文案（thing2） */
    private static final String MESSAGE_GUIDE = "记得记录这一餐，保持数据连续";

    private final ReminderService reminderService;

    private final DietRecordService dietRecordService;

    private final UserService userService;

    private final WxMaConfiguration.WxMaProperties wxMaProperties;

    private final ObjectProvider<WxMaService> wxMaServiceProvider;

    /** 推送外呼执行器（ScheduleConfig 同名 Bean，按构造参数名注入；异步下发避免阻塞调度线程） */
    private final Executor reminderPushExecutor;

    /**
     * 分钟级扫描（fixedDelay 对齐分钟边界不敏感，匹配以 HH:mm 窗口为准）
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
        List<LocalDateTime> window = buildWindow(LocalDateTime.now());
        List<String> hhmmWindow = window.stream().map(m -> m.format(TIME_FORMATTER)).toList();
        List<UserReminder> dueList = reminderService.scanDueReminders(hhmmWindow, SCAN_LIMIT);
        if (dueList.isEmpty()) {
            return;
        }
        for (UserReminder reminder : dueList) {
            for (MealHit hit : hitMeals(reminder, window)) {
                // 异步限并发外呼：调度线程只编排，微信接口耗时不阻塞同池其他任务
                reminderPushExecutor.execute(() -> {
                    try {
                        processOne(reminder, hit.mealType(), hit.remindDate(), templateId, wxMaService);
                    } catch (Exception e) {
                        log.error("[ReminderPushTask] 单条推送处理异常: userId={}, mealType={}",
                                reminder.getUserId(), hit.mealType(), e);
                    }
                });
            }
        }
    }

    /**
     * 构造匹配窗口：[now-N, now] 共 N+1 个到点分钟（保留日期信息，供跨零点归属记账）
     *
     * @param now 当前时间
     * @return 窗口分钟列表（升序）
     */
    private List<LocalDateTime> buildWindow(LocalDateTime now) {
        List<LocalDateTime> window = new ArrayList<>(COMPENSATION_LOOKBACK_MINUTES + 1);
        for (int i = COMPENSATION_LOOKBACK_MINUTES; i >= 0; i--) {
            window.add(now.minusMinutes(i));
        }
        return window;
    }

    /**
     * 判定窗口内命中的餐别（开关开且时间在窗口内；多餐同窗全量返回，逐餐独立走判定链去重）
     *
     * @param reminder 提醒设置
     * @param window   窗口分钟列表
     * @return 命中餐别与归属到点日列表（早→午→晚序；未命中为空表）
     */
    private List<MealHit> hitMeals(UserReminder reminder, List<LocalDateTime> window) {
        List<MealHit> hits = new ArrayList<>(3);
        collectHit(hits, reminder.getBreakfastSwitch(), reminder.getBreakfastTime(),
                MealTypeEnum.BREAKFAST.getCode(), window);
        collectHit(hits, reminder.getLunchSwitch(), reminder.getLunchTime(),
                MealTypeEnum.LUNCH.getCode(), window);
        collectHit(hits, reminder.getDinnerSwitch(), reminder.getDinnerTime(),
                MealTypeEnum.DINNER.getCode(), window);
        return hits;
    }

    /**
     * 单餐命中收集：取窗口内最早到点分钟，按其实际归属日期记账（跨零点补发归昨日，I12 防重复下发）
     */
    private void collectHit(List<MealHit> hits, Integer mealSwitch, String mealTime,
                            Integer mealType, List<LocalDateTime> window) {
        if (!Integer.valueOf(1).equals(mealSwitch) || mealTime == null) {
            return;
        }
        for (LocalDateTime minute : window) {
            if (mealTime.equals(minute.format(TIME_FORMATTER))) {
                hits.add(new MealHit(mealType, minute.toLocalDate()));
                return;
            }
        }
    }

    /** 命中餐别与应归属的提醒日期（窗口跨零点时归属实际到点日，保证 I12 按到点日去重） */
    private record MealHit(Integer mealType, LocalDate remindDate) {
    }

    /**
     * 单条推送判定链（D4 顺序）与下发
     *
     * @param reminder    提醒设置
     * @param mealType    命中餐别
     * @param remindDate  提醒归属日期（实际到点分钟所在日，窗口跨零点补发归昨日）
     * @param templateId  订阅模板ID
     * @param wxMaService 微信服务
     */
    private void processOne(UserReminder reminder, Integer mealType, LocalDate remindDate,
                            String templateId, WxMaService wxMaService) {
        Long userId = reminder.getUserId();
        // 1. 该到点日该餐别已成功推送过 → 跳过（I12 单次）
        if (reminderService.hasSuccessPushToday(userId, mealType, remindDate)) {
            return;
        }
        // 2. 该到点日该餐别已有饮食记录 → 跳过（已记录不重复）
        if (dietRecordService.hasRecord(userId, remindDate, mealType)) {
            return;
        }
        // 3. 游客（无 openid）不推送
        User user = userService.getById(userId);
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
            reminderService.onPushSuccess(userId, mealType, remindDate, templateId);
            log.info("[ReminderPushTask] 订阅消息推送成功: userId={}, mealType={}", userId, mealType);
        } catch (WxErrorException e) {
            String reason = "errcode=" + e.getError().getErrorCode() + ", errmsg=" + e.getError().getErrorMsg();
            reminderService.onPushFail(userId, mealType, remindDate, templateId, reason);
            log.warn("[ReminderPushTask] 订阅消息推送失败（静默降级）: userId={}, mealType={}, reason={}",
                    userId, mealType, reason);
        }
    }
}

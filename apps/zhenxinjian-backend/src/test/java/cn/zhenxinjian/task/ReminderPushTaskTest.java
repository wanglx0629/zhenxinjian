package cn.zhenxinjian.task;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaSubscribeService;
import cn.zhenxinjian.common.enums.MealTypeEnum;
import cn.zhenxinjian.config.WxMaConfiguration;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.po.UserReminder;
import cn.zhenxinjian.service.UserService;
import cn.zhenxinjian.service.impl.DietRecordService;
import cn.zhenxinjian.service.impl.ReminderService;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 三餐提醒推送任务单元测试（无 Spring 容器，Mockito 桩依赖）
 * 覆盖 D4 判定链：模板/服务空转、已推送跳过、已记录跳过、游客跳过、额度 0 跳过、成功下发、微信异常静默；
 * 窗口补偿：上一分钟命中补发、窗口外不命中、多餐同窗逐餐下发、失败随下次扫描有界重试
 * 作者: wanglx
 */
class ReminderPushTaskTest {

    private static final String TEMPLATE_ID = "tpl-remind-1";

    private final ReminderService reminderService = mock(ReminderService.class);
    private final DietRecordService dietRecordService = mock(DietRecordService.class);
    private final UserService userService = mock(UserService.class);
    private final WxMaConfiguration.WxMaProperties wxMaProperties = new WxMaConfiguration.WxMaProperties();
    @SuppressWarnings("unchecked")
    private final ObjectProvider<WxMaService> wxMaServiceProvider = mock(ObjectProvider.class);
    private final WxMaService wxMaService = mock(WxMaService.class);
    private final WxMaSubscribeService subscribeService = mock(WxMaSubscribeService.class);

    /** 测试内同步执行，保证断言时外呼已完成 */
    private final Executor directExecutor = Runnable::run;

    private ReminderPushTask task;

    @BeforeEach
    void setUp() {
        wxMaProperties.setRemindTemplateId(TEMPLATE_ID);
        task = new ReminderPushTask(reminderService, dietRecordService, userService,
                wxMaProperties, wxMaServiceProvider, directExecutor);
    }

    /** 构造一条命中当前分钟（早餐档）的提醒设置 */
    private UserReminder dueReminder(long userId) {
        String hhmm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        UserReminder r = new UserReminder();
        r.setId(userId * 10);
        r.setUserId(userId);
        r.setMasterSwitch(1);
        r.setBreakfastSwitch(1);
        r.setBreakfastTime(hhmm);
        r.setLunchSwitch(0);
        r.setDinnerSwitch(0);
        r.setSubscribeCredit(3);
        return r;
    }

    /** 桩定：扫描命中一条，判定链前三关默认放行（未发过/未记录/正式用户有 openid） */
    private void stubDueWithOpenid(UserReminder reminder) {
        when(reminderService.scanDueReminders(anyList(), anyInt())).thenReturn(List.of(reminder));
        when(reminderService.hasSuccessPushToday(anyLong(), anyInt(), any())).thenReturn(false);
        when(dietRecordService.hasRecord(anyLong(), any(), anyInt())).thenReturn(false);
        User user = new User();
        user.setId(reminder.getUserId());
        user.setWechatOpenid("openid-" + reminder.getUserId());
        when(userService.getById(reminder.getUserId())).thenReturn(user);
    }

    @Test
    void shouldIdleWhenTemplateNotConfigured() {
        wxMaProperties.setRemindTemplateId(null);

        task.pushDueReminders();

        verifyNoInteractions(reminderService, dietRecordService, userService);
    }

    @Test
    void shouldIdleWhenWxServiceUnavailable() {
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(null);

        task.pushDueReminders();

        verifyNoInteractions(reminderService, dietRecordService, userService);
    }

    @Test
    void shouldSkipWhenAlreadySentToday() {
        UserReminder reminder = dueReminder(1L);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        when(reminderService.scanDueReminders(anyList(), anyInt())).thenReturn(List.of(reminder));
        when(reminderService.hasSuccessPushToday(anyLong(), anyInt(), any())).thenReturn(true);

        task.pushDueReminders();

        verifyNoInteractions(dietRecordService, userService);
        verify(reminderService, never()).onPushSuccess(anyLong(), anyInt(), any(), anyString());
    }

    @Test
    void shouldSkipWhenMealAlreadyRecorded() {
        UserReminder reminder = dueReminder(1L);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        when(reminderService.scanDueReminders(anyList(), anyInt())).thenReturn(List.of(reminder));
        when(reminderService.hasSuccessPushToday(anyLong(), anyInt(), any())).thenReturn(false);
        when(dietRecordService.hasRecord(anyLong(), any(), anyInt())).thenReturn(true);

        task.pushDueReminders();

        verifyNoInteractions(userService);
        verify(reminderService, never()).onPushSuccess(anyLong(), anyInt(), any(), anyString());
    }

    @Test
    void shouldSkipGuestWithoutOpenid() {
        UserReminder reminder = dueReminder(1L);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        when(reminderService.scanDueReminders(anyList(), anyInt())).thenReturn(List.of(reminder));
        when(reminderService.hasSuccessPushToday(anyLong(), anyInt(), any())).thenReturn(false);
        when(dietRecordService.hasRecord(anyLong(), any(), anyInt())).thenReturn(false);
        User guest = new User();
        guest.setId(1L);
        when(userService.getById(1L)).thenReturn(guest);

        task.pushDueReminders();

        verify(reminderService, never()).onPushSuccess(anyLong(), anyInt(), any(), anyString());
        verify(reminderService, never()).onPushFail(anyLong(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void shouldSkipWhenSubscribeCreditExhausted() {
        UserReminder reminder = dueReminder(1L);
        reminder.setSubscribeCredit(0);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        stubDueWithOpenid(reminder);

        task.pushDueReminders();

        verify(reminderService, never()).onPushSuccess(anyLong(), anyInt(), any(), anyString());
        verify(reminderService, never()).onPushFail(anyLong(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void shouldSendAndCallbackOnSuccess() throws WxErrorException {
        UserReminder reminder = dueReminder(1L);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        stubDueWithOpenid(reminder);
        when(wxMaService.getSubscribeService()).thenReturn(subscribeService);

        task.pushDueReminders();

        verify(subscribeService).sendSubscribeMsg(any());
        verify(reminderService).onPushSuccess(1L, MealTypeEnum.BREAKFAST.getCode(),
                LocalDate.now(), TEMPLATE_ID);
    }

    @Test
    void shouldCallbackFailSilentlyOnWxError() throws WxErrorException {
        UserReminder reminder = dueReminder(1L);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        stubDueWithOpenid(reminder);
        when(wxMaService.getSubscribeService()).thenReturn(subscribeService);
        WxError error = WxError.builder().errorCode(43101).errorMsg("user refuse").build();
        doThrow(new WxErrorException(error)).when(subscribeService).sendSubscribeMsg(any());

        task.pushDueReminders();

        verify(reminderService).onPushFail(anyLong(), anyInt(), any(LocalDate.class),
                anyString(), anyString());
        verify(reminderService, never()).onPushSuccess(anyLong(), anyInt(), any(), anyString());
    }

    /** 窗口补偿：提醒时间在上 1 分钟（调度延迟错过当分钟）仍命中补发 */
    @Test
    void shouldCompensateMissedMinuteWithinWindow() throws WxErrorException {
        UserReminder reminder = dueReminder(1L);
        reminder.setBreakfastTime(hhmm(-1));
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        stubDueWithOpenid(reminder);
        when(wxMaService.getSubscribeService()).thenReturn(subscribeService);

        task.pushDueReminders();

        verify(subscribeService).sendSubscribeMsg(any());
        verify(reminderService).onPushSuccess(1L, MealTypeEnum.BREAKFAST.getCode(),
                LocalDate.now(), TEMPLATE_ID);
    }

    /** 窗口外不命中：提醒时间在 3 分钟前（滑出 [当前-2, 当前] 窗口）任务侧过滤不下发 */
    @Test
    void shouldNotHitOutsideWindow() {
        UserReminder reminder = dueReminder(1L);
        reminder.setBreakfastTime(hhmm(-3));
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        when(reminderService.scanDueReminders(anyList(), anyInt())).thenReturn(List.of(reminder));

        task.pushDueReminders();

        verifyNoInteractions(dietRecordService, userService);
        verify(reminderService, never()).onPushSuccess(anyLong(), anyInt(), any(), anyString());
        verify(reminderService, never()).onPushFail(anyLong(), anyInt(), any(), anyString(), anyString());
    }

    /** 多餐同窗：早餐上 1 分钟 + 午餐当分钟同窗命中，逐餐独立走判定链各发一条 */
    @Test
    void shouldProcessBothMealsInSameWindow() throws WxErrorException {
        UserReminder reminder = dueReminder(1L);
        reminder.setBreakfastTime(hhmm(-1));
        reminder.setLunchSwitch(1);
        reminder.setLunchTime(hhmm(0));
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        stubDueWithOpenid(reminder);
        when(wxMaService.getSubscribeService()).thenReturn(subscribeService);

        task.pushDueReminders();

        verify(subscribeService, times(2)).sendSubscribeMsg(any());
        verify(reminderService).onPushSuccess(1L, MealTypeEnum.BREAKFAST.getCode(),
                LocalDate.now(), TEMPLATE_ID);
        verify(reminderService).onPushSuccess(1L, MealTypeEnum.LUNCH.getCode(),
                LocalDate.now(), TEMPLATE_ID);
    }

    /** 失败有界重试：当次扫描失败写日志，下次扫描窗口仍覆盖该分钟自然重试并成功 */
    @Test
    void shouldRetryFailedPushWithinWindowOnNextScan() throws WxErrorException {
        UserReminder reminder = dueReminder(1L);
        when(wxMaServiceProvider.getIfAvailable()).thenReturn(wxMaService);
        stubDueWithOpenid(reminder);
        when(wxMaService.getSubscribeService()).thenReturn(subscribeService);
        WxError error = WxError.builder().errorCode(47003).errorMsg("template unstable").build();
        doThrow(new WxErrorException(error)).doNothing().when(subscribeService).sendSubscribeMsg(any());

        task.pushDueReminders();
        task.pushDueReminders();

        verify(subscribeService, times(2)).sendSubscribeMsg(any());
        verify(reminderService).onPushFail(1L, MealTypeEnum.BREAKFAST.getCode(),
                LocalDate.now(), TEMPLATE_ID, "errcode=47003, errmsg=template unstable");
        verify(reminderService).onPushSuccess(1L, MealTypeEnum.BREAKFAST.getCode(),
                LocalDate.now(), TEMPLATE_ID);
    }

    /** 当前时间偏移 offset 分钟后的 HH:mm（与任务窗口口径一致） */
    private String hhmm(int offsetMinutes) {
        return LocalDateTime.now().plusMinutes(offsetMinutes)
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}

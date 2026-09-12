package cn.zhenxinjian.common.enums;

/**
 * 埋点事件码字典枚举（对应 track_event.event_code 列；后端唯一真源）
 * 作者: wanglx
 */
public enum TrackEventEnum {

    /** 微信登录成功 */
    LOGIN_WECHAT("login_wechat", "微信登录"),

    /** 游客进入 */
    LOGIN_GUEST("login_guest", "游客进入"),

    /** 登录失败 */
    LOGIN_FAIL("login_fail", "登录失败"),

    /** 页面访问 */
    PAGE_VIEW("page_view", "页面访问"),

    /** 添加饮食记录 */
    RECORD_ADD("record_add", "添加饮食记录"),

    /** 编辑饮食记录 */
    RECORD_EDIT("record_edit", "编辑饮食记录"),

    /** 删除饮食记录 */
    RECORD_DELETE("record_delete", "删除饮食记录"),

    /** 食物搜索 */
    FOOD_SEARCH("food_search", "食物搜索"),

    /** 查看食物详情 */
    FOOD_DETAIL("food_detail", "查看食物详情"),

    /** 添加自定义食物 */
    FOOD_CUSTOM_ADD("food_custom_add", "添加自定义食物"),

    /** 选择模式 */
    MODE_SELECT("mode_select", "选择模式"),

    /** 切换模式 */
    MODE_SWITCH("mode_switch", "切换模式"),

    /** 查看计划 */
    PLAN_VIEW("plan_view", "查看计划"),

    /** 保存提醒设置 */
    REMINDER_SAVE("reminder_save", "保存提醒设置"),

    /** 订阅提醒授权 */
    REMINDER_SUBSCRIBE("reminder_subscribe", "订阅提醒授权"),

    /** 保存身体档案 */
    BODY_SAVE("body_save", "保存身体档案"),

    /** 记录体重 */
    WEIGHT_ADD("weight_add", "记录体重"),

    /** 保存经期设置 */
    MENSTRUAL_SAVE("menstrual_save", "保存经期设置"),

    /** 首页快捷入口 */
    HOME_QUICK_ENTRY("home_quick_entry", "首页快捷入口"),

    /** 热门食物点击 */
    FOOD_HOT_CLICK("food_hot_click", "热门食物点击"),

    /** 历史搜索点击 */
    FOOD_HISTORY_CLICK("food_history_click", "历史搜索点击"),

    /** 埋点队列饱和告警（端上自监控：本地队列 ≥80% 时上报一次） */
    TRACK_QUEUE_SATURATED("track_queue_saturated", "埋点队列饱和告警");

    /** 事件码（对应 track_event.event_code 列） */
    private final String code;

    /** 事件中文名 */
    private final String desc;

    TrackEventEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按事件码查询枚举
     *
     * @param code 事件码
     * @return 对应枚举；code 无效时返回 null
     */
    public static TrackEventEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (TrackEventEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

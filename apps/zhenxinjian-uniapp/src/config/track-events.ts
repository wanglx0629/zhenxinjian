/**
 * 埋点事件码常量表（前端唯一引用入口；与后端 TrackEventEnum 一一互锚）
 * 作者: wanglx
 *
 * 同步约定：后端 common/enums/TrackEventEnum.java 为契约真源（22 码）；
 * 新增/修改事件码必须先改后端枚举再同步本表；track() 参数已收窄为
 * TrackEventCode 联合类型，表外字面量将在编译期报错（防拼写漂移进毒批次链路）。
 */

/** 埋点事件码（对照后端 TrackEventEnum，键名即枚举名，值即 code，改动须同步后端枚举） */
export const TRACK_EVENT = {
  /** 微信登录成功 */
  LOGIN_WECHAT: 'login_wechat',
  /** 游客进入 */
  LOGIN_GUEST: 'login_guest',
  /** 登录失败 */
  LOGIN_FAIL: 'login_fail',
  /** 页面访问 */
  PAGE_VIEW: 'page_view',
  /** 添加饮食记录 */
  RECORD_ADD: 'record_add',
  /** 编辑饮食记录 */
  RECORD_EDIT: 'record_edit',
  /** 删除饮食记录 */
  RECORD_DELETE: 'record_delete',
  /** 食物搜索 */
  FOOD_SEARCH: 'food_search',
  /** 查看食物详情 */
  FOOD_DETAIL: 'food_detail',
  /** 添加自定义食物 */
  FOOD_CUSTOM_ADD: 'food_custom_add',
  /** 选择模式（后端枚举保留码；当前前端统一以 MODE_SWITCH 上报，见 mode/select.vue） */
  MODE_SELECT: 'mode_select',
  /** 切换模式 */
  MODE_SWITCH: 'mode_switch',
  /** 查看计划 */
  PLAN_VIEW: 'plan_view',
  /** 保存提醒设置 */
  REMINDER_SAVE: 'reminder_save',
  /** 订阅提醒授权 */
  REMINDER_SUBSCRIBE: 'reminder_subscribe',
  /** 保存身体档案 */
  BODY_SAVE: 'body_save',
  /** 记录体重 */
  WEIGHT_ADD: 'weight_add',
  /** 保存经期设置 */
  MENSTRUAL_SAVE: 'menstrual_save',
  /** 首页快捷入口 */
  HOME_QUICK_ENTRY: 'home_quick_entry',
  /** 热门食物点击 */
  FOOD_HOT_CLICK: 'food_hot_click',
  /** 历史搜索点击 */
  FOOD_HISTORY_CLICK: 'food_history_click',
  /** 埋点队列饱和告警（端上自监控：本地队列 ≥80% 时上报一次） */
  TRACK_QUEUE_SATURATED: 'track_queue_saturated'
} as const

/** 埋点事件码联合类型（track() 入参收窄目标；表外字面量编译期报错） */
export type TrackEventCode = (typeof TRACK_EVENT)[keyof typeof TRACK_EVENT]

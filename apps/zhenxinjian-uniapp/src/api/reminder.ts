/**
 * 三餐饮食提醒相关 API（P13 提醒设置 + 微信订阅消息授权上报）
 * 作者: wanglx
 */
import http from './request'

/** 提醒设置视图（与后端 ReminderVO 对齐） */
export interface ReminderVO {
  /** 总开关：0关 1开 */
  masterSwitch: number
  /** 早餐提醒开关：0关 1开 */
  breakfastSwitch: number
  /** 早餐提醒时间 HH:mm */
  breakfastTime: string
  /** 午餐提醒开关：0关 1开 */
  lunchSwitch: number
  /** 午餐提醒时间 HH:mm */
  lunchTime: string
  /** 晚餐提醒开关：0关 1开 */
  dinnerSwitch: number
  /** 晚餐提醒时间 HH:mm */
  dinnerTime: string
  /** 订阅消息剩余额度（0 且存在开启项时前端引导重新授权） */
  subscribeCredit: number
  /** 订阅消息模板ID（wx.requestSubscribeMessage 入参；未配置为空串） */
  templateId: string
}

/** 提醒设置保存入参（与后端 ReminderSaveDTO 对齐，整体保存） */
export interface ReminderSaveRequest {
  masterSwitch: number
  breakfastSwitch: number
  breakfastTime: string
  lunchSwitch: number
  lunchTime: string
  dinnerSwitch: number
  dinnerTime: string
}

/** 查询提醒设置（无记录时后端按默认值落库：08:30/12:00/18:30 全开） */
export function getReminder() {
  return http.get<ReminderVO>('/reminder')
}

/** 整体保存提醒设置（时间非法时后端返回 40701） */
export function saveReminder(data: ReminderSaveRequest) {
  return http.put<ReminderVO>('/reminder', data)
}

/** 上报订阅授权（额度 +1，同分钟内重复上报幂等去重） */
export function reportSubscribe() {
  return http.post<void>('/reminder/subscribe')
}

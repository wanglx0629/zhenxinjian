/**
 * 数据看板 API
 * 作者: wanglx
 */
import request from './request'

/** 总览卡片（今日 DAU 实时查明细表） */
export interface StatsOverview {
  todayDau: number
  yesterdayDau: number
  mau: number
  totalUsers: number
  totalDietRecords: number
}

/** 每日活跃（聚合表） */
export interface DailyActive {
  statDate: string
  dau: number
  guestDau: number
  newUser: number
}

/** 功能排行 */
export interface EventRank {
  eventCode: string
  eventName: string
  pv: number
  uv: number
}

/** 页面排行 */
export interface PageRank {
  page: string
  pv: number
  uv: number
}

/** 总览（今日实时 + 昨日/累计） */
export function getOverview() {
  return request.get<StatsOverview>('/admin/stats/overview')
}

/** DAU 趋势（聚合表，days 1-90） */
export function getActiveTrend(days = 30) {
  return request.get<DailyActive[]>('/admin/stats/active-trend', { params: { days } })
}

/** 功能排行 TOP N */
export function getEventRank(days = 7, limit = 10) {
  return request.get<EventRank[]>('/admin/stats/event-rank', { params: { days, limit } })
}

/** 页面排行 TOP N */
export function getPageRank(days = 7, limit = 10) {
  return request.get<PageRank[]>('/admin/stats/page-rank', { params: { days, limit } })
}

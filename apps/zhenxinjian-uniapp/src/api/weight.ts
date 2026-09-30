/**
 * 体重记录相关 API（体重记录 + 平台判定 + 调碳日志）
 * 作者: wanglx
 */
import http from './request'

/** 体重记录项（与后端 WeightRecordVO 对齐） */
export interface WeightRecordVO {
  id: number
  /** 记录日期 YYYY-MM-DD */
  recordDate: string
  /** 体重 kg */
  weight: number
  /** 是否平台期（按近 7 天波动判定） */
  plateau: boolean
}

/** 调碳日志项（与后端 AdjustLogVO 对齐） */
export interface AdjustLogVO {
  id: number
  /** 动作：1下调 2恢复 */
  action: number
  /** 动作名称：下调/恢复 */
  actionName: string
  /** 触发当日体重 kg */
  triggerWeight: number
  /** 动作发生时间 */
  createTime: string
}

/** 体重记录保存入参（与后端 WeightSaveDTO 对齐） */
export interface WeightSaveRequest {
  /** 记录日期 YYYY-MM-DD */
  recordDate: string
  /** 体重 kg（25-200） */
  weight: number
}

/** 体重趋势点（与后端 WeightTrendVO.Point 对齐） */
export interface WeightTrendPoint {
  /** 记录日期 YYYY-MM-DD */
  date: string
  /** 体重 kg（当日末值） */
  weight: number
  /** 当日是否处于平台期 */
  plateau: boolean
}

/** 体重趋势（与后端 WeightTrendVO 对齐） */
export interface WeightTrendVO {
  /** 每日末值点（按日期升序，不足两日为 null） */
  points: WeightTrendPoint[] | null
  /** 体重差 kg = 最早日末值 − 最新日末值（正=下降，负=上升，不足两日为 null） */
  delta: number | null
  /** 窗口起始日期 */
  startDate: string | null
  /** 窗口结束日期 */
  endDate: string
}

/** 保存体重记录（同日允许多条共存，触发平台下调/恢复判定） */
export function saveWeight(data: WeightSaveRequest) {
  return http.put<WeightRecordVO>('/weight', data)
}

/** 查询体重记录（按日期范围过滤；不传 limit 返回全部） */
export function listWeights(params?: { startDate?: string; endDate?: string; limit?: number }) {
  return http.get<WeightRecordVO[]>('/weight', params)
}

/** 查询体重趋势（range: 7/30/60/90/365/all，默认 30） */
export function getWeightTrend(range: string) {
  return http.get<WeightTrendVO>('/weight/trend', { range })
}

/** 删除体重记录（逻辑删除） */
export function deleteWeight(id: number) {
  return http.delete<void>(`/weight/${id}`)
}

/** 查询调碳日志（下调/恢复动作留痕，按时间倒序） */
export function listWeightAdjustLogs() {
  return http.get<AdjustLogVO[]>('/weight/logs')
}
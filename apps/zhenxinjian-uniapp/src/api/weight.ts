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

/** 保存体重记录（同日幂等覆盖，触发平台下调/恢复判定） */
export function saveWeight(data: WeightSaveRequest) {
  return http.put<WeightRecordVO>('/weight', data)
}

/** 查询体重记录（按日期范围过滤，默认最近 30 条） */
export function listWeights(params?: { startDate?: string; endDate?: string; limit?: number }) {
  return http.get<WeightRecordVO[]>('/weight', params)
}

/** 删除体重记录（逻辑删除） */
export function deleteWeight(id: number) {
  return http.delete<void>(`/weight/${id}`)
}

/** 查询调碳日志（下调/恢复动作留痕，按时间倒序） */
export function listWeightAdjustLogs() {
  return http.get<AdjustLogVO[]>('/weight/logs')
}
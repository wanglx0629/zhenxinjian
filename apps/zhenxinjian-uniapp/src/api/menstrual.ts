/**
 * 经期管理相关 API（查询 / 保存经期设置）
 * 作者: wanglx
 */
import http from './request'

/** 经期设置视图（与后端 MenstrualVO 对齐） */
export interface MenstrualVO {
  /** 是否适用（女性 true；男性 false，界面隐藏经期相关内容） */
  applicable: boolean
  /** 是否开启经期管理：0关 1开 */
  enabled: number
  /** 末次月经起始日 */
  periodStartDate?: string | null
  /** 周期长度 L（21-35） */
  cycleLen?: number
  /** 经期天数 D（3-10） */
  periodDays?: number
  /** 当前阶段键：menstrual/follicular/ovulation/luteal（未开启/空态为空） */
  phaseKey?: string | null
  /** 当前阶段名称（经期/卵泡期/排卵期/黄体期） */
  phaseName?: string | null
  /** 当前阶段日序 dayIdx（1..L，仅开启时有值） */
  dayIdx?: number | null
  /** 碳水上浮 g（仅开启时有值） */
  carbUplift?: number | null
  /** 热量上浮 kcal（仅开启时有值） */
  kcalUplift?: number | null
}

/** 经期设置保存入参（与后端 MenstrualSaveDTO 对齐） */
export interface MenstrualSaveRequest {
  /** 是否开启经期管理：0关 1开 */
  enabled: number
  /** 末次月经起始日（开启时必填，不得为未来日期） */
  periodStartDate?: string | null
  /** 周期长度 L（21-35，默认28） */
  cycleLen?: number
  /** 经期天数 D（3-10，默认5） */
  periodDays?: number
}

/** 查询经期设置（男性返回 applicable=false） */
export function getMenstrual() {
  return http.get<MenstrualVO>('/menstrual')
}

/** 保存经期设置 */
export function saveMenstrual(data: MenstrualSaveRequest) {
  return http.put<MenstrualVO>('/menstrual', data)
}
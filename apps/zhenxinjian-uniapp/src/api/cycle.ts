/**
 * 碳循环相关 API（P05 模式选择 / P06 周期设置 / P07 周期计划 / P16 切换确认）
 * 作者: wanglx
 */
import http from './request'

/** 逐日日型计划（与后端 CycleDayVO 对齐） */
export interface CycleDayVO {
  /** 日序 1..N */
  dayIndex: number
  /** 日历日 YYYY-MM-DD */
  dayDate: string
  /** 日型：1高碳 2中碳 3低碳 */
  dayType: number
  /** 日型名称：高碳日/中碳日/低碳日 */
  dayTypeName: string
  /** 运动日：0否 1是 */
  isSport: number
  /** 目标碳水 g */
  carbG: number
  /** 目标蛋白 g */
  proteinG: number
  /** 目标脂肪 g */
  fatG: number
  /** 目标能量 kcal */
  kcal: number
}

/** 周期计划（与后端 CyclePlanVO 对齐；无周期时 id 为空、days 空列表） */
export interface CyclePlanVO {
  id: number | null
  /** 周期天数（7-14） */
  cycleDays?: number
  /** 脂肪系数：0.8/1.0 */
  cfc?: number
  startDate?: string
  endDate?: string
  /** 碳水池 g */
  carbPool?: number
  /** 脂肪池 g */
  fatPool?: number
  /** 每日蛋白 g（周期内固定） */
  dailyProtein?: number
  /** 状态：1进行中 2已完成 3已终止 */
  status?: number
  /** 逐日计划（按日序升序） */
  days: CycleDayVO[]
  /** 今日日序（今日在周期内时返回 1..N；否则为 null） */
  todayIndex: number | null
}

/** 周期创建入参（与后端 CyclePlanCreateDTO 对齐） */
export interface CyclePlanCreateRequest {
  /** 周期天数（7-14，缺省 7） */
  cycleDays?: number
  /** 脂肪系数:0.8/1.0（缺省 0.8） */
  cfc?: number
  /** 运动日日序列表（1..cycleDays，可空） */
  sportDays?: number[]
}

/** 创建碳循环周期（存在进行中周期时后端先终止旧周期） */
export function createCyclePlan(data: CyclePlanCreateRequest) {
  return http.post<CyclePlanVO>('/cycle/plans', data)
}

/** 查询当前进行中周期（无周期返回空态 id=null） */
export function getCurrentCyclePlan() {
  return http.get<CyclePlanVO>('/cycle/plans/current')
}

/** 查询历史周期详情（仅本人） */
export function getCyclePlanById(id: number) {
  return http.get<CyclePlanVO>(`/cycle/plans/${id}`)
}

/** 终止当前进行中周期（幂等） */
export function terminateCurrentCyclePlan() {
  return http.post<void>('/cycle/plans/current/terminate')
}

/** 切换减脂模式（1=532 2=碳循环；切出碳循环自动终止进行中周期） */
export function switchDietMode(mode: number) {
  return http.put<void>('/body/mode', { mode })
}

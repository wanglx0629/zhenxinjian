/**
 * 532 碳水渐降推进相关 API（四阶段计划卡 + 今日目标）
 * 作者: wanglx
 */
import http from './request'

/** 532 阶段计划项（与后端 Taper532VO.StageItem 对齐） */
export interface Taper532Stage {
  /** 阶段序号 1-4 */
  n: number
  /** 阶段名称 */
  name: string
  /** 阶段说明 */
  desc: string
  /** 碳水调整 g（阶段 4 为 -20） */
  carbDelta: number
  /** 热量调整 kcal（阶段 4 为 -80） */
  kcalDelta: number
  /** 是否平台触发式（仅阶段 4 为 true） */
  plateauTriggered: boolean
}

/** 今日 532 目标（与后端 Taper532VO.TodayTarget 对齐） */
export interface Taper532Today {
  /** 目标碳水 g（1 位小数） */
  carb: number
  /** 目标蛋白 g（1 位小数） */
  protein: number
  /** 目标脂肪 g（1 位小数） */
  fat: number
  /** 目标热量 kcal（取整） */
  kcal: number
  /** 是否平台下调态 */
  adjusted: boolean
  /** 当前经期阶段键（未叠加为空） */
  phaseKey?: string | null
  /** 当前经期阶段名称（未叠加为空） */
  phaseName?: string | null
}

/** 532 四阶段计划卡 + 今日目标（与后端 Taper532VO 对齐） */
export interface Taper532Plan {
  stages: Taper532Stage[]
  /** 今日目标（未建档为空） */
  today: Taper532Today | null
}

/** 查询 532 四阶段计划卡与今日目标 */
export function getTaper532Plan() {
  return http.get<Taper532Plan>('/taper-532/plan')
}
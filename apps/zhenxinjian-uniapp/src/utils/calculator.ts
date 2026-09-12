/**
 * 臻心减核心算法（TypeScript 版，自小程序骨架迁移）
 * 作者: wanglx
 *
 * ★ 仅展示兜底：本文件仅服务录入页实时预览，权威计算口径以后端为准。
 *   影子实现已退库（B-T09）：经期阶段/532 计划/碳循环分配/超标比对/平台期判定等
 *   一律消费后端返回，前端不再保留本地副本。
 */
import { KCAL_PER_G, MACRO_532 } from '@/config/constants'

/* ------------------------------------------------------------------
 * 类型定义
 * ------------------------------------------------------------------ */

/** 用户档案（录入页预览所需字段） */
export interface CalcProfile {
  gender: 'male' | 'female'
  age: number
  height: number
  weight: number
  /** 活动系数值（1.2 / 1.375 / 1.55 / 1.725） */
  act: number
  /** 热量缺口 X（200 / 300 / 400 / 500） */
  deficit: number
}

/** 宏量结果 */
export interface MacroResult {
  kcal: number
  carb: number
  protein: number
  fat: number
}

/* ------------------------------------------------------------------
 * 基础代谢与总消耗（PRD §2.2）——仅展示兜底，权威口径在后端
 * ------------------------------------------------------------------ */

const round = (n: number): number => Math.round(n)

/** BMR · Mifflin-St Jeor 公式（女 −161 / 男 +5），仅展示兜底 */
export function bmr(p: CalcProfile): number {
  const base = 10 * p.weight + 6.25 * p.height - 5 * p.age
  return p.gender === 'male' ? base + 5 : base - 161
}

/** TDEE = BMR × 活动系数，仅展示兜底 */
export function tdee(p: CalcProfile): number {
  return bmr(p) * p.act
}

/** 基准热量 = TDEE − 用户自选缺口 X，仅展示兜底 */
function targetKcal(p: CalcProfile): number {
  return tdee(p) - p.deficit
}

/** 532 基准宏量（未叠加经期上浮），仅录入页预览展示用 */
export function macro532Base(p: CalcProfile): MacroResult {
  const total = targetKcal(p)
  return {
    kcal: round(total),
    carb: round(total * MACRO_532.carb / KCAL_PER_G.carb),
    protein: round(total * MACRO_532.protein / KCAL_PER_G.protein),
    fat: round(total * MACRO_532.fat / KCAL_PER_G.fat)
  }
}

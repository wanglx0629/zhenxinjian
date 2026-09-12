/**
 * 录入区间校验（PRD §2.2 / §8.1）+ 宏量-能量守恒校验（单一真源）
 * 守恒口径以后端 MacroConsistencyValidator 为准（业务不变量 I11）：
 * |碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%
 * 作者: wanglx
 */
import { RANGES, KCAL_PER_G } from '@/config/constants'

export interface RangeCheckResult {
  ok: boolean
  msg: string
  value: number | null
}

/** 字段中文名（空值提示用） */
const FIELD_LABELS: Record<string, string> = {
  age: '年龄',
  height: '身高',
  weight: '体重',
  targetWeight: '目标体重',
  cycleLen: '周期长度',
  periodDays: '经期天数',
  cycleDays: '周期天数'
}

/**
 * @param value 待校验值
 * @param key   RANGES 中的键，如 'age'
 */
export function checkRange(value: unknown, key: string): RangeCheckResult {
  const cfg = RANGES[key]
  if (!cfg) return { ok: true, msg: '', value: Number(value) }
  const n = Number(value)
  if (value === '' || value === null || value === undefined || isNaN(n)) {
    return { ok: false, msg: `请输入${FIELD_LABELS[key] ?? key}`, value: null }
  }
  if (n < cfg.min || n > cfg.max) {
    return { ok: false, msg: `请输入 ${cfg.min} – ${cfg.max} ${cfg.unit} 之间的数值`, value: null }
  }
  return { ok: true, msg: '', value: n }
}

/** 批量校验，返回 { ok, errors } */
export function checkAll(values: Record<string, unknown>): { ok: boolean; errors: Record<string, string> } {
  const errors: Record<string, string> = {}
  let ok = true
  Object.keys(values).forEach(k => {
    const r = checkRange(values[k], k)
    if (!r.ok) { errors[k] = r.msg; ok = false }
  })
  return { ok, errors }
}

/** 目标体重不得高于当前体重（PRD §2.2：减脂场景） */
export function checkTargetWeight(weight: number | string, targetWeight: number | string): { ok: boolean; msg: string } {
  if (Number(targetWeight) > Number(weight) + 0.1) {
    return { ok: false, msg: '减脂场景下目标体重需低于当前体重' }
  }
  return { ok: true, msg: '' }
}

/** 能量守恒允许偏差比例（与后端 MacroConsistencyValidator.TOLERANCE_RATIO 一致，变更须同步后端） */
export const KCAL_TOLERANCE = 0.10

/** 宏量理论产能量：碳水×4 + 蛋白×4 + 脂肪×9（系数锚点 config/constants.ts KCAL_PER_G ↔ 后端 4/4/9） */
export function kcalFromMacros(carb: number, protein: number, fat: number): number {
  return carb * KCAL_PER_G.carb + protein * KCAL_PER_G.protein + fat * KCAL_PER_G.fat
}

export interface KcalConsistencyResult {
  ok: boolean
  /** 宏量理论产能量 kcal */
  expected: number
  /** 偏差 kcal */
  diff: number
  /** 相对偏差比例（÷max(kcal,1)） */
  ratio: number
}

/** 能量守恒判定：理论产能量与标注能量的相对偏差 ≤ 10%（后端同口径，前端仅预校验，权威以后端为准） */
export function checkKcalConsistency(carb: number, protein: number, fat: number, kcal: number): KcalConsistencyResult {
  const expected = kcalFromMacros(carb, protein, fat)
  const diff = Math.abs(expected - kcal)
  const base = Math.max(kcal, 1)
  const ratio = diff / base
  return { ok: ratio <= KCAL_TOLERANCE, expected, diff, ratio }
}

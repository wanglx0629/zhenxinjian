/**
 * 三宏/热量进度计算（首页组件口径单一来源，与 P12 记录页逻辑一致）
 * 作者: wanglx
 */
import { OVER_ADVICE, PROGRESS_COLORS, PROGRESS_THRESHOLD } from '@/config/constants'
import type { DietSummaryVO } from '@/api/diet'

/** 进度项（三宏 + 总热量） */
export interface ProgressItem {
  key: 'carb' | 'protein' | 'fat' | 'kcal'
  label: string
  actual: number
  target: number | null
  rate: number
  unit: string
  /** 三色：80–100 绿 / <80 黄 / >100 红 */
  color: string
  /** 超标量（实际 − 目标，1 位小数；未超标为 0） */
  overAmount: number
  /** 进度条宽度口径（上限 150%） */
  displayRate: number
}

/** 由当日累计构建四项进度（未建档/空态返回空数组） */
export function buildProgressItems(s: DietSummaryVO | null): ProgressItem[] {
  if (!s || !s.recorded) return []
  const base = [
    { key: 'carb', label: '碳水', actual: s.carbActual, target: s.carbTarget, rate: s.carbRate, unit: 'g' },
    { key: 'protein', label: '蛋白', actual: s.proteinActual, target: s.proteinTarget, rate: s.proteinRate, unit: 'g' },
    { key: 'fat', label: '脂肪', actual: s.fatActual, target: s.fatTarget, rate: s.fatRate, unit: 'g' },
    { key: 'kcal', label: '热量', actual: s.kcalActual, target: s.kcalTarget, rate: s.kcalRate, unit: 'kcal' }
  ] as const
  return base.map(item => {
    const rate = item.rate ?? 0
    let color: string = PROGRESS_COLORS.yellow
    if (rate > PROGRESS_THRESHOLD.red) color = PROGRESS_COLORS.red
    else if (rate >= PROGRESS_THRESHOLD.green) color = PROGRESS_COLORS.green
    const overAmount = rate > PROGRESS_THRESHOLD.red && item.target != null
      ? Math.round((item.actual - item.target) * 10) / 10
      : 0
    return { ...item, rate, color, overAmount, displayRate: Math.min(rate, 150) }
  })
}

/** 按超标项动态生成微调建议（文案口径同 diet-record OVER_ADVICE） */
export function buildAdviceList(items: ProgressItem[]): string[] {
  const list: string[] = []
  for (const item of items) {
    if (item.overAmount <= 0) continue
    if (item.key === 'fat') list.push(OVER_ADVICE.fat)
    else if (item.key === 'carb') list.push(OVER_ADVICE.carb)
    else if (item.key === 'protein') list.push(OVER_ADVICE.protein)
    else if (item.key === 'kcal') list.push(OVER_ADVICE.kcal)
  }
  return list
}

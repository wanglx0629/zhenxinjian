/**
 * scalePer100g 纯函数单测（DESIGN-T06 安全网）
 * 作者: wanglx
 */
import { describe, expect, it } from 'vitest'
import { scalePer100g } from './macro'

const M = { carb: 20, protein: 10, fat: 5, kcal: 160 }

describe('scalePer100g', () => {
  it('100g 透传：比值 1，宏量与能量不变', () => {
    expect(scalePer100g(M, 100)).toEqual({ carb: 20, protein: 10, fat: 5, kcal: 160 })
  })

  it('200g 翻倍', () => {
    expect(scalePer100g(M, 200)).toEqual({ carb: 40, protein: 20, fat: 10, kcal: 320 })
  })

  it('宏量保留 2 位小数', () => {
    // 150g：carb 30 / protein 15 / fat 7.5 / kcal 240
    expect(scalePer100g(M, 150)).toEqual({ carb: 30, protein: 15, fat: 7.5, kcal: 240 })
    // 33g：carb 6.6 / protein 3.3 / fat 1.65 / kcal 52.8→53
    expect(scalePer100g(M, 33)).toEqual({ carb: 6.6, protein: 3.3, fat: 1.65, kcal: 53 })
  })

  it('能量按整数四舍五入', () => {
    expect(scalePer100g(M, 33).kcal).toBe(53)
    expect(scalePer100g(M, 1).kcal).toBe(2) // 160×0.01=1.6→2
    expect(scalePer100g(M, 200).kcal).toBe(320)
  })

  it('小数克数换算', () => {
    // 50g：carb 10 / protein 5 / fat 2.5 / kcal 80
    expect(scalePer100g(M, 50)).toEqual({ carb: 10, protein: 5, fat: 2.5, kcal: 80 })
    // 0g 兜底：全 0
    expect(scalePer100g(M, 0)).toEqual({ carb: 0, protein: 0, fat: 0, kcal: 0 })
  })
})
/**
 * computeStickiness 纯函数单测（DESIGN-T04 安全网）
 * 作者: wanglx
 */
import { describe, expect, it } from 'vitest'
import { computeStickiness } from './stats'

describe('computeStickiness', () => {
  it('正常口径：今日 DAU / 近 30 天 MAU 百分比', () => {
    const r = computeStickiness({ todayDau: 30, mau: 100 } as never)
    expect(r.hasData).toBe(true)
    expect(r.value).toBe(30)
  })

  it('封顶 100：DAU 超过 MAU 时按 100 取舍', () => {
    const r = computeStickiness({ todayDau: 120, mau: 100 } as never)
    expect(r.hasData).toBe(true)
    expect(r.value).toBe(100)
  })

  it('mau=0 兜底无数据', () => {
    const r = computeStickiness({ todayDau: 10, mau: 0 } as never)
    expect(r.hasData).toBe(false)
    expect(r.value).toBe(0)
  })

  it('overview 为 null/undefined 按无数据', () => {
    expect(computeStickiness(null)).toEqual({ value: 0, hasData: false })
    expect(computeStickiness(undefined)).toEqual({ value: 0, hasData: false })
  })

  it('todayDau=0 且 mau>0 返回 0%', () => {
    const r = computeStickiness({ todayDau: 0, mau: 100 } as never)
    expect(r.hasData).toBe(true)
    expect(r.value).toBe(0)
  })
})
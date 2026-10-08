/**
 * 管理后台统计指标口径（DESIGN-T04 自 dashboard 组件就地实现下沉）：
 * 指标口径与展示解耦，供视图渲染 / 导出 / 告警等复用点共享，可单测。
 * 作者: wanglx
 */
import type { StatsOverview } from '@/api/stats'

/** 粘性比语义化结果（value 百分比，hasData 是否可展示） */
export interface Stickiness {
  value: number
  hasData: boolean
}

/** DAU/MAU 粘性比（今日 DAU / 近 30 天 MAU），封顶 100；mau<=0 按无数据处理 */
export function computeStickiness(overview: StatsOverview | null | undefined): Stickiness {
  const mau = overview?.mau ?? 0
  if (mau <= 0) return { value: 0, hasData: false }
  return { value: Math.min((overview!.todayDau / mau) * 100, 100), hasData: true }
}
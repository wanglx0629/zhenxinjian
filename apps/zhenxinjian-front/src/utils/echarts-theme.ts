/**
 * ECharts 统一主题（浅色科技风，对齐设计系统 V2.1）
 * 单处维护：色板 / tooltip / 坐标轴 / 网格；所有图表经 baseEchartsOption() 取公共配置
 * 作者: wanglx
 */
import * as echarts from 'echarts/core'

/** 图表主色板（双线起 = 叶绿 + 琥珀；超标系列独立红；科技青作第三辅助） */
export const CHART_PALETTE = ['#00AC7C', '#FFB020', '#0891B2', '#FF4747', '#80D6BE']

/** 统一 tooltip 样式 */
export const CHART_TOOLTIP = {
  backgroundColor: '#ffffff',
  borderColor: '#E3EFE9',
  borderWidth: 1,
  textStyle: { color: '#10312B', fontSize: 13 },
  extraCssText:
    'border-radius:8px;box-shadow:0 4px 18px rgba(0,172,124,.14);padding:10px 12px;'
}

/** 统一坐标轴（浅色科技底） */
export const CHART_AXIS = {
  axisLine: { lineStyle: { color: '#E3EFE9' } },
  axisTick: { show: false },
  axisLabel: { color: '#475569', fontSize: 12 },
  splitLine: { lineStyle: { color: '#f2f6f4' } }
}

/** 图例统一样式 */
export const CHART_LEGEND = {
  textStyle: { color: '#475569', fontSize: 12 },
  itemWidth: 14,
  itemHeight: 8,
  icon: 'roundRect'
}

/**
 * 公共 option（各图按需合并覆盖）
 */
export function baseEchartsOption(): echarts.EChartsCoreOption {
  return {
    color: CHART_PALETTE,
    tooltip: { trigger: 'axis', ...CHART_TOOLTIP },
    legend: CHART_LEGEND,
    grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true }
  }
}

/** 折线面积渐变（传入主色，返回上深下透明的面积色） */
export function areaGradient(color: string, alphaTop = 0.28): echarts.graphic.LinearGradient {
  return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: hexA(color, alphaTop) },
    { offset: 1, color: hexA(color, 0) }
  ])
}

/** hex 转 rgba 字符串（ECharts 渐变用） */
function hexA(hex: string, alpha: number): string {
  const h = hex.replace('#', '')
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

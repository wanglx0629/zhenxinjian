<script setup lang="ts">
/**
 * 体重趋势折线图：每日末值点（同日多条已在后端聚合）。
 * 叶绿折线，末点高亮（白描边圆点），平台点琥珀；底部仅标首/尾日期。
 * 实现：SVG data-uri 经 <image mode="widthFix"> 渲染（mp-weixin 不支持内联 svg，先例 EnergyRing/icons.ts）
 * 作者: wanglx
 */
import { computed } from 'vue'
import type { WeightTrendPoint } from '@/api/weight'
import { BRAND_COLORS } from '@/config/constants'

const props = defineProps<{
  /** 每日末值点（按日期升序，调用方保证 ≥2 点） */
  points: WeightTrendPoint[]
}>()

/** 品牌令牌（单一真源 BRAND_COLORS，与 uni.scss 严格一致，先例 EnergyRing §9.3） */
const GRID = '#EDF1EF'
const MUTED = '#94A3B8'

const W = 320
const H = 180
const PAD_L = 30
const PAD_R = 18
const PAD_T = 16
const PAD_B = 30

const chartSrc = computed(() => {
  const pts = props.points
  const weights = pts.map((p) => p.weight)
  let min = Math.min(...weights)
  let max = Math.max(...weights)
  // 避免单点/水平直线时除零：上下留 0.5kg 余量
  if (max - min < 0.001) {
    min -= 0.5
    max += 0.5
  } else {
    const pad = (max - min) * 0.15
    min -= pad
    max += pad
  }

  const innerW = W - PAD_L - PAD_R
  const innerH = H - PAD_T - PAD_B
  const x = (i: number) =>
    PAD_L + (pts.length === 1 ? innerW / 2 : (i / (pts.length - 1)) * innerW)
  const y = (w: number) => PAD_T + ((max - w) / (max - min)) * innerH

  // 网格线（上中下三条，弱化）
  const gridLines = [0, 0.5, 1]
    .map((t) => {
      const gy = PAD_T + t * innerH
      return `<line x1="${PAD_L}" y1="${gy.toFixed(1)}" x2="${W - PAD_R}" y2="${gy.toFixed(1)}" stroke="${GRID}" stroke-width="1"/>`
    })
    .join('')

  // 折线
  const linePts = pts.map((p, i) => `${x(i).toFixed(1)},${y(p.weight).toFixed(1)}`).join(' ')
  const polyline = `<polyline points="${linePts}" fill="none" stroke="${BRAND_COLORS.primary}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>`

  // 数据点：普通点小叶绿圆；平台点琥珀；末点放大 + 白描边
  const dots = pts
    .map((p, i) => {
      const cx = x(i).toFixed(1)
      const cy = y(p.weight).toFixed(1)
      const isLast = i === pts.length - 1
      const color = p.plateau ? BRAND_COLORS.cta : BRAND_COLORS.primary
      if (isLast) {
        return `<circle cx="${cx}" cy="${cy}" r="6" fill="#fff"/>` +
          `<circle cx="${cx}" cy="${cy}" r="4.2" fill="${color}"/>`
      }
      return `<circle cx="${cx}" cy="${cy}" r="2.6" fill="${color}"/>`
    })
    .join('')

  // 首尾日期轴（MM-DD）
  const shortDate = (d: string) => d.slice(5)
  const firstLabel =
    `<text x="${PAD_L}" y="${H - 8}" font-size="11" fill="${MUTED}">${shortDate(pts[0].date)}</text>`
  const lastLabel =
    `<text x="${W - PAD_R}" y="${H - 8}" font-size="11" fill="${MUTED}" text-anchor="end">${shortDate(pts[pts.length - 1].date)}</text>`

  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}">` +
    gridLines + polyline + dots + firstLabel + lastLabel +
    `</svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
})
</script>

<template>
  <view class="weight-chart">
    <image class="chart-img" :src="chartSrc" mode="widthFix" />
  </view>
</template>

<style scoped lang="scss">
.weight-chart {
  width: 100%;
}

.chart-img {
  width: 100%;
}
</style>

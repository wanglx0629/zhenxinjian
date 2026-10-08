/**
 * 数据看板统计加载 + 图表编排（DESIGN-T03 自 dashboard God 组件下沉）：
 * 总览/趋势/双排行的数据加载与 loding、DAU/MAU 粘性比口径、ECharts 趋势图渲染及生命周期。
 * 作者: wanglx
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import type { Ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getActiveTrend, getEventRank, getOverview, getPageRank } from '@/api/stats'
import type { DailyActive, EventRank, PageRank, StatsOverview } from '@/api/stats'
import { areaGradient, baseEchartsOption, CHART_AXIS } from '@/utils/echarts-theme'
import { computeStickiness } from '@/utils/stats'

/** echarts 按需注册 */
echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

export function useDashboardStats(trendEl: Ref<HTMLElement | undefined>) {
  const overview = ref<StatsOverview | null>(null)
  const overviewLoading = ref(false)
  const trendDays = ref(30)
  const trendData = ref<DailyActive[]>([])
  const trendLoading = ref(false)
  const eventRank = ref<EventRank[]>([])
  const pageRank = ref<PageRank[]>([])
  const rankLoading = ref(false)
  let chart: ReturnType<typeof echarts.init> | null = null

  /** DAU/MAU 粘性比（口径下沉 utils/stats.ts，返回 { value, hasData }） */
  const stickiness = computed(() => computeStickiness(overview.value))

  async function loadOverview() {
    overviewLoading.value = true
    try {
      overview.value = await getOverview()
    } finally {
      overviewLoading.value = false
    }
  }

  function renderTrend() {
    if (!trendEl.value) return
    if (!chart) {
      chart = echarts.init(trendEl.value)
    }
    chart.setOption({
      ...baseEchartsOption(),
      legend: { data: ['DAU', '游客'] },
      grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: trendData.value.map((d) => d.statDate.slice(5)),
        ...CHART_AXIS
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        ...CHART_AXIS
      },
      series: [
        {
          name: 'DAU',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: trendData.value.map((d) => d.dau),
          lineStyle: { width: 3 },
          areaStyle: { color: areaGradient('#00AC7C') }
        },
        {
          name: '游客',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: trendData.value.map((d) => d.guestDau),
          lineStyle: { width: 3 },
          areaStyle: { color: areaGradient('#FFB020', 0.24) }
        }
      ]
    })
  }

  async function loadTrend() {
    trendLoading.value = true
    try {
      trendData.value = await getActiveTrend(trendDays.value)
      renderTrend()
    } finally {
      trendLoading.value = false
    }
  }

  async function loadRanks() {
    rankLoading.value = true
    try {
      const [events, pages] = await Promise.all([getEventRank(7, 10), getPageRank(7, 10)])
      eventRank.value = events
      pageRank.value = pages
    } finally {
      rankLoading.value = false
    }
  }

  function handleResize() {
    chart?.resize()
  }

  onMounted(() => {
    loadOverview()
    loadTrend()
    loadRanks()
    window.addEventListener('resize', handleResize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    chart?.dispose()
    chart = null
  })

  return {
    overview,
    overviewLoading,
    trendDays,
    trendData,
    trendLoading,
    eventRank,
    pageRank,
    rankLoading,
    stickiness,
    loadTrend
  }
}
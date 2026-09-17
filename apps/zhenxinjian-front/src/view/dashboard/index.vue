<script setup lang="ts">
/**
 * 数据看板：KPI 卡片 + DAU 趋势（ECharts 统一主题）+ 功能/页面 TOP10
 * 作者: wanglx
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { User, Calendar, TrendCharts, DataAnalysis } from '@element-plus/icons-vue'
import { getActiveTrend, getEventRank, getOverview, getPageRank } from '@/api/stats'
import type { DailyActive, EventRank, PageRank, StatsOverview } from '@/api/stats'
import { areaGradient, baseEchartsOption, CHART_AXIS } from '@/utils/echarts-theme'

/** echarts 按需注册 */
echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const overview = ref<StatsOverview | null>(null)
const overviewLoading = ref(false)
const trendDays = ref(30)
const trendData = ref<DailyActive[]>([])
const trendLoading = ref(false)
const eventRank = ref<EventRank[]>([])
const pageRank = ref<PageRank[]>([])
const rankLoading = ref(false)
const trendRef = ref<HTMLElement>()
let chart: ReturnType<typeof echarts.init> | null = null

async function loadOverview() {
  overviewLoading.value = true
  try {
    overview.value = await getOverview()
  } finally {
    overviewLoading.value = false
  }
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart) {
    chart = echarts.init(trendRef.value)
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
</script>

<template>
  <div class="dashboard-page">
    <el-row :gutter="16" class="cards" v-loading="overviewLoading">
      <el-col :xs="12" :sm="6">
        <div class="stat-card tech-topline">
          <div class="stat-body">
            <div class="stat-icon icon-leaf">
              <el-icon :size="26"><User /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">
                今日 DAU
                <el-tag size="small" type="warning" effect="light">实时</el-tag>
              </div>
              <div class="card-value num">{{ overview?.todayDau ?? '—' }}</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card tech-topline">
          <div class="stat-body">
            <div class="stat-icon icon-amber">
              <el-icon :size="26"><Calendar /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">昨日 DAU</div>
              <div class="card-value num">{{ overview?.yesterdayDau ?? '—' }}</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card tech-topline">
          <div class="stat-body">
            <div class="stat-icon icon-cyan">
              <el-icon :size="26"><TrendCharts /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">近 30 天 MAU</div>
              <div class="card-value num">{{ overview?.mau ?? '—' }}</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card tech-topline">
          <div class="stat-body">
            <div class="stat-icon icon-mix">
              <el-icon :size="26"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">累计用户 / 饮食记录</div>
              <div class="card-value num">
                {{ overview?.totalUsers ?? '—' }}<span class="slash">/</span>{{
                  overview?.totalDietRecords ?? '—'
                }}
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="block tech-topline" v-loading="trendLoading">
      <div class="block-header">
        <span class="block-title">DAU 趋势</span>
        <el-radio-group size="small" v-model="trendDays" @change="loadTrend">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div class="block-desc">昨日及以前为聚合口径</div>
      <div ref="trendRef" class="trend-chart"></div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="block tech-topline" v-loading="rankLoading">
          <div class="block-title">功能 TOP10</div>
          <div class="block-desc">近 7 天 · 按点击次数</div>
          <el-table :data="eventRank" size="small" class="rank-table">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="eventName" label="功能" min-width="140" />
            <el-table-column prop="pv" label="次数 PV" width="100" align="right" />
            <el-table-column prop="uv" label="人数 UV" width="100" align="right" />
            <template #empty>
              <el-empty description="暂无功能点击数据" :image-size="100" />
            </template>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="block tech-topline" v-loading="rankLoading">
          <div class="block-title">页面 TOP10</div>
          <div class="block-desc">近 7 天 · 按访问量</div>
          <el-table :data="pageRank" size="small" class="rank-table">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="page" label="页面" min-width="160" />
            <el-table-column prop="pv" label="次数 PV" width="100" align="right" />
            <el-table-column prop="uv" label="人数 UV" width="100" align="right" />
            <template #empty>
              <el-empty description="暂无页面访问数据" :image-size="100" />
            </template>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.cards {
  margin-bottom: 16px;
}

.stat-card {
  height: 100%;
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: var(--zhenxinjian-radius-lg);
  padding: 18px 18px 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--zhenxinjian-shadow-card);
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--zhenxinjian-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.icon-leaf {
  background: linear-gradient(135deg, #00AC7C, #33BD96);
}

.icon-amber {
  background: linear-gradient(135deg, #FFB020, #FFC24D);
}

.icon-cyan {
  background: linear-gradient(135deg, #0891B2, #22D3EE);
}

.icon-mix {
  background: linear-gradient(135deg, #00AC7C, #FFB020);
}

.stat-main {
  flex: 1;
  min-width: 0;
}

.card-label {
  font-size: 13px;
  color: var(--zhenxinjian-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-value {
  margin-top: 4px;
  font-size: 30px;
  font-weight: 700;
  color: var(--zhenxinjian-text);
  line-height: 1.1;
}

.slash {
  color: var(--zhenxinjian-text-placeholder);
  margin: 0 6px;
  font-weight: 400;
}

.block {
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: var(--zhenxinjian-radius-lg);
  padding: 18px 20px 14px;
  margin-bottom: 16px;
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.block-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.block-desc {
  font-size: 12px;
  color: var(--zhenxinjian-text-placeholder);
  margin-top: 4px;
}

.trend-chart {
  height: 320px;
  margin-top: 8px;
}

.rank-table {
  margin-top: 10px;
}
</style>

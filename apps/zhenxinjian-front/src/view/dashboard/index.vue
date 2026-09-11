<script setup lang="ts">
/**
 * 数据看板：总览卡片 + DAU 趋势（ECharts）+ 功能/页面 TOP10
 * 作者: wanglx
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getActiveTrend, getEventRank, getOverview, getPageRank } from '@/api/stats'
import type { DailyActive, EventRank, PageRank, StatsOverview } from '@/api/stats'

const overview = ref<StatsOverview | null>(null)
const trendDays = ref(30)
const trendData = ref<DailyActive[]>([])
const eventRank = ref<EventRank[]>([])
const pageRank = ref<PageRank[]>([])
const trendRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function loadOverview() {
  overview.value = await getOverview()
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart) {
    chart = echarts.init(trendRef.value)
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['DAU', '游客'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trendData.value.map((d) => d.statDate.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: 'DAU', type: 'line', smooth: true, data: trendData.value.map((d) => d.dau) },
      { name: '游客', type: 'line', smooth: true, data: trendData.value.map((d) => d.guestDau) }
    ]
  })
}

async function loadTrend() {
  trendData.value = await getActiveTrend(trendDays.value)
  renderTrend()
}

async function loadRanks() {
  eventRank.value = await getEventRank(7, 10)
  pageRank.value = await getPageRank(7, 10)
}

function handleTrendDaysChange() {
  loadTrend()
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
    <el-row :gutter="16" class="cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never">
          <div class="card-label">
            今日 DAU
            <el-tag size="small" type="warning">实时</el-tag>
          </div>
          <div class="card-value">{{ overview?.todayDau ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never">
          <div class="card-label">昨日 DAU</div>
          <div class="card-value">{{ overview?.yesterdayDau ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never">
          <div class="card-label">近 30 天 MAU</div>
          <div class="card-value">{{ overview?.mau ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never">
          <div class="card-label">累计用户 / 饮食记录</div>
          <div class="card-value">
            {{ overview?.totalUsers ?? '-' }} / {{ overview?.totalDietRecords ?? '-' }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="block-header">
          <span>DAU 趋势（昨日及以前为聚合口径）</span>
          <el-radio-group size="small" v-model="trendDays" @change="handleTrendDaysChange">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendRef" class="trend-chart"></div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="block">
          <template #header>
            <span>功能 TOP10（近 7 天，按点击次数）</span>
          </template>
          <el-table :data="eventRank" size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="eventName" label="功能" min-width="140" />
            <el-table-column prop="pv" label="次数（PV）" width="110" align="right" />
            <el-table-column prop="uv" label="人数（UV）" width="110" align="right" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="block">
          <template #header>
            <span>页面 TOP10（近 7 天，按访问量）</span>
          </template>
          <el-table :data="pageRank" size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="page" label="页面" min-width="160" />
            <el-table-column prop="pv" label="次数（PV）" width="110" align="right" />
            <el-table-column prop="uv" label="人数（UV）" width="110" align="right" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.cards {
  margin-bottom: 16px;
}

.card-label {
  font-size: 13px;
  color: var(--zhenxinjian-text-sub, #8a8f99);
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.block {
  margin-bottom: 16px;
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.trend-chart {
  height: 320px;
}
</style>

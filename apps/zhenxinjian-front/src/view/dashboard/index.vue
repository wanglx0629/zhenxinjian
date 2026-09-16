<script setup lang="ts">
/**
 * 数据看板：总览卡片 + DAU 趋势（ECharts）+ 功能/页面 TOP10
 * 作者: wanglx
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { User, UserFilled, TrendCharts, Document } from '@element-plus/icons-vue'
import { getActiveTrend, getEventRank, getOverview, getPageRank } from '@/api/stats'
import type { DailyActive, EventRank, PageRank, StatsOverview } from '@/api/stats'

/** B-T31：echarts 按需注册（趋势图仅需折线图 + 三组件 + Canvas 渲染器，替代全量包） */
echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const overview = ref<StatsOverview | null>(null)
const trendDays = ref(30)
const trendData = ref<DailyActive[]>([])
const eventRank = ref<EventRank[]>([])
const pageRank = ref<PageRank[]>([])
const trendRef = ref<HTMLElement>()
let chart: ReturnType<typeof echarts.init> | null = null

async function loadOverview() {
  overview.value = await getOverview()
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart) {
    chart = echarts.init(trendRef.value)
  }
  chart.setOption({
    color: ['#00AC7C', '#FFB020'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#E3EFE9',
      textStyle: { color: '#10312B' },
      extraCssText: 'border-radius: 8px; box-shadow: 0 4px 16px rgba(0,172,124,0.15);'
    },
    legend: { data: ['DAU', '游客'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trendData.value.map((d) => d.statDate.slice(5)),
      axisLine: { lineStyle: { color: '#E3EFE9' } },
      axisLabel: { color: '#475569' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#F4F8F6' } },
      axisLabel: { color: '#475569' }
    },
    series: [
      {
        name: 'DAU',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 7,
        data: trendData.value.map((d) => d.dau),
        lineStyle: { width: 3 },
        itemStyle: { color: '#00AC7C' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,172,124,0.28)' },
            { offset: 1, color: 'rgba(0,172,124,0)' }
          ])
        }
      },
      {
        name: '游客',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 7,
        data: trendData.value.map((d) => d.guestDau),
        lineStyle: { width: 3 },
        itemStyle: { color: '#FFB020' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,176,32,0.24)' },
            { offset: 1, color: 'rgba(255,176,32,0)' }
          ])
        }
      }
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
        <el-card shadow="hover" class="stat-card stat-card-teal">
          <div class="stat-body">
            <div class="stat-icon">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">
                今日 DAU
                <el-tag size="small" type="warning">实时</el-tag>
              </div>
              <div class="card-value">{{ overview?.todayDau ?? '-' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card stat-card-cyan">
          <div class="stat-body">
            <div class="stat-icon">
              <el-icon :size="28"><UserFilled /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">昨日 DAU</div>
              <div class="card-value">{{ overview?.yesterdayDau ?? '-' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card stat-card-orange">
          <div class="stat-body">
            <div class="stat-icon">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">近 30 天 MAU</div>
              <div class="card-value">{{ overview?.mau ?? '-' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card stat-card-green">
          <div class="stat-body">
            <div class="stat-icon">
              <el-icon :size="28"><Document /></el-icon>
            </div>
            <div class="stat-main">
              <div class="card-label">累计用户 / 饮食记录</div>
              <div class="card-value">
                {{ overview?.totalUsers ?? '-' }} / {{ overview?.totalDietRecords ?? '-' }}
              </div>
            </div>
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

.stat-card {
  border: 1px solid var(--zhenxinjian-border);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.stat-card-teal .stat-icon {
  background: linear-gradient(135deg, #00AC7C, #33BD96);
  box-shadow: 0 4px 12px rgba(0, 172, 124, 0.3);
}

.stat-card-cyan .stat-icon {
  background: linear-gradient(135deg, #0891B2, #22D3EE);
  box-shadow: 0 4px 12px rgba(8, 145, 178, 0.3);
}

.stat-card-orange .stat-icon {
  background: linear-gradient(135deg, #FFB020, #FFC24D);
  box-shadow: 0 4px 12px rgba(255, 176, 32, 0.3);
}

.stat-card-green .stat-icon {
  background: linear-gradient(135deg, #00AC7C, #4ADE80);
  box-shadow: 0 4px 12px rgba(0, 172, 124, 0.3);
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
  margin-top: 6px;
  font-size: 32px;
  font-weight: 700;
  color: var(--zhenxinjian-text);
  line-height: 1.1;
  letter-spacing: -0.5px;
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

<script setup lang="ts">
/**
 * ECharts 示例图表（柱状图）
 * 作者: luote (luote) - https://luote996.cn
 */
import { onMounted, onBeforeUnmount, ref, shallowRef } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsType } from 'echarts/core'

echarts.use([BarChart, GridComponent, TooltipComponent, TitleComponent, CanvasRenderer])

const chartRef = ref<HTMLDivElement | null>(null)
const chartInstance = shallowRef<EChartsType | null>(null)

function renderChart() {
  if (!chartRef.value) {
    return
  }
  if (!chartInstance.value) {
    chartInstance.value = echarts.init(chartRef.value)
  }
  chartInstance.value.setOption({
    title: {
      text: '示例数据（可替换为业务接口）',
      left: 'center',
      textStyle: {
        fontSize: 14,
        color: '#303133',
        fontWeight: 500
      }
    },
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: 40,
      right: 20,
      top: 48,
      bottom: 32
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问量',
        type: 'bar',
        data: [120, 200, 150, 80, 70, 110, 130],
        itemStyle: {
          color: '#409eff',
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: 28
      }
    ]
  })
}

function handleResize() {
  chartInstance.value?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance.value?.dispose()
  chartInstance.value = null
})
</script>

<template>
  <div ref="chartRef" class="demo-chart" />
</template>

<style scoped>
.demo-chart {
  width: 100%;
  height: 320px;
}
</style>

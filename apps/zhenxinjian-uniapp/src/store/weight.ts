/**
 * 体重记录状态管理（体重记录 + 平台判定 + 调碳日志，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  deleteWeight,
  getWeightTrend,
  listWeightAdjustLogs,
  listWeights,
  saveWeight,
  type AdjustLogVO,
  type WeightRecordVO,
  type WeightSaveRequest,
  type WeightTrendVO
} from '@/api/weight'

export const useWeightStore = defineStore('weight', () => {
  /** 体重记录列表（按日期倒序、同日 id 倒序，全量） */
  const records = ref<WeightRecordVO[]>([])
  /** 调碳日志列表（按时间倒序） */
  const logs = ref<AdjustLogVO[]>([])
  /** 体重趋势（当前窗口） */
  const trend = ref<WeightTrendVO | null>(null)
  /** 当前趋势窗口 7/30/60/90/365/all */
  const range = ref<string>('30')
  /** 趋势加载中 */
  const trendLoading = ref(false)
  /** 是否已请求过 */
  const loaded = ref(false)
  /** 提交中（防重复点击） */
  const submitting = ref(false)

  /** 最近一条体重 kg（无记录为 null） */
  const latestWeight = computed(() => records.value[0]?.weight ?? null)
  /** 是否当前处于平台期（以最近记录标记为准） */
  const isPlateau = computed(() => records.value[0]?.plateau === true)
  /** 趋势是否可渲染（窗口内 ≥2 个不同日） */
  const hasTrend = computed(() => !!trend.value?.points?.length)

  /** 拉取体重记录列表 */
  async function fetchRecords() {
    records.value = await listWeights()
    loaded.value = true
    return records.value
  }

  /** 拉取体重趋势（按当前窗口） */
  async function fetchTrend() {
    trendLoading.value = true
    try {
      trend.value = await getWeightTrend(range.value)
      return trend.value
    } finally {
      trendLoading.value = false
    }
  }

  /** 切换窗口并拉取趋势 */
  async function changeRange(next: string) {
    if (range.value === next) return trend.value
    range.value = next
    return fetchTrend()
  }

  /** 拉取调碳日志 */
  async function fetchLogs() {
    logs.value = await listWeightAdjustLogs()
    return logs.value
  }

  /** 保存体重记录（成功后重拉列表与趋势） */
  async function save(data: WeightSaveRequest) {
    submitting.value = true
    try {
      const vo = await saveWeight(data)
      await fetchRecords()
      await fetchTrend().catch(() => undefined)
      return vo
    } finally {
      submitting.value = false
    }
  }

  /** 删除体重记录（成功后重拉列表与趋势） */
  async function remove(id: number) {
    await deleteWeight(id)
    await fetchRecords()
    await fetchTrend().catch(() => undefined)
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    records.value = []
    logs.value = []
    trend.value = null
    range.value = '30'
    trendLoading.value = false
    loaded.value = false
    submitting.value = false
  }

  return {
    records,
    logs,
    trend,
    range,
    trendLoading,
    loaded,
    submitting,
    latestWeight,
    isPlateau,
    hasTrend,
    fetchRecords,
    fetchTrend,
    changeRange,
    fetchLogs,
    save,
    remove,
    reset
  }
})
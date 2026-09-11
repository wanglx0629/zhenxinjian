/**
 * 体重记录状态管理（体重记录 + 平台判定 + 调碳日志，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  deleteWeight,
  listWeightAdjustLogs,
  listWeights,
  saveWeight,
  type AdjustLogVO,
  type WeightRecordVO,
  type WeightSaveRequest
} from '@/api/weight'

export const useWeightStore = defineStore('weight', () => {
  /** 体重记录列表（按日期倒序） */
  const records = ref<WeightRecordVO[]>([])
  /** 调碳日志列表（按时间倒序） */
  const logs = ref<AdjustLogVO[]>([])
  /** 是否已请求过 */
  const loaded = ref(false)
  /** 提交中（防重复点击） */
  const submitting = ref(false)

  /** 最近一条体重 kg（无记录为 null） */
  const latestWeight = computed(() => records.value[0]?.weight ?? null)
  /** 是否当前处于平台期（以最近记录标记为准） */
  const isPlateau = computed(() => records.value[0]?.plateau === true)

  /** 拉取体重记录列表 */
  async function fetchRecords() {
    records.value = await listWeights()
    loaded.value = true
    return records.value
  }

  /** 拉取调碳日志 */
  async function fetchLogs() {
    logs.value = await listWeightAdjustLogs()
    return logs.value
  }

  /** 保存体重记录（成功后重拉列表） */
  async function save(data: WeightSaveRequest) {
    submitting.value = true
    try {
      const vo = await saveWeight(data)
      await fetchRecords()
      return vo
    } finally {
      submitting.value = false
    }
  }

  /** 删除体重记录（成功后重拉列表） */
  async function remove(id: number) {
    await deleteWeight(id)
    await fetchRecords()
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    records.value = []
    logs.value = []
    loaded.value = false
    submitting.value = false
  }

  return {
    records,
    logs,
    loaded,
    submitting,
    latestWeight,
    isPlateau,
    fetchRecords,
    fetchLogs,
    save,
    remove,
    reset
  }
})
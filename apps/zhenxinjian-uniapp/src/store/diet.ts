/**
 * 饮食记录状态管理（P11 添加 / P12 当日记录 + 累计进度，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createDietRecord,
  deleteDietRecord,
  getDietSummary,
  listDietRecords,
  updateDietRecord,
  type DietDayVO,
  type DietRecordCreateRequest,
  type DietRecordUpdateRequest,
  type DietSummaryVO
} from '@/api/diet'
import { useBodyStore } from '@/store/body'
import { ymd } from '@/utils/format'

export const useDietStore = defineStore('diet', () => {
  /** 当前查看日期（YYYY-MM-DD，默认当日） */
  const currentDate = ref(ymd(new Date()))
  /** 当日记录（餐别四组分组 + 小计 + 合计） */
  const dayData = ref<DietDayVO | null>(null)
  /** 当日累计与目标进度 */
  const summary = ref<DietSummaryVO | null>(null)
  /** 是否已请求过 */
  const loaded = ref(false)
  /** 提交中（防重复点击） */
  const submitting = ref(false)

  /** 未建档空态标记（summary.recorded=false 涵盖未建档与碳循环无周期两口径） */
  const noProfile = computed(() => loaded.value && summary.value !== null && !summary.value.recorded)

  /**
   * 空态三分支单一真源（B-T22 自 home/record 双页收敛）：
   * summary.recorded=false 时按 bodyStore 档案与模式分流——
   * 未建档 → showBodyEmpty；已建档且碳循环无周期 → showCycleEmpty
   */
  const showBodyEmpty = computed(() => noProfile.value && !useBodyStore().profile?.recorded)
  const showCycleEmpty = computed(
    () => noProfile.value && !!useBodyStore().profile?.recorded && useBodyStore().isCycleMode
  )

  /** 拉取当日记录与累计（增删改后重拉保证实时） */
  async function fetchDay(date?: string) {
    const d = date || currentDate.value
    currentDate.value = d
    const [day, sum] = await Promise.all([listDietRecords(d), getDietSummary(d)])
    dayData.value = day
    summary.value = sum
    loaded.value = true
  }

  /** 拉取指定日期数据但不切换当前查看日期（首页强制当日，不污染记录页历史日期） */
  async function peekDay(date: string) {
    const [day, sum] = await Promise.all([listDietRecords(date), getDietSummary(date)])
    dayData.value = day
    summary.value = sum
    loaded.value = true
  }

  /** 新增记录（成功后重拉） */
  async function addRecord(data: DietRecordCreateRequest) {
    submitting.value = true
    try {
      await createDietRecord(data)
      await fetchDay()
    } finally {
      submitting.value = false
    }
  }

  /** 编辑记录（成功后重拉） */
  async function editRecord(id: number, data: DietRecordUpdateRequest) {
    submitting.value = true
    try {
      await updateDietRecord(id, data)
      await fetchDay()
    } finally {
      submitting.value = false
    }
  }

  /** 删除记录（成功后重拉） */
  async function removeRecord(id: number) {
    await deleteDietRecord(id)
    await fetchDay()
  }

  /** 切换日期（重新拉取） */
  async function changeDate(date: string) {
    currentDate.value = date
    await fetchDay(date)
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    currentDate.value = ymd(new Date())
    dayData.value = null
    summary.value = null
    loaded.value = false
    submitting.value = false
  }

  return {
    currentDate,
    dayData,
    summary,
    loaded,
    submitting,
    noProfile,
    showBodyEmpty,
    showCycleEmpty,
    fetchDay,
    peekDay,
    addRecord,
    editRecord,
    removeRecord,
    changeDate,
    reset
  }
})

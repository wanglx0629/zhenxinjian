/**
 * 三餐饮食提醒状态管理（P13 提醒设置 + 我的页提醒副标题，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getReminder,
  saveReminder,
  reportSubscribe,
  type ReminderSaveRequest,
  type ReminderVO
} from '@/api/reminder'

export const useReminderStore = defineStore('reminder', () => {
  /** 提醒设置视图（无记录时后端按默认值落库） */
  const vo = ref<ReminderVO | null>(null)
  /** 是否已请求过（区分「未加载」与「已加载」） */
  const loaded = ref(false)
  /** 提交中（防重复点击） */
  const submitting = ref(false)

  /** 总开关是否开启（我的页副标题口径） */
  const masterEnabled = computed(() => vo.value?.masterSwitch === 1)

  /** 拉取提醒设置 */
  async function fetch() {
    vo.value = await getReminder()
    loaded.value = true
    return vo.value
  }

  /** 整体保存提醒设置（返回保存后视图覆盖本地） */
  async function save(data: ReminderSaveRequest) {
    submitting.value = true
    try {
      vo.value = await saveReminder(data)
      loaded.value = true
      return vo.value
    } finally {
      submitting.value = false
    }
  }

  /** 上报订阅授权（额度 +1，同分钟内重复上报幂等去重） */
  async function report() {
    await reportSubscribe()
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    vo.value = null
    loaded.value = false
    submitting.value = false
  }

  return {
    vo,
    loaded,
    submitting,
    masterEnabled,
    fetch,
    save,
    report,
    reset
  }
})

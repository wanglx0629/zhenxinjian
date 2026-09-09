/**
 * 身体数据状态管理（P03 录入 / P04 结果，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getBodyProfile, saveBodyProfile, type BodyProfileResult, type BodyProfileSaveRequest } from '@/api/body'

export const useBodyStore = defineStore('body', () => {
  /** 当前档案与计算结果（以后端返回为准，前端预览值提交后由本字段覆盖） */
  const profile = ref<BodyProfileResult | null>(null)
  /** 是否已请求过（区分「未加载」与「已加载但空态」） */
  const loaded = ref(false)
  /** 提交中（防重复点击） */
  const submitting = ref(false)

  /** 空态标识：已加载且未录入 */
  const isEmpty = computed(() => loaded.value && !profile.value?.recorded)
  /** 免责声明（后端统一下发，页面不可移除） */
  const disclaimer = computed(() => profile.value?.disclaimer || '')

  /** 拉取当前档案（未录入返回空态，不抛错） */
  async function fetchProfile() {
    profile.value = await getBodyProfile()
    loaded.value = true
  }

  /**
   * 保存档案（录入/修改一体）
   * 成功后以后端返回覆盖本地（含重算快照、风险标记、免责声明）
   */
  async function saveProfile(data: BodyProfileSaveRequest) {
    submitting.value = true
    try {
      profile.value = await saveBodyProfile(data)
      loaded.value = true
      return profile.value
    } finally {
      submitting.value = false
    }
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    profile.value = null
    loaded.value = false
    submitting.value = false
  }

  return {
    profile,
    loaded,
    submitting,
    isEmpty,
    disclaimer,
    fetchProfile,
    saveProfile,
    reset
  }
})

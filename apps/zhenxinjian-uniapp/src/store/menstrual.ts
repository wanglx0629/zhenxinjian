/**
 * 经期管理状态管理（经期设置查询/保存，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getMenstrual, saveMenstrual, type MenstrualSaveRequest, type MenstrualVO } from '@/api/menstrual'
import { ymd } from '@/utils/format'

export const useMenstrualStore = defineStore('menstrual', () => {
  /** 经期设置视图（男性/未建档 applicable=false） */
  const vo = ref<MenstrualVO | null>(null)
  /** 是否已请求过（区分「未加载」与「已加载但空态」） */
  const loaded = ref(false)
  /** 数据所属日期（阶段徽标服务端按拉取日计算，跨天即失效需重拉） */
  const loadedDate = ref('')
  /** 提交中（防重复点击） */
  const submitting = ref(false)

  /** 是否适用（女性 true） */
  const applicable = computed(() => !!vo.value?.applicable)
  /** 是否已开启经期管理 */
  const enabled = computed(() => vo.value?.enabled === 1)
  /** 当前阶段徽标（女性开启且有阶段时返回名称，否则空） */
  const phaseName = computed(() =>
    vo.value?.applicable && vo.value?.enabled === 1 ? vo.value?.phaseName || '' : ''
  )

  /** 数据已为当日新鲜（tab 切回据此跳过重拉；跨天自动失效） */
  const freshToday = computed(() => loaded.value && loadedDate.value === ymd(new Date()))

  /** 拉取经期设置（男性/未建档返回空态，不抛错） */
  async function fetch() {
    vo.value = await getMenstrual()
    loaded.value = true
    loadedDate.value = ymd(new Date())
    return vo.value
  }

  /** 保存经期设置（返回保存后的视图覆盖本地） */
  async function save(data: MenstrualSaveRequest) {
    submitting.value = true
    try {
      vo.value = await saveMenstrual(data)
      loaded.value = true
      loadedDate.value = ymd(new Date())
      return vo.value
    } finally {
      submitting.value = false
    }
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    vo.value = null
    loaded.value = false
    loadedDate.value = ''
    submitting.value = false
  }

  return {
    vo,
    loaded,
    freshToday,
    submitting,
    applicable,
    enabled,
    phaseName,
    fetch,
    save,
    reset
  }
})
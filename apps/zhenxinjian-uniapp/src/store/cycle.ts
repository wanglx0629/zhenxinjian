/**
 * 碳循环状态管理（P05 模式选择 / P06 周期设置 / P07 周期计划，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createCyclePlan,
  getCurrentCyclePlan,
  terminateCurrentCyclePlan,
  type CyclePlanCreateRequest,
  type CyclePlanVO
} from '@/api/cycle'
import { CYCLE } from '@/config/constants'

/** cfc 上次选择本地持久化键（zxj_ 前缀规范） */
const CFC_KEY = 'zxj_cycle_cfc'

export const useCycleStore = defineStore('cycle', () => {
  /** 当前进行中周期（含逐日计划；无周期时 id=null） */
  const currentPlan = ref<CyclePlanVO | null>(null)
  /** 是否已请求过（区分「未加载」与「已加载但无周期」） */
  const loaded = ref(false)
  /** 创建提交中（防重复点击） */
  const creating = ref(false)
  /** 上次选择的脂肪系数（本地持久化，不随周期重置） */
  const lastCfc = ref<number>(Number(uni.getStorageSync(CFC_KEY)) || CYCLE.fatCoefDefault)

  /** 无进行中周期空态标记：已加载且（无计划 或 计划 id 为空） */
  const noPlan = computed(() => loaded.value && !currentPlan.value?.id)
  /** 逐日计划 */
  const days = computed(() => currentPlan.value?.days ?? [])
  /** 今日日序（今日在周期内时 1..N） */
  const todayIndex = computed(() => currentPlan.value?.todayIndex ?? null)
  /** 今日日型计划（无周期/今日不在周期内为 null） */
  const todayDay = computed(() => {
    const idx = todayIndex.value
    if (!idx) return null
    return days.value.find(d => d.dayIndex === idx) ?? null
  })

  /** 拉取当前进行中周期（无周期返回空态，不抛错） */
  async function fetchCurrent() {
    currentPlan.value = await getCurrentCyclePlan()
    loaded.value = true
  }

  /** 持久化脂肪系数选择（切换即持久化，创建失败不丢失） */
  function setCfc(v: number) {
    lastCfc.value = v
    uni.setStorageSync(CFC_KEY, String(v))
  }

  /** 创建周期（成功后覆盖本地并持久化 cfc 选择） */
  async function create(data: CyclePlanCreateRequest) {
    creating.value = true
    try {
      currentPlan.value = await createCyclePlan(data)
      loaded.value = true
      if (data.cfc) {
        setCfc(data.cfc)
      }
      return currentPlan.value
    } finally {
      creating.value = false
    }
  }

  /** 终止当前周期（成功后置空态） */
  async function terminate() {
    await terminateCurrentCyclePlan()
    currentPlan.value = null
    loaded.value = true
  }

  /** 清空本地状态（退出登录/切换身份时调用；cfc 选择保留） */
  function reset() {
    currentPlan.value = null
    loaded.value = false
    creating.value = false
  }

  return {
    currentPlan,
    loaded,
    creating,
    lastCfc,
    noPlan,
    days,
    todayIndex,
    todayDay,
    fetchCurrent,
    create,
    terminate,
    reset,
    setCfc
  }
})

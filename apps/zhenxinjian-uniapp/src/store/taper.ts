/**
 * 532 碳水渐降计划状态管理（P08 四阶段计划卡 + 今日目标，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTaper532Plan, type Taper532Plan } from '@/api/taper'

export const useTaperStore = defineStore('taper', () => {
  /** 532 四阶段计划卡与今日目标（未建档 today=null） */
  const plan = ref<Taper532Plan | null>(null)
  /** 是否已请求过（区分「未加载」与「已加载但空态」） */
  const loaded = ref(false)

  /** 拉取 532 计划（失败抛错由页面捕获，request.ts 已统一 toast） */
  async function fetch() {
    plan.value = await getTaper532Plan()
    loaded.value = true
    return plan.value
  }

  /** 清空本地状态（退出登录/切换身份时调用） */
  function reset() {
    plan.value = null
    loaded.value = false
  }

  return {
    plan,
    loaded,
    fetch,
    reset
  }
})

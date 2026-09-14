/**
 * 用户状态管理（微信授权登录 + 游客身份，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserInfo, WechatProfilePayload } from '@/api/types'
import { createGuest, getCurrentUser, logout as logoutApi, wechatLogin } from '@/api/auth'
import { TRACK_EVENT } from '@/config/track-events'
import { useBodyStore } from '@/store/body'
import { useCycleStore } from '@/store/cycle'
import { useDietStore } from '@/store/diet'
import { useMenstrualStore } from '@/store/menstrual'
import { useReminderStore } from '@/store/reminder'
import { useTaperStore } from '@/store/taper'
import { useWeightStore } from '@/store/weight'
import { track } from '@/utils/track'
import {
  getGuestKey,
  getToken,
  removeGuestKey,
  removeToken,
  setGuestKey,
  setToken
} from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(null)

  /** 用户类型：GUEST 游客 / WECHAT 微信正式用户 */
  const userType = computed(() => userInfo.value?.userType || '')
  const isGuest = computed(() => userType.value === 'GUEST')

  /** 是否已登录（游客或正式用户均视为已登录） */
  const isLoggedIn = () => !!token.value

  /**
   * 微信授权登录全流程（B-T24：uni.login → code → wechatLogin 收敛 store 单一入口）
   * 携带本地 guestKey 触发游客数据迁移（后端幂等）；
   * 成功自动 toast + 跳首页，失败自动 toast + track，页面只需管 loading 与节流
   *
   * @param profile 授权页采集的昵称 + 头像本地路径（chooseAvatar / nickname input）
   */
  async function loginByWechat(
    profile: WechatProfilePayload,
    opts?: { successText?: string; failText?: string }
  ): Promise<boolean> {
    try {
      // 注意：部分平台 Promise 形式返回 [err, res] 数组，需兼容取值
      const result: unknown = await uni.login({ provider: 'weixin' })
      const loginRes = (Array.isArray(result) ? result[1] : result) as { code?: string }
      const code = loginRes?.code
      if (!code) {
        track(TRACK_EVENT.LOGIN_FAIL)
        uni.showToast({ title: '获取登录凭证失败，请重试', icon: 'none' })
        return false
      }
      const guestKey = getGuestKey()
      const loginResult = await wechatLogin({
        code,
        nickname: profile.nickname,
        avatarPath: profile.avatarPath,
        ...(guestKey ? { guestKey } : {})
      })
      token.value = loginResult.token
      userInfo.value = loginResult.user
      setToken(loginResult.token)
      // 登录成功即完成迁移，游客标识使命结束
      removeGuestKey()
      track(TRACK_EVENT.LOGIN_WECHAT)
      uni.showToast({ title: opts?.successText || '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/home/index' })
      }, 400)
      return true
    } catch {
      track(TRACK_EVENT.LOGIN_FAIL)
      uni.showToast({ title: opts?.failText || '登录失败，请重试', icon: 'none' })
      return false
    }
  }

  /**
   * 游客身份签发/复用：无 guestKey 签发新游客（3 天）；
   * 带有效 guestKey 复用记录且不重置起算时刻
   */
  async function loginAsGuest() {
    const result = await createGuest(getGuestKey() || undefined)
    token.value = result.token
    userInfo.value = result.user
    setToken(result.token)
    setGuestKey(result.guestKey)
  }

  /**
   * 放弃游客数据退出：清本地登录态与游客标识，回引导页
   * （服务端数据保留至 7 天清空期限，期间重新授权仍可找回）
   */
  function abandonGuest() {
    resetBusinessStores()
    token.value = ''
    userInfo.value = null
    removeToken()
    removeGuestKey()
    uni.reLaunch({ url: '/pages/auth/guide' })
  }

  /**
   * 退出登录：正式用户先调登出接口（token 黑名单）；
   * 游客/接口失败均清本地态回引导页
   */
  async function logout() {
    if (token.value && userType.value !== 'GUEST') {
      try {
        await logoutApi()
      } catch {
        // 接口失败也继续清本地态
      }
    }
    resetBusinessStores()
    token.value = ''
    userInfo.value = null
    removeToken()
    removeGuestKey()
    uni.reLaunch({ url: '/pages/auth/guide' })
  }

  /** 清空全部业务 store 本地态（登出/放弃游客统一编排，页面不再逐个 reset） */
  function resetBusinessStores() {
    useDietStore().reset()
    useBodyStore().reset()
    useCycleStore().reset()
    useWeightStore().reset()
    useMenstrualStore().reset()
    useReminderStore().reset()
    useTaperStore().reset()
  }

  /** 续期 token 同步内存态（api 层 AuthHooks 注入点，request.ts 去 store 化） */
  function syncToken(t: string) {
    token.value = t
  }

  /** 清会话内存态（api 层 401/游客到期 AuthHooks 注入点；storage 清理与页面跳转由 request 负责） */
  function clearSession() {
    token.value = ''
    userInfo.value = null
  }

  /** 拉取当前用户 */
  async function fetchUserInfo() {
    if (!token.value) {
      return
    }
    userInfo.value = await getCurrentUser()
  }

  return {
    token,
    userInfo,
    userType,
    isGuest,
    isLoggedIn,
    loginByWechat,
    loginAsGuest,
    abandonGuest,
    logout,
    fetchUserInfo,
    syncToken,
    clearSession
  }
})

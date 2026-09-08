/**
 * 用户状态管理（微信授权登录 + 游客身份，小程序端）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserInfo } from '@/api/types'
import { createGuest, getCurrentUser, logout as logoutApi, wechatLogin } from '@/api/auth'
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
   * 微信授权登录：wx.login() 的 code 换登录态
   * 携带本地 guestKey 触发游客数据迁移（后端幂等）
   */
  async function loginByWechat(code: string) {
    const guestKey = getGuestKey()
    const result = await wechatLogin(guestKey ? { code, guestKey } : { code })
    token.value = result.token
    userInfo.value = result.user
    setToken(result.token)
    // 登录成功即完成迁移，游客标识使命结束
    removeGuestKey()
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
    token.value = ''
    userInfo.value = null
    removeToken()
    removeGuestKey()
    uni.reLaunch({ url: '/pages/auth/guide' })
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
    fetchUserInfo
  }
})

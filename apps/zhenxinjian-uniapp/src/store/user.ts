/**
 * 用户状态管理
 * 作者: luote (luote) - https://luote996.cn
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginRequest, UserInfo } from '@/api/types'
import { getCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(null)

  /** 是否已登录 */
  const isLoggedIn = () => !!token.value

  /** 登录 */
  async function login(data: LoginRequest) {
    const result = await loginApi(data)
    token.value = result.token
    userInfo.value = result.user
    setToken(result.token)
  }

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      removeToken()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }

  /** 拉取当前用户 */
  async function fetchUserInfo() {
    if (!token.value) {
      return
    }
    userInfo.value = await getCurrentUser()
  }

  return { token, userInfo, isLoggedIn, login, logout, fetchUserInfo }
})

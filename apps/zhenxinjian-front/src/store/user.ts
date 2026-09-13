/**
 * 用户状态管理
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/api/types'
import { getCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  // store 为登录态唯一真源，localStorage 仅作持久化介质（初始化时恢复）
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  /** 是否已登录 */
  const isLoggedIn = () => !!token.value

  /** 登录 */
  async function login(data: LoginRequest) {
    const result = await loginApi(data)
    token.value = result.token
    userInfo.value = result.user
    localStorage.setItem('token', result.token)
  }

  /** 清会话单一入口：清内存态与持久化介质，不含导航（导航属调用方/组合根职责） */
  function clearSession() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  /** 续期同步：响应头 x-refresh-token 经 request 层 hooks 回调至此 */
  function syncToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearSession()
    }
  }

  /** 获取当前用户信息 */
  async function fetchUserInfo() {
    if (!token.value) return
    userInfo.value = await getCurrentUser()
  }

  return { token, userInfo, isLoggedIn, login, logout, clearSession, syncToken, fetchUserInfo }
})

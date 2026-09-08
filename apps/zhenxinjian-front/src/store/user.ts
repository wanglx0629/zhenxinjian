/**
 * 用户状态管理
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/api/types'
import { getCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest } from '@/api/types'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
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

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      router.push('/login')
    }
  }

  /** 获取当前用户信息 */
  async function fetchUserInfo() {
    if (!token.value) return
    userInfo.value = await getCurrentUser()
  }

  return { token, userInfo, isLoggedIn, login, logout, fetchUserInfo }
})

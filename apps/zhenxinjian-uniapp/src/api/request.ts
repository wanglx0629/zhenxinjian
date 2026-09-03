/**
 * uni.request 统一封装
 * 作者: luote (luote) - https://luote996.cn
 */
import type { Result } from './types'
import { getToken, removeToken, setToken } from '@/utils/storage'
import { useUserStore } from '@/store/user'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

/** 登录相关接口不附带 Authorization */
const AUTH_SKIP_URLS = ['/auth/login', '/auth/register', '/auth/captcha']

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
  header?: Record<string, string>
}

/**
 * 发起请求并解包 data
 */
function request<T>(options: RequestOptions): Promise<T> {
  const skipAuth = AUTH_SKIP_URLS.some((path) => options.url.includes(path))
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.header || {})
  }
  if (!skipAuth) {
    const token = getToken()
    if (token) {
      header.Authorization = `Bearer ${token}`
    }
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data as UniApp.RequestOptions['data'],
      header,
      timeout: 15000,
      success: (res) => {
        const status = res.statusCode || 0
        if (status === 401) {
          clearSessionAndGoLogin()
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          reject(new Error('unauthorized'))
          return
        }
        // 续期 Token（部分端 header 键名为小写）
        const headers = (res.header || {}) as Record<string, string>
        const newToken = headers['X-Refresh-Token'] || headers['x-refresh-token']
        if (newToken) {
          setToken(newToken)
          try {
            useUserStore().token = newToken
          } catch {
            // Pinia 未就绪时仅写 storage
          }
        }
        const body = res.data as Result<T>
        if (!body || typeof body !== 'object') {
          uni.showToast({ title: '响应异常', icon: 'none' })
          reject(new Error('invalid response'))
          return
        }
        if (body.code !== 200) {
          if (body.code === 401) {
            clearSessionAndGoLogin()
          }
          uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message || 'request failed'))
          return
        }
        resolve(body.data)
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 清会话并跳转登录页
 */
function clearSessionAndGoLogin() {
  removeToken()
  try {
    const userStore = useUserStore()
    userStore.token = ''
    userStore.userInfo = null
  } catch {
    // ignore
  }
  uni.reLaunch({ url: '/pages/login/index' })
}

const http = {
  get<T>(url: string, data?: unknown) {
    return request<T>({ url, method: 'GET', data })
  },
  post<T>(url: string, data?: unknown) {
    return request<T>({ url, method: 'POST', data })
  },
  put<T>(url: string, data?: unknown) {
    return request<T>({ url, method: 'PUT', data })
  },
  delete<T>(url: string, data?: unknown) {
    return request<T>({ url, method: 'DELETE', data })
  }
}

export default http

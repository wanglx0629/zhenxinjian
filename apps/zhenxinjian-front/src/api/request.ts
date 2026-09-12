/**
 * Axios 请求封装
 * 作者: wanglx
 */
import axios, { type AxiosRequestConfig } from 'axios'
import type { Result } from './types'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

/** 登录相关接口不附带 Authorization，避免过期 Token 干扰白名单 */
const AUTH_SKIP_URLS = ['/auth/login', '/auth/captcha']

/**
 * 清会话并跳转登录
 */
function clearSessionAndGoLogin(message = '登录已过期，请重新登录') {
  localStorage.removeItem('token')
  try {
    const userStore = useUserStore()
    userStore.token = ''
    userStore.userInfo = null
  } catch {
    // ignore
  }
  router.push('/login')
  ElMessage.error(message)
}

// 请求拦截：附加 Token
instance.interceptors.request.use((config) => {
  const url = config.url || ''
  const skipAuth = AUTH_SKIP_URLS.some((path) => url.includes(path))
  if (!skipAuth) {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

// 响应拦截：统一错误处理
instance.interceptors.response.use(
  (response) => {
    const newToken = response.headers['x-refresh-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
      // 同步 Pinia，避免其他模块仍使用旧 token
      try {
        const userStore = useUserStore()
        userStore.token = newToken
      } catch {
        // Pinia 未就绪时仅写 localStorage
      }
    }
    const res = response.data as Result
    // 后端业务码 401 也可能挂在 HTTP 200 上，需与 HTTP 401 同等处理
    if (res.code === 401) {
      clearSessionAndGoLogin(res.message || '登录已过期，请重新登录')
      return Promise.reject(new Error(res.message || 'unauthorized'))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    response.data = res
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      clearSessionAndGoLogin()
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

/** 统一请求方法，自动解包 data */
const request = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return instance.get<Result<T>>(url, config).then((res) => res.data.data)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.post<Result<T>>(url, data, config).then((res) => res.data.data)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.put<Result<T>>(url, data, config).then((res) => res.data.data)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return instance.delete<Result<T>>(url, config).then((res) => res.data.data)
  }
}

export default request

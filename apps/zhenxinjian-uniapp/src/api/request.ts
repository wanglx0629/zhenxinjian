/**
 * uni.request 统一封装
 * 作者: wanglx
 *
 * 分层约束：api 层不依赖 store——401/游客到期的会话内存态清理与续期 token 的
 * 内存同步经 AuthHooks 回调注入（main.ts 组合根装配 user store），
 * 消除 store→api→store 循环依赖
 */
import type { Result } from './types'
import { getToken, removeToken, setToken } from '@/utils/storage'

/** API 服务地址（api 层单一来源，track.ts 经此处引用） */
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

/** 业务错误码：游客体验已到期 */
const GUEST_EXPIRED_CODE = 40201

/** 登录相关接口不附带 Authorization */
const AUTH_SKIP_URLS = ['/auth/wechat/login', '/auth/guest']

/**
 * 会话事件钩子（api 层去 store 化的注入点，main.ts 组合根装配）
 * onSessionClear：401/游客到期清内存会话态（storage 清理与页面跳转由本层负责）
 * onTokenRefreshed：服务端续期 token 同步内存态
 */
export interface AuthHooks {
  onSessionClear: () => void
  onTokenRefreshed: (token: string) => void
}

let authHooks: AuthHooks | null = null

/** 装配会话事件钩子（应用启动时调用一次；未装配时仅做 storage 层处理，语义等价原 Pinia 未就绪分支） */
export function setAuthHooks(hooks: AuthHooks) {
  authHooks = hooks
}

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
          authHooks?.onTokenRefreshed(newToken)
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
          // 游客到期：清登录态并跳强制授权页（spec：到期强制授权拦截）
          if (body.code === GUEST_EXPIRED_CODE) {
            clearSessionAndGoExpire()
            reject(new Error('guest expired'))
            return
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
  authHooks?.onSessionClear()
  uni.reLaunch({ url: '/pages/auth/guide' })
}

/**
 * 游客到期：清登录态（保留 guestKey，7 天内授权仍可迁移）并跳强制授权页
 */
function clearSessionAndGoExpire() {
  removeToken()
  authHooks?.onSessionClear()
  uni.reLaunch({ url: '/pages/auth/expire' })
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

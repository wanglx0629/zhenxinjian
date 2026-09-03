/**
 * WebSocket 工具（H5 用子协议，小程序用 Authorization）
 * 作者: luote (luote) - https://luote996.cn
 */
import { getToken } from '@/utils/storage'

export interface WsMessage {
  type: string
  from: string
  content: string
  time: string
  online?: number
}

/**
 * 组装 WebSocket URL（绝对地址，兼容小程序）
 */
export function buildWsUrl(): string {
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const wsPath = import.meta.env.VITE_WS_PATH || '/api/ws/demo'
  let origin = ''
  try {
    const u = new URL(apiBase)
    origin = `${u.protocol === 'https:' ? 'wss:' : 'ws:'}//${u.host}`
  } catch {
    origin = 'ws://localhost:8080'
  }
  const path = wsPath.startsWith('/') ? wsPath : `/${wsPath}`
  return `${origin}${path}`
}

/**
 * 当前子协议名
 */
export function resolveWsProtocol(): string {
  return import.meta.env.VITE_WS_PROTOCOL || 'bearer'
}

/**
 * 是否 H5（可用 Sec-WebSocket-Protocol）
 */
export function isH5Platform(): boolean {
  try {
    const info = uni.getSystemInfoSync() as UniNamespace.GetSystemInfoResult & {
      uniPlatform?: string
    }
    const platform = (info.uniPlatform || '').toLowerCase()
    return platform === 'web' || platform === 'h5'
  } catch {
    return false
  }
}

export interface ConnectSocketOptions {
  onOpen?: () => void
  onMessage?: (data: WsMessage | string) => void
  onClose?: () => void
  onError?: () => void
}

/**
 * 建立带鉴权的 WebSocket 连接
 */
export function connectAuthedSocket(options: ConnectSocketOptions) {
  const token = getToken()
  if (!token) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return null
  }
  const url = buildWsUrl()
  const protocol = resolveWsProtocol()
  const useProtocol = isH5Platform()

  const task = uni.connectSocket({
    url,
    protocols: useProtocol ? [protocol, token] : [],
    header: useProtocol
      ? {}
      : {
          Authorization: `Bearer ${token}`
        },
    complete: () => undefined
  })

  task.onOpen(() => {
    options.onOpen?.()
  })
  task.onMessage((res) => {
    const raw = typeof res.data === 'string' ? res.data : String(res.data)
    try {
      options.onMessage?.(JSON.parse(raw) as WsMessage)
    } catch {
      options.onMessage?.(raw)
    }
  })
  task.onClose(() => {
    options.onClose?.()
  })
  task.onError(() => {
    options.onError?.()
  })
  return task
}

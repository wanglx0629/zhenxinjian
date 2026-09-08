/**
 * WebSocket 地址与鉴权工具（Token 走子协议，不进 query）
 * 作者: wanglx
 */

/**
 * 组装 WebSocket 地址（开发走同源代理，生产可用绝对路径）
 */
export function buildWsUrl() {
  const wsPath = import.meta.env.VITE_WS_PATH || '/api/ws/demo'
  const path = wsPath.startsWith('/') ? wsPath : `/${wsPath}`
  if (path.startsWith('ws://') || path.startsWith('wss://')) {
    return path
  }
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path.replace(/^http/, 'ws')
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}${path}`
}

/**
 * 子协议名（须与后端 zhenxinjian.websocket.protocol 一致）
 */
export function resolveWsProtocol() {
  return import.meta.env.VITE_WS_PROTOCOL || 'bearer'
}

/**
 * 展示用路径（不含协议主机）
 */
export function resolveWsPathLabel() {
  const wsPath = import.meta.env.VITE_WS_PATH || '/api/ws/demo'
  if (wsPath.startsWith('ws://') || wsPath.startsWith('wss://') || wsPath.startsWith('http')) {
    try {
      return new URL(wsPath.replace(/^ws/, 'http')).pathname
    } catch {
      return wsPath
    }
  }
  return wsPath.startsWith('/') ? wsPath : `/${wsPath}`
}

/**
 * 埋点上报 API（静默上报：失败不弹 toast、不触发登录跳转，队列侧自行重试）
 * 作者: wanglx
 *
 * 与 request.ts 共享 BASE_URL 单一来源；独立裸 uni.request 属刻意——
 * 静默语义（不 toast/不 401 跳转）与 10s 短超时不同于业务请求 15s 默认
 */
import type { Result } from './types'
import { BASE_URL } from './request'
import { getToken } from '@/utils/storage'

export interface TrackEventItem {
  eventCode: string
  page?: string
  extra?: Record<string, unknown>
  clientTime?: string
}

/**
 * 批量上报埋点事件（单批≤50；登录态可选，有 token 附带、无也放行）
 * 成功 resolve 被服务端剔除的非法事件码列表（毒性隔离，端上永久丢弃不再重试）；
 * 网络/5xx 类失败 reject（可重试失败，端上整批保留）
 */
export function reportEvents(events: TrackEventItem[]): Promise<string[]> {
  const header: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) {
    header.Authorization = `Bearer ${token}`
  }
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/track/events`,
      method: 'POST',
      data: { events },
      header,
      timeout: 10000,
      success: (res) => {
        const body = res.data as Result<string[]>
        if (res.statusCode === 200 && body && body.code === 200) {
          resolve(Array.isArray(body.data) ? body.data : [])
        } else {
          reject(new Error(body?.message || 'track failed'))
        }
      },
      fail: (err) => reject(err)
    })
  })
}

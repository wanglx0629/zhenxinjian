/**
 * 认证相关 API
 * 作者: wanglx
 */
import http from './request'
import type { GuestLoginResult, LoginResult, UserInfo, WechatLoginRequest } from './types'

/** 用户登出 */
export function logout() {
  return http.post<void>('/auth/logout')
}

/** 获取当前用户 */
export function getCurrentUser() {
  return http.get<UserInfo>('/auth/me')
}

/** 微信授权登录（code 换 openid；可选 guestKey 触发游客数据迁移） */
export function wechatLogin(data: WechatLoginRequest) {
  return http.post<LoginResult>('/auth/wechat/login', data)
}

/** 游客身份签发/复用（带既有有效 guestKey 复用记录，不重置 3 天起算） */
export function createGuest(guestKey?: string) {
  return http.post<GuestLoginResult>('/auth/guest', guestKey ? { guestKey } : {})
}

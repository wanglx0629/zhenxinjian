/**
 * 认证相关 API
 * 作者: wanglx
 */
import http from './request'
import type {
  CaptchaResult,
  GuestLoginResult,
  LoginRequest,
  LoginResult,
  RegisterRequest,
  UserInfo,
  WechatLoginRequest
} from './types'

/** 获取图形验证码 */
export function getCaptcha() {
  return http.get<CaptchaResult>('/auth/captcha')
}

/** 用户登录 */
export function login(data: LoginRequest) {
  return http.post<LoginResult>('/auth/login', data)
}

/** 用户注册 */
export function register(data: RegisterRequest) {
  return http.post<void>('/auth/register', data)
}

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

/** 游客身份续用（同 createGuest，语义化别名：启动时带 guestKey 换新 token） */
export function guestRenew(guestKey: string) {
  return createGuest(guestKey)
}

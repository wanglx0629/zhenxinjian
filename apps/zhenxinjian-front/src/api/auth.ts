/**
 * 认证相关 API
 * 作者: wanglx
 */
import request from './request'
import type { CaptchaResult, LoginRequest, LoginResult, UserInfo } from './types'

/** 获取图形验证码 */
export function getCaptcha() {
  return request.get<CaptchaResult>('/auth/captcha')
}

/** 用户登录 */
export function login(data: LoginRequest) {
  return request.post<LoginResult>('/auth/login', data)
}

/** 用户登出 */
export function logout() {
  return request.post<void>('/auth/logout')
}

/** 获取当前用户 */
export function getCurrentUser() {
  return request.get<UserInfo>('/auth/me')
}

/**
 * 认证相关 API
 * 作者: luote (luote) - https://luote996.cn
 */
import http from './request'
import type { CaptchaResult, LoginRequest, LoginResult, RegisterRequest, UserInfo } from './types'

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

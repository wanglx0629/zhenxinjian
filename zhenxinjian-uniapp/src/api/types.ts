/**
 * API 类型定义（与 Web 端 / 后端约定对齐）
 * 作者: luote (luote) - https://luote996.cn
 */

/** 统一响应结构 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaUuid: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  captcha: string
  captchaUuid: string
}

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  role?: string
  status?: number
  gender?: number
  lastLoginTime?: string
  createTime?: string
}

/** 登录响应 */
export interface LoginResult {
  token: string
  user: UserInfo
}

/** 验证码响应 */
export interface CaptchaResult {
  uuid: string
  image: string
}

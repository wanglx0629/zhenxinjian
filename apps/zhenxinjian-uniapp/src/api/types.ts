/**
 * API 类型定义（与 Web 端 / 后端约定对齐）
 * 作者: wanglx
 */

/** 统一响应结构 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
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
  userType?: string
  guestExpireAt?: string
  lastLoginTime?: string
  createTime?: string
}

/** 登录响应 */
export interface LoginResult {
  token: string
  user: UserInfo
}

/** 游客登录响应 */
export interface GuestLoginResult extends LoginResult {
  guestKey: string
}

/** 微信登录请求 */
export interface WechatLoginRequest {
  code: string
  guestKey?: string
}

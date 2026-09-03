/**
 * API 类型定义
 * 作者: luote (luote) - https://luote996.cn
 */

/** 统一响应结构 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页查询基类（对应后端 PageQuery） */
export interface PageQuery {
  page?: number
  size?: number
}

/** 用户分页查询（对应后端 UserQuery） */
export interface UserQuery extends PageQuery {
  keyword?: string
  status?: number
  role?: string
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
  remark?: string
  lastLoginTime?: string
  createTime?: string
  wechatOpenid?: string
  wechatUnionid?: string
  wechatNickname?: string
  wechatAvatar?: string
  wechatBindStatus?: number
  wechatBindTime?: string
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

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

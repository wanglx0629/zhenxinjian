/**
 * 认证相关 API
 * 作者: wanglx
 */
import http, { BASE_URL } from './request'
import type { GuestLoginResult, LoginResult, Result, UserInfo, WechatProfilePayload } from './types'

/** 用户登出 */
export function logout() {
  return http.post<void>('/auth/logout')
}

/** 获取当前用户 */
export function getCurrentUser() {
  return http.get<UserInfo>('/auth/me')
}

/**
 * 微信授权登录（multipart/form-data，一次请求携带 code + nickname + 头像文件）
 * <p>选择 uni.uploadFile 而非 uni.request：multipart 文件上传在小程序端仅 uploadFile 支持；
 * 响应解包逻辑与 request.ts 保持一致（Result.code === 200 取 data，否则 toast 并 reject）</p>
 *
 * @param params.code      wx.login() 临时凭证
 * @param params.nickname  input type="nickname" 采集的昵称
 * @param params.avatarPath button open-type="chooseAvatar" 返回的本地临时路径
 * @param params.guestKey  可选，游客标识（触发数据迁移）
 */
export function wechatLogin(params: WechatProfilePayload & { code: string; guestKey?: string }) {
  return new Promise<LoginResult>((resolve, reject) => {
    const formData: Record<string, string> = {
      code: params.code,
      nickname: params.nickname
    }
    if (params.guestKey) {
      formData.guestKey = params.guestKey
    }
    uni.uploadFile({
      url: `${BASE_URL}/auth/wechat/login`,
      filePath: params.avatarPath,
      name: 'avatar',
      formData,
      timeout: 15000,
      success: (res) => {
        if (res.statusCode !== 200) {
          uni.showToast({ title: '登录失败，请重试', icon: 'none' })
          reject(new Error(`http ${res.statusCode}`))
          return
        }
        let body: Result<LoginResult> | null = null
        try {
          body = JSON.parse(res.data) as Result<LoginResult>
        } catch {
          body = null
        }
        if (!body || typeof body !== 'object') {
          uni.showToast({ title: '响应异常', icon: 'none' })
          reject(new Error('invalid response'))
          return
        }
        if (body.code !== 200) {
          uni.showToast({ title: body.message || '登录失败', icon: 'none' })
          reject(new Error(body.message || 'login failed'))
          return
        }
        resolve(body.data)
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

/** 游客身份签发/复用（带既有有效 guestKey 复用记录，不重置 3 天起算） */
export function createGuest(guestKey?: string) {
  return http.post<GuestLoginResult>('/auth/guest', guestKey ? { guestKey } : {})
}

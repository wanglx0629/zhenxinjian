/**
 * 本地存储封装（zxj_ 前缀规范，D3：游客身份仅存本机）
 * 作者: wanglx
 */

const TOKEN_KEY = 'zxj_token'
const GUEST_KEY_KEY = 'zxj_guest_key'

/** 读取 Token */
export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

/** 写入 Token */
export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

/** 清除 Token */
export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

/** 读取游客标识 */
export function getGuestKey(): string {
  return uni.getStorageSync(GUEST_KEY_KEY) || ''
}

/** 写入游客标识 */
export function setGuestKey(guestKey: string) {
  uni.setStorageSync(GUEST_KEY_KEY, guestKey)
}

/** 清除游客标识 */
export function removeGuestKey() {
  uni.removeStorageSync(GUEST_KEY_KEY)
}

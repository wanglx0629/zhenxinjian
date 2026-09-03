/**
 * 本地存储封装
 * 作者: luote (luote) - https://luote996.cn
 */

const TOKEN_KEY = 'token'

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

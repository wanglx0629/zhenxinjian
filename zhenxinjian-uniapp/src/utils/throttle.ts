/**
 * 提交节流：防止连续点击
 * 作者: luote (luote) - https://luote996.cn
 */

let lastSubmitAt = 0

/**
 * 是否允许提交（默认间隔 2 秒）
 */
export function canSubmit(intervalMs = 2000): boolean {
  const now = Date.now()
  if (now - lastSubmitAt < intervalMs) {
    return false
  }
  lastSubmitAt = now
  return true
}

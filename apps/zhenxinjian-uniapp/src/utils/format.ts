/**
 * 格式化工具
 * 作者: wanglx
 */
import { GREETING_DEFAULT, GREETING_SLOTS } from '@/config/constants'

const pad = (n: number): string => (n < 10 ? '0' + n : String(n))

/** Date → YYYY-MM-DD */
export function ymd(d?: Date | string | number | null): string {
  const x = d ? new Date(d) : new Date()
  return `${x.getFullYear()}-${pad(x.getMonth() + 1)}-${pad(x.getDate())}`
}

/** Date → M月D日 */
export function md(d?: Date | string | number | null): string {
  const x = d ? new Date(d) : new Date()
  return `${x.getMonth() + 1}月${x.getDate()}日`
}

/** 数字保留一位小数并去尾零 */
export function g(n: number | string): string {
  const v = Math.round(Number(n) * 10) / 10
  return Number.isInteger(v) ? String(v) : v.toFixed(1)
}

/** 星期 */
const WK = ['日', '一', '二', '三', '四', '五', '六']
export function week(d?: Date | string | number | null): string {
  const x = d ? new Date(d) : new Date()
  return '周' + WK[x.getDay()]
}

/** Date → M月D日 周X */
export function mdWeek(d?: Date | string | number | null): string {
  return md(d) + ' ' + week(d)
}

/** 分时段问候语（05–11 早上好 / 11–14 中午好 / 14–18 下午好 / 18–23 晚上好 / 23–05 夜深了） */
export function greeting(d?: Date | string | number | null): string {
  const x = d ? new Date(d) : new Date()
  const h = x.getHours()
  const slot = GREETING_SLOTS.find(s => h >= s.from && h < s.to)
  return slot ? slot.text : GREETING_DEFAULT
}

/** 按当前时段取默认餐别（05–10 早 / 10–15 午 / 15–20:30 晚 / 其余加餐）；code 与 MEAL_TYPES 对齐 */
export function defaultMealType(d?: Date | string | number | null): number {
  const x = d ? new Date(d) : new Date()
  const h = x.getHours() + x.getMinutes() / 60
  if (h >= 5 && h < 10) return 1
  if (h >= 10 && h < 15) return 2
  if (h >= 15 && h < 20.5) return 3
  return 4
}

/**
 * 游客体验剩余时长文案（首页与我的页统一口径）
 * ≥1 天 →「X 天」/「X 天 X 时」；不足 1 天 →「X 时」；≤0 →「已到期」
 */
export function guestLeftText(expireAt?: string | null): string {
  if (!expireAt) return ''
  const diffMs = new Date(expireAt).getTime() - Date.now()
  if (diffMs <= 0) return '已到期'
  const totalHours = Math.ceil(diffMs / 3600000)
  if (totalHours < 24) return `${totalHours} 时`
  const days = Math.floor(totalHours / 24)
  const hours = totalHours % 24
  return hours > 0 ? `${days} 天 ${hours} 时` : `${days} 天`
}

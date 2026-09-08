/**
 * 格式化工具
 * 作者: luote (luote) - https://luote996.cn
 */
import { round } from './calculator'

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

export { round }

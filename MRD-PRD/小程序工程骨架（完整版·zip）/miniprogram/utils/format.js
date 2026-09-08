// utils/format.js —— 格式化工具
const { round } = require('./calculator.js')

const pad = n => (n < 10 ? '0' + n : '' + n)

/** Date → YYYY-MM-DD */
function ymd(d) {
  const x = d ? new Date(d) : new Date()
  return `${x.getFullYear()}-${pad(x.getMonth() + 1)}-${pad(x.getDate())}`
}

/** Date → M月D日 */
function md(d) {
  const x = d ? new Date(d) : new Date()
  return `${x.getMonth() + 1}月${x.getDate()}日`
}

/** 数字保留一位小数并去尾零 */
function g(n) {
  const v = Math.round(Number(n) * 10) / 10
  return Number.isInteger(v) ? String(v) : v.toFixed(1)
}

/** 星期 */
const WK = ['日', '一', '二', '三', '四', '五', '六']
function week(d) {
  const x = d ? new Date(d) : new Date()
  return '周' + WK[x.getDay()]
}

module.exports = { ymd, md, g, week, round }

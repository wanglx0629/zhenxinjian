// utils/validate.js —— 录入区间校验（PRD §2.2 / §8.1）
const K = require('../config/constants.js')

/**
 * @param {*} value  待校验值
 * @param {string} key  RANGES 中的键，如 'age'
 * @returns {{ok: boolean, msg: string, value: number}}
 */
function checkRange(value, key) {
  const cfg = K.RANGES[key]
  if (!cfg) return { ok: true, msg: '', value: Number(value) }
  const n = Number(value)
  if (value === '' || value === null || value === undefined || isNaN(n)) {
    return { ok: false, msg: `请输入${cfg.label || key}`, value: null }
  }
  if (n < cfg.min || n > cfg.max) {
    return { ok: false, msg: `请输入 ${cfg.min} – ${cfg.max} ${cfg.unit} 之间的数值`, value: null }
  }
  return { ok: true, msg: '', value: n }
}

/** 批量校验，返回 { ok, errors } */
function checkAll(values) {
  const errors = {}
  let ok = true
  Object.keys(values).forEach(k => {
    const r = checkRange(values[k], k)
    if (!r.ok) { errors[k] = r.msg; ok = false }
  })
  return { ok, errors }
}

/** 目标体重不得高于当前体重（PRD §2.2：减脂场景） */
function checkTargetWeight(weight, targetWeight) {
  if (Number(targetWeight) > Number(weight) + 0.1) {
    return { ok: false, msg: '减脂场景下目标体重需低于当前体重' }
  }
  return { ok: true, msg: '' }
}

module.exports = { checkRange, checkAll, checkTargetWeight }

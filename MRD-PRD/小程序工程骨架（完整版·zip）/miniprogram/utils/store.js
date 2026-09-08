// utils/store.js —— 本地存储封装（游客数据留存 / 登录后继承，PRD §2.1）
// 生产环境建议替换为微信云开发数据库，接口保持一致即可平滑迁移。

const PREFIX = 'zxj_'

function set(key, value) {
  try {
    wx.setStorageSync(PREFIX + key, value)
    return true
  } catch (e) {
    console.error('[store.set]', key, e)
    return false
  }
}

function get(key, def) {
  try {
    const v = wx.getStorageSync(PREFIX + key)
    return (v === '' || v === null || v === undefined) ? def : v
  } catch (e) {
    console.error('[store.get]', key, e)
    return def
  }
}

function remove(key) {
  try { wx.removeStorageSync(PREFIX + key); return true } catch (e) { return false }
}

/** 按日期存取当日记录，便于跨天切换 */
function getRecordsByDate(dateStr) {
  return get(`records_${dateStr}`, [])
}

function setRecordsByDate(dateStr, records) {
  return set(`records_${dateStr}`, records)
}

/** 游客数据合并到已登录账号（PRD §2.1：不清除，自动继承） */
function mergeGuestData(openid) {
  const profile = get('profile', null)
  const weights = get('weights', [])
  if (profile) set(`profile_${openid}`, profile)
  if (weights.length) set(`weights_${openid}`, weights)
  return { profile, weights }
}

module.exports = { set, get, remove, getRecordsByDate, setRecordsByDate, mergeGuestData }

// pages/expire/expire.js —— 周期到期提示（F20 碳池耗尽 / 周期结束引导）
const store = require('../../utils/store.js')

Page({
  data: { cycleDays: 7 },

  onLoad() {
    const p = store.get('profile', null)
    this.setData({ cycleDays: p ? (p.cycleDays || 7) : 7 })
  },

  onRestart() {
    const p = store.get('profile')
    p.train = []
    p.cycleStart = new Date().toISOString().slice(0, 10)
    store.set('profile', p)
    getApp().saveProfile(p)
    wx.navigateTo({ url: '/pages/cycleSet/cycleSet' })
  },

  onBackHome() { wx.switchTab({ url: '/pages/home/home' }) }
})

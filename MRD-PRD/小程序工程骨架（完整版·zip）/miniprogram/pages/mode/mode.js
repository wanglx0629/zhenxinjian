// pages/mode/mode.js —— P05 模式选择（F10 532 / F11 碳循环）
const store = require('../../utils/store.js')

Page({
  data: { mode: '532' },

  onShow() {
    const p = store.get('profile', null)
    if (p) this.setData({ mode: p.mode })
  },

  onPick(e) {
    const mode = e.currentTarget.dataset.v
    const p = store.get('profile')
    if (p && p.mode && p.mode !== mode) {
      // 已处于某模式中途切换 → 走 P16 确认页（F15 周期终止与碳池清空）
      wx.navigateTo({ url: `/pages/switch/switch?to=${mode}` })
      return
    }
    this.setMode(mode)
  },

  setMode(mode) {
    const p = store.get('profile')
    p.mode = mode
    store.set('profile', p)
    getApp().saveProfile(p)
    if (mode === 'cycle') {
      wx.navigateTo({ url: '/pages/cycleSet/cycleSet' })
    } else {
      wx.navigateTo({ url: '/pages/plan532/plan532' })
    }
  }
})

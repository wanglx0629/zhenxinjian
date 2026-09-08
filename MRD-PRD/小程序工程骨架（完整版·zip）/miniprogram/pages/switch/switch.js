// pages/switch/switch.js —— P16 切换模式（F15 终止周期与碳池清空确认）
const store = require('../../utils/store.js')
const K = require('../../config/constants.js')

Page({
  data: { to: '532', toLabel: '', curLabel: '', cycleDays: 7, pool: 0 },

  onLoad(q) {
    const to = q.to || '532'
    const p = store.get('profile', null)
    const cur = p ? p.mode : '532'
    const label = m => (m === 'cycle' ? '碳循环模式' : '532 碳水渐降')
    this.setData({
      to, toLabel: label(to), curLabel: label(cur),
      cycleDays: p ? (p.cycleDays || 7) : 7,
      pool: p && p.targetWeight ? Math.round(p.targetWeight * K.CYCLE.carbCoef * (p.cycleDays || 7)) : 0
    })
  },

  onConfirm() {
    const p = store.get('profile')
    p.mode = this.data.to
    // F15：切换模式时终止当前周期，清空碳水池与运动日标记
    p.train = []
    p.cycleStart = ''
    store.set('profile', p)
    getApp().saveProfile(p)
    wx.showToast({ title: '已切换', icon: 'success' })
    setTimeout(() => {
      if (p.mode === 'cycle') wx.navigateTo({ url: '/pages/cycleSet/cycleSet' })
      else wx.navigateTo({ url: '/pages/plan532/plan532' })
    }, 600)
  },

  onCancel() { wx.navigateBack() }
})

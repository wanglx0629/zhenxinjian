// pages/cycleSet/cycleSet.js —— P06 碳循环设置（F12 周期天数 / F13 运动日 / F19 脂肪系数）
const store = require('../../utils/store.js')
const K = require('../../config/constants.js')

Page({
  data: {
    cycleDays: 7,
    targetWeight: '',
    coefIndex: 0,
    coefs: K.CYCLE.fatCoefOptions,
    train: [],
    days: []
  },

  onShow() {
    const p = store.get('profile', null)
    if (!p) { wx.navigateBack(); return }
    const train = p.train || []
    this.buildDays(p.cycleDays || 7, train)
    this.setData({
      cycleDays: p.cycleDays || 7,
      targetWeight: p.targetWeight || '',
      coefIndex: K.CYCLE.fatCoefOptions.indexOf(p.carbFatCoef || 0.8),
      train
    })
  },

  buildDays(n, train) {
    const days = []
    for (let i = 0; i < n; i++) {
      days.push({ i, label: `第 ${i + 1} 天`, isTrain: !!(train && train[i]) })
    }
    this.setData({ days })
  },

  onDays(e) {
    const n = Number(e.currentTarget.dataset.v)
    const p = store.get('profile')
    p.cycleDays = n
    p.train = (p.train || []).slice(0, n)
    store.set('profile', p)
    getApp().saveProfile(p)
    this.buildDays(n, p.train)
    this.setData({ cycleDays: n, train: p.train })
  },

  onTargetWeight(e) {
    const p = store.get('profile')
    p.targetWeight = Number(e.detail.value)
    store.set('profile', p)
    this.setData({ targetWeight: e.detail.value })
  },

  onCoef(e) {
    const i = Number(e.currentTarget.dataset.i)
    const p = store.get('profile')
    p.carbFatCoef = K.CYCLE.fatCoefOptions[i]
    store.set('profile', p)
    getApp().saveProfile(p)
    this.setData({ coefIndex: i })
  },

  onToggleTrain(e) {
    const i = Number(e.currentTarget.dataset.i)
    const p = store.get('profile')
    p.train = p.train || []
    p.train[i] = !p.train[i]
    store.set('profile', p)
    getApp().saveProfile(p)
    this.buildDays(p.cycleDays, p.train)
    this.setData({ train: p.train })
  },

  onNext() {
    wx.navigateTo({ url: '/pages/cyclePlan/cyclePlan' })
  }
})

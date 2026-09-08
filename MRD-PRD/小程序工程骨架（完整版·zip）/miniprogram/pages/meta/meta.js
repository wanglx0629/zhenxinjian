// pages/meta/meta.js —— P04 代谢结果展示（F08 BMR/TDEE / F09 缺口选择）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')
const K = require('../../config/constants.js')

Page({
  data: {
    bmr: 0, tdee: 0, target: 0,
    deficits: K.DEFICIT_OPTIONS,
    deficitIndex: 1,
    weeklyLoss: 0
  },

  onShow() {
    const p = store.get('profile', null)
    if (!p) { wx.navigateBack(); return }
    const idx = K.DEFICIT_OPTIONS.indexOf(p.deficit)
    this.setData({ deficitIndex: idx >= 0 ? idx : 1 })
    this.recalc()
  },

  recalc() {
    const p = store.get('profile')
    const bmr = calc.bmr(p)
    const tdee = calc.tdee(p)
    const target = calc.targetKcal(p)
    // 7700 kcal ≈ 1 kg 脂肪
    const weeklyLoss = (p.deficit * 7 / 7700)
    this.setData({
      bmr: calc.round(bmr), tdee: calc.round(tdee), target: calc.round(target),
      weeklyLoss: weeklyLoss.toFixed(2)
    })
  },

  onDeficit(e) {
    const p = store.get('profile')
    p.deficit = K.DEFICIT_OPTIONS[Number(e.currentTarget.dataset.i)]
    store.set('profile', p)
    getApp().saveProfile(p)
    this.setData({ deficitIndex: Number(e.currentTarget.dataset.i) })
    this.recalc()
  },

  onNext() {
    wx.navigateTo({ url: '/pages/mode/mode' })
  }
})

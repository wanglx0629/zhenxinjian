// pages/weight/weight.js —— P14 体重记录与趋势（F28 体重趋势 / 平台期判定）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')
const { ymd, md } = require('../../utils/format.js')

Page({
  data: {
    weight: '', list: [], latest: null, delta: null, plateau: false, trend: ''
  },

  onShow() { this.load() },

  load() {
    const list = store.get('weights', [])
    const p = store.get('profile', null)
    const latest = list.length ? list[list.length - 1] : null
    let delta = null
    if (list.length >= 2) {
      delta = (list[list.length - 1].weight - list[0].weight).toFixed(1)
    }
    this.setData({
      list: list.slice().reverse(),
      latest, delta,
      plateau: calc.isPlateau(list),
      weight: p ? String(p.weight) : ''
    })
  },

  onInput(e) { this.setData({ weight: e.detail.value }) },

  onSave() {
    const w = Number(this.data.weight)
    if (!w || w < 25 || w > 200) {
      wx.showToast({ title: '请输入 25 – 200 kg', icon: 'none' }); return
    }
    const list = store.get('weights', [])
    const dateStr = ymd(new Date())
    const idx = list.findIndex(x => x.date === dateStr)
    if (idx >= 0) list[idx].weight = w
    else list.push({ date: dateStr, weight: w })

    store.set('weights', list)

    // 同步更新档案当前体重（F06）
    const p = store.get('profile')
    if (p) { p.weight = w; store.set('profile', p); getApp().saveProfile(p) }

    wx.showToast({ title: '已记录', icon: 'success' })
    this.load()
  },

  onPlateauAction() {
    wx.showModal({
      title: '检测到体重平台期',
      content: '近 7 天体重波动小于 0.3kg。是否进入「阶段 4 · 平台突破期」？碳水 −20g、总热量 −80 kcal。',
      success: (res) => {
        if (res.confirm) wx.navigateTo({ url: '/pages/plan532/plan532' })
      }
    })
  }
})

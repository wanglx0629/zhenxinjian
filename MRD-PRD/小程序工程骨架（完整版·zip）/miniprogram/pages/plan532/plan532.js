// pages/plan532/plan532.js —— P08 532 计划（F16 四阶段 / F17 平台微调 / F18 经期录入 / F29 四阶段饮食匹配）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')
const { ymd } = require('../../utils/format.js')

Page({
  data: {
    stages: [], phase: null, isFemale: false,
    period: false, periodStart: '', cycleLen: 28, periodDays: 5,
    todayStr: ''
  },

  onShow() {
    const p = store.get('profile', null)
    if (!p) { wx.navigateBack(); return }
    const ph = calc.menstrualPhase(p)
    this.setData({
      stages: calc.plan532Stages(p),
      phase: ph,
      isFemale: p.gender === 'female',
      period: !!p.period,
      periodStart: p.periodStart || ymd(new Date()),
      cycleLen: p.cycleLen || 28,
      periodDays: p.periodDays || 5,
      todayStr: ymd(new Date())
    })
  },

  // F18 经期周期管理开关（仅女性展示）
  onTogglePeriod() {
    const p = store.get('profile')
    p.period = !p.period
    if (p.period && !p.periodStart) p.periodStart = ymd(new Date())
    store.set('profile', p)
    getApp().saveProfile(p)
    this.refresh()
  },

  onDateChange(e) {
    const p = store.get('profile')
    p.periodStart = e.detail.value
    store.set('profile', p); this.refresh()
  },

  onNumChange(e) {
    const k = e.currentTarget.dataset.k
    const p = store.get('profile')
    p[k] = Number(e.detail.value)
    store.set('profile', p); this.refresh()
  },

  refresh() {
    const p = store.get('profile')
    getApp().saveProfile(p)
    this.setData({
      stages: calc.plan532Stages(p),
      phase: calc.menstrualPhase(p),
      period: !!p.period,
      periodStart: p.periodStart,
      cycleLen: p.cycleLen,
      periodDays: p.periodDays
    })
  }
})

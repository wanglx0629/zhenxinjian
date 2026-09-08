// pages/home/home.js —— P02 首页今日总览（F03 今日目标 / F04 进度 / F26 超标预警 / F29 经期标签）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')
const { ymd, md, week } = require('../../utils/format.js')

Page({
  data: {
    dateLabel: '', modeLabel: '', phase: null,
    target: { carb: 0, protein: 0, fat: 0, kcal: 0 },
    actual: { carb: 0, protein: 0, fat: 0, kcal: 0 },
    pct: { carb: 0, protein: 0, fat: 0, kcal: 0 },
    over: null,
    hasProfile: false,
    recordCount: 0
  },

  onShow() {
    const p = store.get('profile', null)
    if (!p) { this.setData({ hasProfile: false }); return }

    const now = new Date()
    const dateStr = ymd(now)

    // 今日目标：按模式计算
    let target
    if (p.mode === 'cycle') {
      const a = calc.cycleAlloc(p, { now })
      // 取周期内第 (今天 - 周期起始) 天；简易实现取第 1 天，生产环境按周期起始日推算
      const d = a.days[0]
      target = { carb: d.carb, protein: d.protein, fat: d.fat, kcal: d.kcal }
    } else {
      target = calc.macro532(p, now)
    }

    // 今日实际
    const records = store.getRecordsByDate(dateStr)
    const actual = calc.sumRecords(records)
    const over = calc.overCheck(target, actual)

    const pct = k => target[k] > 0 ? Math.min(100, Math.round(actual[k] / target[k] * 100)) : 0

    this.setData({
      hasProfile: true,
      dateLabel: `${md(now)} ${week(now)}`,
      modeLabel: p.mode === 'cycle' ? '碳循环模式' : '532 碳水渐降',
      phase: calc.menstrualPhase(p, now),
      target: {
        carb: calc.round(target.carb), protein: calc.round(target.protein),
        fat: calc.round(target.fat), kcal: calc.round(target.kcal)
      },
      actual: {
        carb: calc.round(actual.carb), protein: calc.round(actual.protein),
        fat: calc.round(actual.fat), kcal: calc.round(actual.kcal)
      },
      pct: { carb: pct('carb'), protein: pct('protein'), fat: pct('fat'), kcal: pct('kcal') },
      over,
      recordCount: records.length
    })
  },

  goPlan() {
    const p = store.get('profile')
    wx.navigateTo({ url: p.mode === 'cycle' ? '/pages/cyclePlan/cyclePlan' : '/pages/plan532/plan532' })
  },
  goAdd() { wx.navigateTo({ url: '/pages/add/add' }) },
  goBody() { wx.navigateTo({ url: '/pages/body/body' }) }
})

// pages/cyclePlan/cyclePlan.js —— P07 碳循环计划（F14 蛋白脂肪锁定 / F20 池消耗 / F21 日型轮换）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')

Page({
  data: {
    totalDays: 7, pool: 0, poolFat: 0, used: 0, remain: 0,
    protein: 0, days: [], types: [], phase: null
  },

  onShow() {
    const p = store.get('profile', null)
    if (!p) { wx.navigateBack(); return }
    const a = calc.cycleAlloc(p)
    const used = a.days.slice(0, 2).reduce((s, d) => s + d.carb, 0)

    this.setData({
      totalDays: p.cycleDays,
      pool: calc.round(a.cc.pool),
      poolFat: calc.round(a.cc.poolFat),
      protein: calc.round(a.protein),
      used: calc.round(used),
      remain: calc.round(a.cc.pool - used),
      days: a.days,
      types: ['high', 'mid', 'low'].map(t => {
        const per = t === 'high' ? a.perH : t === 'mid' ? a.perM : a.perL
        const color = t === 'high' ? '#F59E0B' : t === 'mid' ? '#0D9488' : '#6366F1'
        const label = t === 'high' ? '高碳日' : t === 'mid' ? '中碳日' : '低碳日'
        const kcal = calc.round(per.carb * 4 + a.cc.protein * 4 + per.fat * 9)
        return { t, label, color, carb: calc.round(per.carb), fat: calc.round(per.fat), kcal }
      }),
      phase: a.phase
    })
  },

  onGoHome() { wx.switchTab({ url: '/pages/home/home' }) }
})

// pages/diary/diary.js —— P12 当日饮食记录（F25 增删改）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')
const { ymd, md } = require('../../utils/format.js')

Page({
  data: {
    dateStr: '', dateLabel: '', records: [], grouped: [],
    total: { carb: 0, protein: 0, fat: 0, kcal: 0 }
  },

  onShow() { this.load() },

  load() {
    const now = new Date()
    const dateStr = ymd(now)
    const records = store.getRecordsByDate(dateStr)
    const meals = ['早餐', '午餐', '晚餐', '加餐']
    const grouped = meals.map(m => ({
      meal: m, items: records.filter(r => r.meal === m)
    })).filter(g => g.items.length)

    this.setData({
      dateStr, dateLabel: md(now), records, grouped,
      total: {
        carb: calc.round(calc.sumRecords(records).carb),
        protein: calc.round(calc.sumRecords(records).protein),
        fat: calc.round(calc.sumRecords(records).fat),
        kcal: calc.round(calc.sumRecords(records).kcal)
      }
    })
  },

  onAdd() { wx.navigateTo({ url: '/pages/food/food?from=add' }) },

  onDelete(e) {
    const id = Number(e.currentTarget.dataset.id)
    wx.showModal({
      title: '删除这条记录？', content: '删除后不可恢复',
      success: (res) => {
        if (!res.confirm) return
        const records = store.getRecordsByDate(this.data.dateStr).filter(r => r.id !== id)
        store.setRecordsByDate(this.data.dateStr, records)
        wx.showToast({ title: '已删除', icon: 'success' })
        this.load()
      }
    })
  },

  onEdit(e) {
    const id = Number(e.currentTarget.dataset.id)
    const r = this.data.records.filter(x => x.id === id)[0]
    if (r) wx.navigateTo({ url: `/pages/add/add?id=${r.foodId}&qty=${r.grams}` })
  }
})

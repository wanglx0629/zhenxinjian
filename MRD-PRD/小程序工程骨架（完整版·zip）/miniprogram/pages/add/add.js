// pages/add/add.js —— P11 添加饮食记录（F24 重量→单餐宏量换算）
const store = require('../../utils/store.js')
const calc = require('../../utils/calculator.js')
const { getFoodById } = require('../../data/foods.js')
const { ymd } = require('../../utils/format.js')

Page({
  data: {
    food: null, qty: 100,
    meals: ['早餐', '午餐', '晚餐', '加餐'], mealIndex: 1,
    macro: { carb: 0, protein: 0, fat: 0, kcal: 0 },
    isDry: false
  },

  onLoad(q) {
    const food = getFoodById(q.id) || getFoodById('F001')
    this.setData({ food, isDry: /（干）|干$/.test(food.name) })
    this.calc()
  },

  onQty(e) { this.setData({ qty: Number(e.detail.value) || 0 }), this.calc() },
  onQtyStep(e) {
    const d = Number(e.currentTarget.dataset.d)
    this.setData({ qty: Math.max(0, this.data.qty + d) })
    this.calc()
  },
  onMeal(e) { this.setData({ mealIndex: Number(e.detail.value) }) },

  calc() {
    const m = calc.foodMacro(this.data.food, this.data.qty)
    this.setData({
      macro: { carb: calc.round(m.carb), protein: calc.round(m.protein), fat: calc.round(m.fat), kcal: calc.round(m.kcal) }
    })
  },

  onSave() {
    const d = this.data
    const dateStr = ymd(new Date())
    const records = store.getRecordsByDate(dateStr)
    records.push({
      id: Date.now(),
      meal: d.meals[d.mealIndex],
      foodId: d.food.id, name: d.food.name,
      grams: d.qty,
      carb: d.macro.carb, protein: d.macro.protein, fat: d.macro.fat, kcal: d.macro.kcal,
      time: new Date().toTimeString().slice(0, 5)
    })
    store.setRecordsByDate(dateStr, records)
    wx.showToast({ title: '已记录', icon: 'success' })
    setTimeout(() => wx.switchTab({ url: '/pages/diary/diary' }), 600)
  }
})

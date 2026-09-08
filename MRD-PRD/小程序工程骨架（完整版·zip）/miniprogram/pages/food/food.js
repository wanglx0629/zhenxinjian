// pages/food/food.js —— P10 食物库（F22 分类浏览 / F23 搜索筛选）
const { FOODS, CATEGORIES, searchFoods } = require('../../data/foods.js')

Page({
  data: {
    cats: CATEGORIES, curCat: '全部', kw: '', list: FOODS.slice(0, 50), total: FOODS.length
  },

  onCat(e) { this.setData({ curCat: e.currentTarget.dataset.v }), this.doSearch() },
  onKw(e) { this.setData({ kw: e.detail.value }), this.doSearch() },
  clearKw() { this.setData({ kw: '' }), this.doSearch() },

  doSearch() {
    const r = searchFoods(this.data.kw, this.data.curCat)
    this.setData({ list: r.slice(0, 100), total: r.length })
  },

  pick(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/add/add?id=${id}` })
  }
})

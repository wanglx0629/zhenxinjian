// pages/mine/mine.js —— P15 我的（入口汇总）
const store = require('../../utils/store.js')

Page({
  data: { profile: null, isGuest: true, modeLabel: '' },

  onShow() {
    const p = store.get('profile', null)
    this.setData({
      profile: p,
      isGuest: !store.get('openid', ''),
      modeLabel: p ? (p.mode === 'cycle' ? '碳循环模式' : '532 碳水渐降') : '未设置'
    })
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (url) wx.navigateTo({ url })
  },

  onClear() {
    wx.showModal({
      title: '确认清空本地数据？', content: '将删除本机全部记录且不可恢复',
      success: (res) => {
        if (!res.confirm) return
        const keys = wx.getStorageInfoSync().keys || []
        keys.filter(k => k.indexOf('zxj_') === 0).forEach(k => wx.removeStorageSync(k))
        wx.showToast({ title: '已清空', icon: 'success' })
        wx.reLaunch({ url: '/pages/guest/guest' })
      }
    })
  }
})

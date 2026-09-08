// pages/guest/guest.js —— P01 登录 / 游客入口（F01 微信授权登录 / F02 游客模式）
const store = require('../../utils/store.js')

Page({
  data: { hasProfile: false },

  onShow() {
    // 已有档案 → 直接进入首页，不再强登（PRD §2.1：降低首屏流失）
    const profile = store.get('profile', null)
    if (profile) {
      wx.switchTab({ url: '/pages/home/home' })
    }
    this.setData({ hasProfile: !!profile })
  },

  // 微信一键登录
  onLogin() {
    wx.getUserProfile({
      desc: '用于同步你的减脂数据',
      success: (res) => {
        // TODO(开发)：调用云函数 login 换取 openid，再拉取云端档案
        // wx.cloud.callFunction({ name: 'login' }).then(r => {
        //   store.set('openid', r.result.openid)
        //   store.mergeGuestData(r.result.openid)   // 游客数据继承，不清除
        // })
        store.set('userInfo', res.userInfo)
        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => wx.switchTab({ url: '/pages/home/home' }), 600)
      },
      fail: () => {
        wx.showToast({ title: '已取消，可先以游客身份体验', icon: 'none' })
      }
    })
  },

  // 游客模式：完整功能可用，数据仅存本地（PRD F02）
  onGuest() {
    store.set('isGuest', true)
    wx.navigateTo({ url: '/pages/body/body?from=guest' })
  },

  // 已有数据？本地恢复
  onRestore() {
    wx.navigateTo({ url: '/pages/body/body?from=restore' })
  }
})

// app.js —— 臻心减 V1.1 全局入口
const store = require('./utils/store.js')

App({
  globalData: {
    // 用户档案（P03 身体数据页录入，落盘至本地缓存）
    profile: null,
    // 今日饮食记录
    records: [],
    // 是否游客模式（未登录）
    isGuest: true
  },

  onLaunch() {
    // 1) 读取本地档案；无则视为游客
    const profile = store.get('profile', null)
    this.globalData.profile = profile
    this.globalData.isGuest = !store.get('openid', '')

    // 2) 读取今日记录
    this.globalData.records = store.get('records', [])

    // 3) 游客数据本地留存，登录后合并（PRD §2.1：游客数据不清除，登录后自动继承）
    console.log('[臻心减] 启动完成，模式：', this.globalData.isGuest ? '游客' : '已登录')
  },

  onShow() {
    // 每次回前台刷新「今日」——经期阶段 / 碳循环日序均依赖当天日期
    const today = new Date()
    const todayStr = this.formatDate(today)
    if (store.get('lastActiveDate', '') !== todayStr) {
      store.set('lastActiveDate', todayStr)
      // 跨天：清空当日记录缓存，重新从云端/本地按日期拉取
      this.globalData.records = store.get(`records_${todayStr}`, [])
    }
  },

  formatDate(d) {
    const p = n => (n < 10 ? '0' + n : '' + n)
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
  },

  // 供各页面调用：保存档案并广播
  saveProfile(profile) {
    this.globalData.profile = profile
    store.set('profile', profile)
  }
})

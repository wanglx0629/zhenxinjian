// pages/remind/remind.js —— P13 提醒设置（F27 三餐提醒开关与时间）
const store = require('../../utils/store.js')

Page({
  data: {
    remind: { breakfast: true, lunch: true, dinner: true, times: { breakfast: '07:30', lunch: '12:00', dinner: '18:30' } },
    keys: [
      { k: 'breakfast', label: '早餐提醒' },
      { k: 'lunch', label: '午餐提醒' },
      { k: 'dinner', label: '晚餐提醒' }
    ]
  },

  onShow() {
    const p = store.get('profile', null)
    if (p && p.remind) this.setData({ remind: p.remind })
  },

  onToggle(e) {
    const k = e.currentTarget.dataset.k
    const remind = this.data.remind
    remind[k] = !remind[k]
    this.save(remind)
  },

  onTime(e) {
    const k = e.currentTarget.dataset.k
    const remind = this.data.remind
    remind.times[k] = e.detail.value
    this.save(remind)
  },

  save(remind) {
    const p = store.get('profile')
    p.remind = remind
    store.set('profile', p)
    getApp().saveProfile(p)
    this.setData({ remind })
    // TODO(开发)：调用订阅消息 wx.requestSubscribeMessage，并按时间写入定时触发
  }
})

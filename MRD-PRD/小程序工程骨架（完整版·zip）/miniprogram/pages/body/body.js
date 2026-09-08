// pages/body/body.js —— P03 身体数据录入（F05 性别 / F06 年龄身高体重 / F07 活动系数）
const store = require('../../utils/store.js')
const { checkRange, checkTargetWeight } = require('../../utils/validate.js')
const K = require('../../config/constants.js')

Page({
  data: {
    gender: 'female',
    age: '', height: '', weight: '', targetWeight: '',
    actIndex: 1,
    acts: K.ACTIVITY,
    errors: {},
    from: ''
  },

  onLoad(q) {
    this.setData({ from: q.from || '' })
    const p = store.get('profile', null)
    if (p) {
      const idx = K.ACTIVITY.findIndex(a => a.value === p.act)
      this.setData({
        gender: p.gender || 'female',
        age: p.age || '', height: p.height || '',
        weight: p.weight || '', targetWeight: p.targetWeight || '',
        actIndex: idx >= 0 ? idx : 1
      })
    }
  },

  onGender(e) { this.setData({ gender: e.currentTarget.dataset.v }) },
  onInput(e) {
    const k = e.currentTarget.dataset.k
    this.setData({ [k]: e.detail.value })
  },
  onAct(e) { this.setData({ actIndex: Number(e.detail.value) }) },

  onSubmit() {
    const d = this.data
    const errors = {}
    let ok = true
    ;['age', 'height', 'weight', 'targetWeight'].forEach(k => {
      const r = checkRange(d[k], k)
      if (!r.ok) { errors[k] = r.msg; ok = false }
    })
    const tw = checkTargetWeight(d.weight, d.targetWeight)
    if (!tw.ok) { errors.targetWeight = tw.msg; ok = false }
    this.setData({ errors })
    if (!ok) return

    const profile = {
      gender: d.gender,
      age: Number(d.age), height: Number(d.height),
      weight: Number(d.weight), targetWeight: Number(d.targetWeight),
      act: K.ACTIVITY[d.actIndex].value,
      actLabel: K.ACTIVITY[d.actIndex].label,
      // 模式与周期默认项（P05 / P06 可改）
      mode: '532', cycleDays: 7, train: [],
      deficit: 300, carbFatCoef: K.CYCLE.fatCoefDefault,
      // 经期周期管理（女性专属，F18/F29）
      period: false, periodStart: '', cycleLen: 28, periodDays: 5,
      remind: { breakfast: true, lunch: true, dinner: true, times: { breakfast: '07:30', lunch: '12:00', dinner: '18:30' } }
    }
    store.set('profile', profile)
    getApp().saveProfile(profile)

    wx.navigateTo({ url: '/pages/meta/meta' })
  }
})

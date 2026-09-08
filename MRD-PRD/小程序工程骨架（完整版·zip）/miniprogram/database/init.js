// database/init.js —— 微信云开发数据库集合与索引初始化脚本
// 用法：在云开发控制台「数据库 → 导入」或在云函数中执行一次 initCollections()
// 集合权限建议：全部设为「仅创建者可读写」，服务端云函数使用 admin 权限绕过。

const COLLECTIONS = {
  // 用户档案（含经期周期管理字段）
  profiles: {
    doc: {
      _openid: '',
      gender: 'female',            // male / female
      age: 30, height: 162, weight: 55, targetWeight: 53,
      act: 1.375,                  // 活动系数
      mode: '532',                 // 532 / cycle
      deficit: 300,                // 用户自选热量缺口 X
      cycleDays: 7,                // 碳循环周期长度
      carbFatCoef: 0.8,            // 碳循环脂肪系数 0.8 / 1.0
      train: [],                   // 运动日布尔数组，长度 = cycleDays
      // 经期周期管理（女性专属 F18 / F29）
      period: false,
      periodStart: '',             // YYYY-MM-DD
      cycleLen: 28,                // 21–35
      periodDays: 5,               // 3–10
      // 提醒设置 F27
      remind: { breakfast: true, lunch: true, dinner: true,
                times: { breakfast: '07:30', lunch: '12:00', dinner: '18:30' } },
      createdAt: null, updatedAt: null
    },
    indexes: [
      { name: 'idx_openid', keys: [{ _openid: 1 }], unique: true }
    ]
  },

  // 饮食记录
  records: {
    doc: {
      _openid: '',
      date: '2026-09-04',          // YYYY-MM-DD，按天聚合
      meal: 'lunch',               // breakfast / lunch / dinner / snack
      foodId: 'F001',
      name: '米饭（蒸，粳米）',
      grams: 150,
      carb: 38.9, protein: 3.9, fat: 0.5, kcal: 175,
      createdAt: null
    },
    indexes: [
      { name: 'idx_openid_date', keys: [{ _openid: 1 }, { date: -1 }] },
      { name: 'idx_date_meal', keys: [{ date: -1 }, { meal: 1 }] }
    ]
  },

  // 体重记录
  weights: {
    doc: { _openid: '', date: '2026-09-04', weight: 55.0, createdAt: null },
    indexes: [{ name: 'idx_openid_date', keys: [{ _openid: 1 }, { date: -1 }] }]
  },

  // 食物库（只读，共 200 条；由 03-数据/foods_200.json 导入）
  foods: {
    doc: {
      id: 'F001', cat: '01 谷薯杂豆·主食', name: '大米（粳米，生）', alias: '白米、东北大米',
      carb: 77.9, protein: 7.4, fat: 0.8, kcal: 348, serving: 75
    },
    indexes: [
      { name: 'idx_id', keys: [{ id: 1 }], unique: true },
      { name: 'idx_cat', keys: [{ cat: 1 }] }
    ]
  },

  // 用户表
  users: {
    doc: { _openid: '', createdAt: null, lastLoginAt: null },
    indexes: [{ name: 'idx_openid', keys: [{ _openid: 1 }], unique: true }]
  }
}

/** 在云函数中执行一次即可完成建表 + 建索引 */
async function initCollections() {
  const cloud = require('wx-server-sdk')
  cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
  const db = cloud.database()
  const log = []

  for (const name of Object.keys(COLLECTIONS)) {
    const cfg = COLLECTIONS[name]
    try {
      await db.createCollection(name)
      log.push(`[created] ${name}`)
    } catch (e) {
      log.push(`[exists ] ${name} (${e.message})`)
    }
    for (const idx of cfg.indexes || []) {
      try {
        await db.collection(name).createIndex(idx)
        log.push(`  index ok: ${idx.name}`)
      } catch (e) {
        log.push(`  index skip: ${idx.name} (${e.message})`)
      }
    }
  }
  return log
}

module.exports = { COLLECTIONS, initCollections }

// cloudfunctions/saveRecord —— 写入饮食记录（PRD F24 / F25）
const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()

exports.main = async (event, context) => {
  const { openid } = cloud.getWXContext()
  const { date, meal, foodId, name, grams, carb, protein, fat, kcal } = event

  // 服务端二次校验，防止客户端伪造（PRD §2.10 安全要求）
  const num = v => typeof v === 'number' && isFinite(v) && v >= 0
  if (!date || !meal || !foodId || !num(grams) || !num(carb) || !num(protein) || !num(fat) || !num(kcal)) {
    return { ok: false, msg: '参数非法' }
  }
  if (grams > 5000) return { ok: false, msg: '单次重量不得超过 5000g' }

  const res = await db.collection('records').add({
    data: {
      _openid: openid, date, meal, foodId, name,
      grams, carb, protein, fat, kcal,
      createdAt: db.serverDate()
    }
  })
  return { ok: true, _id: res._id }
}

// cloudfunctions/login —— 换取 openid，游客数据继承入口（PRD §2.1）
const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })

exports.main = async (event, context) => {
  const wxContext = cloud.getWXContext()
  const openid = wxContext.OPENID
  const db = cloud.database()
  const _ = db.command

  // upsert 用户
  const col = db.collection('users')
  const exist = await col.where({ _openid: openid }).count()
  if (exist.total === 0) {
    await col.add({
      data: {
        _openid: openid,
        createdAt: db.serverDate(),
        lastLoginAt: db.serverDate()
      }
    })
  } else {
    await col.where({ _openid: openid }).update({
      data: { lastLoginAt: db.serverDate() }
    })
  }

  return { openid, appid: wxContext.APPID, unionid: wxContext.UNIONID || '' }
}

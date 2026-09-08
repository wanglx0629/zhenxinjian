// cloudfunctions/sendRemind —— 三餐提醒定时触发（PRD F27）
// 需在 cloudfunctions/sendRemind/config.json 中配置定时触发器：
//   { "triggers": [{ "name": "remind", "type": "timer", "config": "0 30 7 * * * *" }] }
// 建议建三个云函数或同一函数配三条 cron，分别对应用户设置的早/午/晚时间。
const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()

exports.main = async (event, context) => {
  const meal = event.meal || 'lunch'   // breakfast / lunch / dinner
  const now = new Date()

  // 取出该餐次已开启提醒、且提醒时间 <= 当前时间的用户
  const users = await db.collection('profiles')
    .where({ [`remind.${meal}`]: true })
    .limit(100)
    .get()

  const tasks = users.data.map(async u => {
    const t = u.remind && u.remind.times ? u.remind.times[meal] : null
    if (!t) return
    // 命中当前分钟才推送（cron 每分钟跑一次做精确匹配）
    if (t !== `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`) return
    try {
      await cloud.openapi.subscribeMessage.send({
        touser: u._openid,
        templateId: event.templateId,
        page: 'pages/home/home',
        data: {
          thing1: { value: `${{ breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }[meal]}记录提醒` },
          thing2: { value: '记得记录这一餐，保持数据连续' }
        }
      })
    } catch (e) {
      // 用户未授权订阅消息时静默降级，不做打扰（PRD §2.9 已知限制）
      console.warn('send fail', u._openid, e.message)
    }
  })
  await Promise.all(tasks)
  return { ok: true, count: users.data.length }
}

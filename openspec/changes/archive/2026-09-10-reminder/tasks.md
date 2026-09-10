# Tasks — 三餐饮食提醒（P13 提醒设置 + 微信订阅消息推送）

## 1. 数据库

- [x] 1.1 编写 `sql/change7_user_reminders.sql`：新建 `user_reminders`（总开关 + 三餐开关/时间 + 订阅额度，默认 08:30/12:00/18:30，生成列 `active_user_id` 活跃唯一）与 `reminder_send_log`（user_id + remind_date + meal_type + send_status + fail_reason + template_id）；两表均带 `delete_flag`/`status`/审计列，符合开发规范 §4
- [x] 1.2 本地执行迁移脚本并核对表结构（生成列唯一索引生效、utf8mb4、注释齐全）

## 2. 后端提醒设置与订阅额度

- [x] 2.1 `ExceptionConstant` 启用 407xx：40701 `REMINDER_TIME_INVALID`、40702 `REMINDER_TEMPLATE_MISSING`；`MealTypeEnum` 复用既有早/午/晚（加餐不参与提醒）
- [x] 2.2 新建 PO/Mapper：`UserReminder`（`@TableLogic`，含 subscribeCredit）、`ReminderSendLog`；对应 Mapper 接口
- [x] 2.3 `WxMaConfiguration.WxMaProperties` 增 `remindTemplateId`（`zhenxinjian.wechat.remind-template-id`），application yml 留配置位（不入库、不硬编码）
- [x] 2.4 `ReminderService`：`getOrCreate`（无记录按默认值落库，响应附 subscribeCredit 与模板 ID）；`save`（整体保存，HH:mm 正则校验失败抛 40701；总开关关闭时保留三餐原值）；`reportSubscribe`（额度 +1，同分钟内重复上报幂等去重）
- [x] 2.5 `ReminderController`：`GET /api/reminder`、`PUT /api/reminder`、`POST /api/reminder/subscribe`；统一 `Result` 返回、用户归属取 `UserContext`

## 3. 后端定时推送

- [x] 3.1 `ReminderPushTask`：`@Scheduled(fixedDelay = 60_000)` 扫描「总开关开 + 任一餐别时间 = 当前 HH:mm（Asia/Shanghai）」的设置行
- [x] 3.2 判定链按 D4 顺序：当日该餐别无成功日志（I12 单次）→ 当日该餐别无饮食记录（复用 `DietRecordMapper`）→ `wechat_openid` 非空（游客跳过）→ subscribeCredit > 0 → 模板 ID 已配置（未配置任务空转）
- [x] 3.3 命中后经 `WxMaService.getMsgService().sendSubscribeMsg(...)` 下发（page 跳首页，文案随模板字段定）；成功则同事务扣额度 + 写成功日志，微信异常仅写失败日志不扣额度、不重试
- [x] 3.4 `ReminderMigrator implements GuestDataMigrator`：migrate（正式用户无设置 → 改归属，幂等；已有 → 游客记录逻辑删除）；purge（两表逻辑删除）；注册进既有迁移编排与过期清理流程

## 4. 小程序 P13 与入口

- [x] 4.1 `api/reminder.ts`：get/save/reportSubscribe 三方法；`pages.json` 注册 `pages/reminder/index`
- [x] 4.2 `pages/reminder/index.vue`：onShow 拉取设置；总开关 + 早/午/晚三行（switch + time picker）；保存统一 PUT；首次任一开关由关切开时先 `wx.requestSubscribeMessage(tmplIds 取接口下发模板 ID)` 引导授权，保存成功后再发起订阅补额度，接受则上报 reportSubscribe
- [x] 4.3 降级与提示：拒绝授权不阻塞保存并 toast「未授权将无法接收推送」；游客展示「授权登录后才能接收微信推送」；额度 0 且存在开启项时展示重新授权提示
- [x] 4.4 `pages/mine/index.vue` 功能列表加「提醒设置」（副标题 已开启/已关闭，onShow 同步），`navigateTo pages/reminder/index`

## 5. 联调与验收

- [ ] 5.1 设置闭环走查：首次 GET 默认值落库（08:30/12:00/18:30 全开）；PUT 保存后重查一致；非法时间 40701 拒存；关总开关保留三餐原值
- [ ] 5.2 推送链路验收（开发环境模板 ID 配置后）：造到点设置，核对单次去重（同日同餐别仅一条成功日志）、已记录跳过（先记午餐则午餐不推）、额度扣减（成功 -1、失败不扣）、额度 0 静默、游客不推送
- [ ] 5.3 订阅上报幂等：同分钟重复调 reportSubscribe 额度仅 +1
- [ ] 5.4 游客迁移验收：无冲突改归属（重复执行幂等）、冲突保留正式设置；过期清理两表逻辑删除
- [ ] 5.5 P13/P14 对照原型走查：三行开关时间交互、首次开启授权引导、保存后补额度、游客提示、我的页副标题状态同步
- [x] 5.6 `mvn compile` 与 `npm run build:mp-weixin` 通过；`openspec validate reminder --strict` 通过；规格与实现一致性自查后归档

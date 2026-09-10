# Proposal — 三餐饮食提醒（P13 提醒设置 + 微信订阅消息推送）

## Why

MVP 四大能力闭环（算得准、记得快、有提醒、能坚持）中「有提醒」尚未落地：PRD F25/F27 要求总开关 + 三餐独立开关/时间（默认 08:30/12:00/18:30）、每日单次、已记录不重复（不变量 I12 / EVAL-020）。小程序工程骨架分析已明确口径「提醒走后端定时任务」（`doscFile/projectFile/03-小程序工程骨架分析.md` §替代方案），骨架云函数 `sendRemind` 仅为演示。提醒是小白用户「能坚持」的关键召回手段，也是 Phase 3 排期的唯一内容，必须补齐。

## What Changes

- 新建 `user_reminders` 表（每用户一条活跃记录：总开关 + 早/午/晚三餐独立开关与时间，默认全开 08:30/12:00/18:30）与 `reminder_send_log` 表（推送日志：用户 + 日期 + 餐别，支撑每日单次去重与失败追溯）
- 后端提醒设置接口：`GET /api/reminder`（无记录时按默认值落库返回）、`PUT /api/reminder`（总开关/三餐开关/时间整体保存）
- 订阅额度接口：`POST /api/reminder/subscribe`（前端 `wx.requestSubscribeMessage()` 授权成功后上报，额度 +1）；推送消耗额度，额度为 0 时静默跳过（PRD §2.9 已知限制：未授权静默降级不打扰）
- 后端定时任务：分钟级扫描到点且开启提醒的正式用户，校验当日未推送（I12 单次）且该餐别当日无饮食记录（已记录不重复），经 `WxMaService` 下发订阅消息并写日志；模板 ID 走配置 `zhenxinjian.wechat.remind-template-id`，未配置时任务空转
- 小程序 P13 提醒设置页 `pages/reminder/index.vue`：总开关 + 三餐开关/时间选择器；首次开启时引导 `wx.requestSubscribeMessage()` 授权，每次保存后重新发起订阅补充额度；订阅状态与「需登录后推送」提示
- 「我的」页功能列表新增「提醒设置」入口（home-dashboard 归档时显式留置给本变更）
- 游客迁移补齐：`ReminderMigrator` 迁移游客提醒设置（幂等 UPDATE）；过期游客清理两表逻辑删除。**口径**：游客无 openid，设置可保存与迁移，但实际推送仅在授权登录后生效（页面明示）
- 错误码启用 407xx 段位（提醒时间格式非法 / 订阅模板未配置）

**范围外（后续变更）**：首页提醒状态卡（home-dashboard 范围外条目继续留置，非 PRD 必需）；加餐提醒（PRD 仅三餐）；长期订阅消息（类目不支持，一期仅一次性订阅）；532 推进与经期管理（P08/P09）。

## Capabilities

### New Capabilities

- `diet/reminder`: 三餐饮食提醒全生命周期——提醒设置查询与保存（默认值落库）、总开关与三餐独立开关/时间、订阅消息授权上报与额度管理、分钟级定时推送（每日单次去重 + 已记录跳过 + 静默降级）、推送日志、游客迁移归属

### Modified Capabilities

- `mine/center`: 功能列表新增「提醒设置」入口（跳 P13），其余需求不变

## Impact

- **DB**：`sql/change7_user_reminders.sql`（新建 `user_reminders` / `reminder_send_log` 两表，均带 `delete_flag`/`status`/审计列；`user_reminders` 按生成列活跃唯一约束每用户一条）
- **后端**：新 `ReminderController/Service/Mapper`、`UserReminder`/`ReminderSendLog` PO；`ReminderPushTask` 分钟级 `@Scheduled`；`ReminderMigrator implements GuestDataMigrator`；`ExceptionConstant` 启用 407xx；`ZhenxinjianProperties`/application yml 增 `remind-template-id` 配置项；复用既有 `WxMaService` 与 `DietRecordMapper`（当日餐别记录判定）
- **小程序**：`pages/reminder/index.vue`（P13）+ `pages.json` 注册；`api/reminder.ts`；`pages/mine/index.vue` 功能列表加入口；`wx.requestSubscribeMessage()` 引导与降级提示
- **外部依赖**：需在微信公众平台为小程序申请一次性订阅消息模板（三餐提醒类目），模板 ID 配入 `application-*.yml`；未配置时推送任务空转、设置功能照常可用
- **口径真源**：MVP v1 §提醒、不变量 I12（提醒单次性）、骨架 `sendRemind` 云函数与 `pages/remind`（仅作口径参考，默认时间以 MVP v1 的 08:30/12:00/18:30 为准，骨架 07:30 不采纳）

# Design — 三餐饮食提醒

## Context

后端已有 `WxMaService`（`WxMaConfiguration`，appid 配置时注册）与 `@EnableScheduling`（游客过期清理 cron 已在用）；`User.wechatOpenid` 可直接作为推送 `touser`；`DietRecordMapper` 可判定「当日该餐别已有记录」。小程序侧骨架 `pages/remind` 与云函数 `sendRemind` 提供口径参考（分钟级匹配、未授权静默降级），但默认时间以 MVP v1 的 08:30/12:00/18:30 为准。建表须遵守开发规范 §4：`delete_flag` + `@TableLogic`、业务表 `status` 列、活跃唯一走生成列。

## Goals / Non-Goals

**Goals:**

- 提醒设置 CRUD（每用户一条活跃记录）+ 分钟级定时推送闭环（单次去重 / 已记录跳过 / 额度消耗 / 静默降级）
- 订阅消息授权额度模型（上报 +1、推送 -1、耗尽静默）
- 游客设置保存、迁移与过期清理；「我的」页入口

**Non-Goals:**

- 加餐提醒、长期订阅、模板运营（模板申请为运维动作，仅留配置位）
- 首页提醒状态卡；多条模板/多语言文案；推送失败重试与告警

## Decisions

### D1 表结构：`user_reminders` 单表单行 + `reminder_send_log` 日志表

`user_reminders`：`id / user_id / master_switch / breakfast_switch / breakfast_time / lunch_switch / lunch_time / dinner_switch / dinner_time / subscribe_credit / status / delete_flag / create_by...审计列`；按开发规范 §4 用生成列（`active_user_id = IF(delete_flag=0, user_id, NULL)`）+ 唯一索引保证每用户一条活跃记录。开关与额度单行存放是因为 PRD 仅三餐且无加餐规划，列式比子表简单且一次读全。

`reminder_send_log`：`id / user_id / remind_date / meal_type / send_status(成功/失败/跳过原因) / fail_reason / template_id / status / delete_flag / 审计列`。去重靠「查询当日该餐别成功日志是否存在」而非唯一索引——失败/跳过也要写日志（追溯用），若上唯一约束反而妨碍多状态记录；并发重复由分钟任务单实例 + 先查后写兜底（单机单体，无双实例竞争）。

### D2 定时任务：`@Scheduled(fixedDelay = 60_000)` 分钟级扫描，不用延迟队列

每分钟查出「总开关开 + 任一餐别时间 = 当前 HH:mm」的设置行（时区以 JVM/DB 的 Asia/Shanghai 为准），逐条过判定链（单次 → 已记录 → openid → 额度 → 模板配置）后调 `WxMaService.getMsgService().sendSubscribeMsg(...)`。备选方案（RocketMQ 延迟消息 / Redis ZSET 延迟队列）对「每分钟几十到几百条」的一期规模属于过度设计；cron 表达式按用户时间动态匹配不可行，故用 fixedDelay 扫表。任务内不做重试（PRD 静默降级口径）。

### D3 订阅额度模型：用户维度计数，授权上报 +1，推送成功 -1

微信一次性订阅消息每次接受 = 一次下发资格（按模板计）。一期只有一个模板，故额度退化为用户维度计数器 `subscribe_credit`：前端每次保存设置后调 `wx.requestSubscribeMessage()`（每次调用用户可再接受一次），接受则 `POST /api/reminder/subscribe` 上报 +1；推送成功 -1；额度 0 静默跳过。备选「不记额度、发送时直接尝试、43101 失败即停」无法让前端提前感知需重新授权，体验差，弃用。额度上限不设硬顶（微信侧自有限流），但上报接口 MUST 幂等防抖：同一分钟内的重复上报只记一次（防前端重复调用刷额度）。

### D4 推送判定顺序：单次 → 已记录 → openid → 额度 → 模板

先查「当日该餐别成功日志」（ cheapest、最常见命中），再查饮食记录，再校验 openid/额度/模板配置。全部判定在事务外只读执行，发送成功后同事务扣额度 + 写日志，失败仅写失败日志不扣额度。

### D5 游客口径：设置可建可迁，推送仅正式用户

游客无 openid，无法接收订阅消息，但 PRD 要求游客 3 天完整功能体验——故设置接口对游客开放（体验配置流程），推送任务扫描时以 `wechat_openid IS NOT NULL` 过滤。迁移器 `ReminderMigrator implements GuestDataMigrator`：`migrate` 时正式用户已有设置则逻辑删除游客记录（保留正式设置），否则改归属；`purge` 两表逻辑删除。与 `CarbCyclePlanMigrator` 的「冲突降级」模式一致。

### D6 错误码与配置

- 40701 `REMINDER_TIME_INVALID` 提醒时间格式非法；40702 `REMINDER_TEMPLATE_MISSING` 订阅模板未配置（保留，运行时仅空转不报错，预留给后续管理端校验）
- 模板 ID 走 `zhenxinjian.wechat.remind-template-id`（`WxMaConfiguration.WxMaProperties` 扩展），不入库、不硬编码（Never 规则）

### D7 前端：独立页面 + 每次保存补额度，不用全局 store

`pages/reminder/index.vue` 进入即 `GET /api/reminder`；开关/时间变更本地暂存，点保存统一 `PUT`；保存成功后调 `wx.requestSubscribeMessage({ tmplIds: [templateId] })`（模板 ID 由 GET 响应下发，避免前端硬编码），接受则上报。首次开启任一开关时先引导授权再保存。「我的」页入口副标题状态随 `onShow` 刷新，不引入新的 pinia store（设置读写低频，直接走 api 层）。

## Risks / Trade-offs

- [分钟级扫描当用户量增长后全表扫压力上升] → 一期用户规模小；查询带 `master_switch=1` 与时间匹配索引（`(master_switch, breakfast_time/lunch_time/dinner_time)` 三列分别索引或按 OR 拆分三次小查询），后续可平滑替换为延迟队列
- [用户把系统时间/时区改掉导致匹配偏差] → 匹配以服务端 Asia/Shanghai 为准，与 DB/JDBC 时区口径一致
- [一次性订阅天然需要用户反复授权，召回率受限（PRD §2.9 已知限制）] → 页面在额度 0 且存在开启项时提示重新授权；不做强打扰
- [微信下发接口失败（模板未审、用户取关小程序）] → 失败仅记日志不重试，符合静默降级口径；上线前需运维完成模板申请与审核（外部依赖）
- [上报接口被恶意调用刷额度] → 同分钟幂等 + 额度仅决定能否收到自家提醒，无外溢价值

## Migration Plan

1. 执行 `sql/change7_user_reminders.sql`（建两表，无存量数据迁移）
2. 部署后端（新接口 + 定时任务；模板 ID 未配置时任务空转，行为安全）
3. 微信公众平台申请一次性订阅消息模板并审核通过 → 配置 `remind-template-id` → 推送生效
4. 发布小程序新版本（P13 + 我的页入口）

回滚：下线小程序页面入口与后端任务开关（配置置空即空转），表保留无副作用。

## Open Questions

- 订阅消息模板的具体字段（thing1/thing2 文案与长度限制）待模板申请审核结果确定，仅影响推送 payload 组装，不影响表结构与接口契约

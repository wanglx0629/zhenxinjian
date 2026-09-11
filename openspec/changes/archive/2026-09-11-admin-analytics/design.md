# 管理后台与行为分析设计文档

> 落地实施计划：docs/superpowers/plans/2026-09-11-admin-analytics.md
> 日期：2026-09-11 · 状态：已确认 · 路径：Architectural（方案一：MySQL 单表直存 + 定时聚合）

## 1. 背景与范围

PRD 待确认项「管理后台一期功能边界」经需求方确认为：食物库维护 + 用户管理 + 饮食记录查看 + 行为分析（DAU/MAU + 功能点击埋点）。

**范围内**

- 小程序行为埋点采集与批量上报（核心行为 + 页面 PV + 功能点击）
- 活跃统计（DAU/MAU，口径：当日埋点表有任意事件的去重用户）
- 管理端数据看板、用户管理（改造）、食物库维护、饮食记录查看

**范围外**

- 埋点事件码管理端可配置化（改枚举发版即可）
- 管理端代改/代删用户业务数据（只读原则）
- track_event 明细归档策略（数据量小，后续按需加 90 天清理）
- 广告/营销/推送运营功能

## 2. 数据模型

`sql/change9_admin_analytics.sql`，三表均带 `delete_flag`/`status`/审计列（建表规约）。

### 2.1 `track_event`（埋点明细表）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK AUTO | |
| user_id | BIGINT NULL | 用户 ID；未登录态允许 NULL |
| user_type | VARCHAR(16) NULL | WECHAT/GUEST 冗余（聚合免 JOIN，游客被清理后仍可统计） |
| event_code | VARCHAR(64) | 事件码，白名单枚举 |
| event_name | VARCHAR(64) | 事件中文名（冗余） |
| page | VARCHAR(64) | 页面路径（PV/点击类填写） |
| extra_json | VARCHAR(512) | 扩展 JSON（关键词/餐别/食物 ID），不建索引 |
| client_time | DATETIME | 端上发生时间（仅记录，不参与统计） |
| status / delete_flag / create_by / update_by / create_time / update_time | | 规约列 |

索引：`idx_create_time(create_time)`、`idx_user_time(user_id, create_time)`、`idx_code_time(event_code, create_time)`

### 2.2 `stat_daily_active`（每日活跃聚合表）

| 列 | 说明 |
|---|---|
| id / stat_date(DATE) | 统计日期 |
| dau | 当日去重活跃用户（user_id 非 NULL 去重） |
| guest_dau | 其中游客数（按 track_event.user_type） |
| new_user | 当日新增注册（users.create_time 聚合） |
| 规约列 | |

幂等：软删表禁普通 UNIQUE，聚合任务先软删当日旧行再插新行。

### 2.3 `stat_event_daily`（事件日聚合表）

| 列 | 说明 |
|---|---|
| id / stat_date / event_code / event_name | |
| pv | 当日事件总次数 |
| uv | 当日去重用户数 |
| 规约列 | |

幂等同上。

## 3. 埋点链路

### 3.1 上报协议

`POST /api/track/events`（登录态可选，游客/未登录引导页也可上报），单批 ≤ 50 条：

```json
{ "events": [ { "eventCode": "record_add", "page": "pages/record/add",
    "extra": {"mealType": 2, "source": 1}, "clientTime": "2026-09-11 12:30:00" } ] }
```

- eventCode 白名单校验，非法码整批拒收 40901
- 后端批量 insert，不阻塞 C 端业务
- 不做严格幂等（埋点允许少量重复）；限流 = 认证 + 单批上限

### 3.2 小程序端采集

新增 `src/utils/track.ts`：

- `track(eventCode, extra?)`：内存队列 + storage 持久化（`zxj_track_queue`），每 10 秒或满 20 条批量上报；失败保留重试，队列上限 200
- `trackPage(pagePath)`：封装 `page_view`
- App `onLaunch` 启动 flush 定时器；各页面 `onShow` 调 `trackPage`；关键行为处调 `track`

### 3.3 事件码清单（`TrackEventEnum` 后端唯一真源）

| 分类 | event_code | 触发点 |
|---|---|---|
| 账号 | `login_wechat` / `login_guest` / `login_fail` | 登录成功/游客进入/授权失败 |
| 页面 PV | `page_view`（page 列区分 16 页） | 各页 onShow |
| 饮食记录 | `record_add` / `record_edit` / `record_delete` | 增删改成功 |
| 食物库 | `food_search`(extra.keyword) / `food_detail` / `food_custom_add` | 搜索/详情/自定义 |
| 模式 | `mode_select`(extra.mode) / `mode_switch` / `plan_view` | 选/切模式/看计划 |
| 提醒 | `reminder_save` / `reminder_subscribe` | 保存/授权上报 |
| 身体数据 | `body_save` / `weight_add` / `menstrual_save` | 建档/体重/经期保存 |
| 功能点击 | `home_quick_entry`(extra.target) / `food_hot_click` / `food_history_click` | 快捷入口/热门/历史 |

**统计口径**：活跃 = 当日 track_event 中 user_id 非 NULL 去重；统计一律用 `create_time`（防端上时间篡改）。

## 4. 管理端接口

全部 `/api/admin/**`，`@PreAuthorize("hasRole('ADMIN')")`，错误码 409xx（40901 埋点事件非法 / 40902 统计参数非法 / 40903 食物维护冲突）。

### 4.1 `AdminStatsController`（`/admin/stats`）

| 接口 | 说明 |
|---|---|
| `GET /overview` | 今日 DAU（实时 DISTINCT track_event）、昨日 DAU、近 30 天 MAU、累计用户、累计饮食记录 |
| `GET /active-trend?days=7\|30` | DAU 趋势（查聚合表，默认 30） |
| `GET /event-rank?days=7&limit=10` | 功能排行（stat_event_daily 按 pv） |
| `GET /page-rank?days=7&limit=10` | 页面排行（page_view 按 page 聚合） |
| `POST /aggregate?date=` | 手工重跑某日聚合（补数，幂等） |

### 4.2 `AdminFoodController`（`/admin/foods`）

| 接口 | 说明 |
|---|---|
| `GET /admin/foods` | 分页 + 关键字 + 分类 + 来源 + 状态筛选 |
| `POST /admin/foods` | 新增内置（code F201 起顺延；同名拒收 40903） |
| `PUT /admin/foods/{id}` | 编辑（仅内置） |
| `DELETE /admin/foods/{id}` | 软删（仅内置；diet_records 快照冗余不受影响） |
| `PUT /admin/foods/{id}/status` | 停用/启用（停用 C 端搜索不可见） |

### 4.3 用户管理

复用现有 `/users`（已带 ADMIN 鉴权）；新增 `GET /admin/users/{id}/profile` 聚合视图（用户 + user_body 档案 + 当前模式）。

### 4.4 `AdminDietController`（`/admin/diet-records`）

`GET /admin/diet-records`：分页，按用户 ID/昵称、日期范围、餐别筛选；**只读**。

### 4.5 聚合任务

`StatAggregateTask`：`@Scheduled(cron = "0 0 1 * * ?")` 统计昨日 → 两聚合表软删重插；支持按日期重跑。

**展示口径**：今日数据实时查 track_event（界面标"实时"），昨日及以前查聚合表。

## 5. 前端页面（zhenxinjian-front）

路由全部 `meta: { admin: true }`：

```
/ MainLayout
├─ /dashboard      数据看板（新首页）
├─ /users          用户管理（改造接真实接口 + 详情抽屉）
├─ /foods          食物库维护（列表 + 新增/编辑弹窗）
├─ /diet-records   饮食记录查看（只读）
└─ /login          不动
```

- **看板**：4 卡片（今日 DAU 实时徽标/昨日 DAU/30 天 MAU/累计用户）+ DAU 趋势折线（7/30 天，ECharts）+ 功能 TOP10 条形 + 页面 TOP10 表格
- **用户管理**：分页筛选；详情抽屉（profile 聚合）；编辑（昵称/状态/角色/备注）；软删；新增弹窗
- **食物库**：编号/名称/分类/三宏/热量/来源/状态列；内置行可编辑/停用/删除，自定义行只读置灰；弹窗校验（三宏 1 位小数、热量整数）
- **饮食记录**：纯只读表格，无操作列
- **api/**：`stats.ts` `food.ts` `diet.ts` `adminUser.ts`，走既有 request.ts
- **删除**：`view/websocket`、`view/register`、`component/DemoChart.vue`、`DemoWebSocket.vue`、`WsStatusCard.vue`

## 6. 错误处理与测试

- 40901/40902/40903 进 `ExceptionConstant`/`CommonConstant`；接口统一 `Result`
- UT：TrackEventEnum 白名单、批量校验、聚合幂等（软删重插）、食物 code 顺延与同名冲突
- IT：上报→落库→聚合→看板查询全链路；管理端越权 403
- 验收：小程序触发事件 → 次日聚合 → 看板展示一致；DAU 与手工 SQL 核对

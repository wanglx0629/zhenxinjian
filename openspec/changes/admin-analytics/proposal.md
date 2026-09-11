# Proposal — 管理后台与行为分析（数据看板 + 食物库维护 + 饮食记录查看 + 埋点统计）

## Why

PRD 待确认项「管理后台一期功能边界」经确认为：食物库维护 + 用户管理 + 饮食记录查看 + 行为分析（DAU/MAU + 功能点击埋点）。当前管理端仅有脚手架用户 CRUD，无业务运营能力，也无任何埋点采集。活跃与功能使用情况完全黑盒，无法支撑运营决策。

## What Changes

- 新建 `track_event` 埋点明细表与 `stat_daily_active`/`stat_event_daily` 两张聚合表（均带 `delete_flag`/`status`/审计列）
- 小程序 `utils/track.ts` 批量上报（10s/20 条 flush，失败保留重试，队列上限 200，storage 持久化），接入 21 个事件码与 19 页 PV
- 后端 `POST /api/track/events` 白名单校验批量落库（登录态可选，游客/未登录引导页也可上报）
- `StatAggregateTask` 每日 01:00 幂等聚合（软删重插），支持按日期手工重跑补数
- 管理端 `/api/admin/**`：数据看板 5 接口（overview/active-trend/event-rank/page-rank/aggregate）、食物库维护 5 接口、饮食记录只读分页、用户聚合视图
- 前端 zhenxinjian-front：看板页（ECharts 趋势 + TOP10）/ 用户页改造加详情抽屉 / 食物库页 / 饮食记录页；删除 websocket/register/home 演示页与 Demo 组件
- 错误码启用 409xx 段（40901 埋点事件非法 / 40902 统计参数非法 / 40903 食物维护冲突）

**范围外**：埋点事件码管理端可配置化（改枚举发版即可）；管理端代改/代删用户业务数据（只读原则）；track_event 明细归档策略；广告/营销/推送运营功能。

## Capabilities

### New Capabilities

- `track/event`: 埋点批量上报、白名单校验（整批拒收）、事件码字典（TrackEventEnum 21 码，后端唯一真源）
- `admin/stats`: overview/active-trend/event-rank/page-rank/aggregate 五接口与每日 01:00 聚合任务（今日实时查明细、昨日及以前查聚合表）
- `admin/food`: 内置食物分页筛选/新增（code F201 起顺延）/编辑/软删/停用启用（自定义食物只读）
- `admin/diet`: 饮食记录只读分页（用户关键字/日期范围/餐别筛选）

### Modified Capabilities

- `admin/user`: 现有 /users CRUD 之上补 `GET /admin/users/{id}/profile` 聚合视图（用户 + 身体档案 + 当前模式/周期）

## Impact

- **DB**：`sql/change9_admin_analytics.sql`（新建 `track_event` / `stat_daily_active` / `stat_event_daily` 三表）
- **后端**：`TrackController`/`TrackEventService`、`TrackEventEnum`（21 码）、`StatAggregateService`/`StatAggregateTask`、`AdminStatsController`、`AdminFoodController`、`AdminDietController`、`AdminUserController`；`ExceptionConstant`/`CommonConstant` 启用 409xx；`application.yml` permit-urls 放行 `/track/events`
- **小程序**：`utils/track.ts`（队列 + storage 持久化 + 定时 flush）+ `api/track.ts` + 19 页 onShow 接入 `trackPage` + 21 类行为事件接入；`App.vue` onLaunch 启动 flush 定时器
- **前端 front**：4 页（dashboard/users 改造/foods/diet-records）+ 4 个 api 模块（stats/adminUser/adminFood/adminDiet）+ 路由/布局调整（移除 websocket/register 演示入口）
- **口径真源**：`docs/superpowers/specs/2026-09-11-admin-analytics-design.md`（已确认设计文档）
- **统计口径**：活跃 = 当日 `track_event` 中 `user_id` 非 NULL 去重（COUNT DISTINCT）；统计一律用 `create_time`（防端上时间篡改）；游客 DAU 按冗余列 `user_type=GUEST` 免 JOIN 统计

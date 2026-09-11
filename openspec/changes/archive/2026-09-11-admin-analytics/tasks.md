# Tasks — 管理后台与行为分析（数据看板 + 食物库维护 + 饮食记录查看 + 埋点统计）

## 1. 数据库

- [x] 1.1 编写 `sql/change9_admin_analytics.sql`：新建 `track_event`（user_id/user_type 冗余/event_code/event_name/page/extra_json/client_time，三索引）、`stat_daily_active`（stat_date/dau/guest_dau/new_user）、`stat_event_daily`（stat_date/event_code/event_name/pv/uv）；三表均带 `delete_flag`/`status`/审计列/`version`，符合建表规约
- [x] 1.2 本地执行迁移并核对表结构（utf8mb4、注释齐全、三索引生效）

## 2. 埋点上报后端

- [x] 2.1 `ExceptionConstant`/`CommonConstant` 启用 409xx（40901 TRACK_EVENT_INVALID / 40902 STATS_PARAM_INVALID / 40903 ADMIN_FOOD_CONFLICT + TRACK_BATCH_MAX_SIZE=50 + TRACK_EXTRA_MAX_LENGTH=512）
- [x] 2.2 新建 `TrackEventEnum`（21 码，code+desc+of 三件套，后端唯一真源）
- [x] 2.3 新建 PO/Mapper/DTO：`TrackEvent`（`@TableLogic`/`@Version`/审计填充）、`TrackEventMapper`、`TrackEventItemDTO`（eventCode @NotBlank；extra Map；clientTime 可空）、`TrackEventBatchDTO`（events @NotEmpty @Size(max=50)）
- [x] 2.4 `TrackEventService.saveBatch`：白名单整批校验（任一非法整批拒收 40901）、event_name 冗余枚举 desc、extra 序列化超 512 截断、未登录 userId/userType 传 null
- [x] 2.5 `TrackController`：`POST /track/events`（登录态可选）；`application.yml` permit-urls 追加 `/track/events`
- [x] 2.6 `TrackEventServiceTest` 3 用例 PASS（合法批量落库 / 非法码 40901 整批拒收 / 未登录允许）+ `mvn compile` 通过

## 3. 聚合服务与定时任务

- [x] 3.1 新建 PO/Mapper：`StatDailyActive`、`StatEventDaily`（风格同 ReminderSendLog）及对应 Mapper
- [x] 3.2 `StatAggregateService.aggregate(date)`：COUNT(DISTINCT user_id) 算 DAU/guest_dau（user_type=GUEST 免 JOIN）、users.create_time 算 new_user；同事务软删当日旧行 + 插新行（幂等，支持重跑补数）；事件按 event_code 分组 pv/uv
- [x] 3.3 `StatAggregateTask`：`@Scheduled(cron = "0 0 1 * * ?")` 聚合昨日，异常仅记日志
- [x] 3.4 `StatAggregateServiceTest` 2 用例 PASS（幂等软删重插 / 无事件插零行）+ `mvn compile` 通过

## 4. 数据看板接口

- [x] 4.1 新建 VO：`StatsOverviewVO`（todayDau/yesterdayDau/mau/totalUsers/totalDietRecords）、`DailyActiveVO`、`EventRankVO`、`PageRankVO`
- [x] 4.2 `AdminStatsService`：overview（今日 DAU 实时查 track_event，昨日查聚合表，MAU=近30天去重）；activeTrend（days 1-90 越界 40902，查聚合表）；eventRank/pageRank（days 1-90、limit 1-50 越界 40902）；rerunAggregate（未来日期 40902，委托聚合服务）
- [x] 4.3 `AdminStatsController`：`/admin/stats` 五接口，`@PreAuthorize("hasRole('ADMIN')")` + `@SecurityRequirement(name = "Bearer")`
- [x] 4.4 `AdminStatsServiceTest` 口径用例 PASS；Swagger 走查 ADMIN 200 / 非 ADMIN 403

## 5. 食物库维护接口

- [x] 5.1 新建 `AdminFoodSaveDTO`（宏量 0-100、kcal 0-5000、serving 默认 100）、`AdminFoodQuery extends PageQuery`（keyword/categoryCode/source/status）
- [x] 5.2 `AdminFoodService`：新增内置 code 从 F201 起顺延（max(code 数值)+1）；同名（内置活跃范围）拒收 40903；宏量与 4/4/9 换算偏差 ±10% 校验复用 C 端口径（40402/40403）；编辑/删除/停用仅 source=1 否则 40404
- [x] 5.3 `AdminFoodController`：`GET/POST /admin/foods`、`PUT/DELETE /admin/foods/{id}`、`PUT /admin/foods/{id}/status`
- [x] 5.4 `AdminFoodServiceTest` 4 用例 PASS（同名 40903 / 热量偏差 40403 / 自定义拒写 40404 / 首个 code F201）+ `mvn compile` 通过

## 6. 饮食记录只读 + 用户聚合视图接口

- [x] 6.1 新建 `AdminDietRecordQuery extends PageQuery`（userKeyword/startDate/endDate/mealType）、`AdminDietRecordVO`、`AdminUserProfileVO`
- [x] 6.2 `AdminDietService.page`：userKeyword 纯数字按 id 精确否则昵称模糊取 id 集（无命中返回空页）；分页按 recordDate desc；批量组装用户昵称（已删显示「用户#id」）
- [x] 6.3 `AdminDietController`：`GET /admin/diet-records`（只读，无写接口）
- [x] 6.4 `AdminUserController`：`GET /admin/users/{id}/profile` 聚合视图（用户 + 活跃档案 + 当前模式 + 进行中周期，无则 null）
- [x] 6.5 `mvn compile` 通过 + Swagger 走查分页/聚合/403

## 7. 小程序埋点采集

- [x] 7.1 新建 `api/track.ts`（reportEvents 批量上报）与 `utils/track.ts`（内存队列 + storage 持久化 zxj_track_queue；10s/20条 flush；失败保留重试；队列上限 200；clientTime 仅记录）
- [x] 7.2 `App.vue` onLaunch 启动 `startTrackFlushTimer()`
- [x] 7.3 19 页 onShow 接入 `trackPage`（auth/guide、auth/expire、body/profile、body/result、cycle/plan、cycle/setting、food/custom-edit、food/custom-list、food/detail、food/index、home/index、mine/index、mode/select、record/add、record/index、reminder/index、taper/plan、weight/index、menstrual/index）
- [x] 7.4 21 类行为事件接入（login_wechat/login_guest/login_fail、record_add/edit/delete、food_search/food_hot_click/food_history_click/food_detail/food_custom_add、mode_select/mode_switch、plan_view、reminder_save/reminder_subscribe、body_save、weight_add、menstrual_save、home_quick_entry），埋点失败不阻塞业务
- [x] 7.5 `npx vue-tsc --noEmit` 与 `npm run build:mp-weixin` 通过

## 8. 管理端数据看板页

- [x] 8.1 新建 `api/stats.ts`（getOverview/getActiveTrend/getEventRank/getPageRank）
- [x] 8.2 新建 `view/dashboard/index.vue`：4 卡片（今日 DAU 标"实时"徽标）+ DAU 趋势 ECharts 折线（7/30 天切换）+ 功能 TOP10 + 页面 TOP10 表格；resize 监听与 chart dispose
- [x] 8.3 路由调整：注册 dashboard/foods/diet-records（meta.admin），根 redirect 改 `/dashboard`，移除 websocket/register 路由；`MainLayout.vue` 导航改四项移除 WebSocket 入口
- [x] 8.4 `npm run build` 通过

## 9. 用户管理页改造

- [x] 9.1 新建 `api/adminUser.ts`（getUserProfile）
- [x] 9.2 `view/users/index.vue`：操作列加「详情」，el-drawer + el-descriptions 展示聚合视图（用户字段/身体档案/当前模式/进行中周期，无档案显示"未建档"）
- [x] 9.3 `npm run build` 通过

## 10. 食物库维护页

- [x] 10.1 新建 `api/adminFood.ts`（page/create/update/delete/changeStatus）
- [x] 10.2 新建 `view/foods/index.vue`：筛选（关键字/分类/来源/状态）+ 表格（code/名称/分类/三宏/热量/来源/状态）+ 新增/编辑弹窗（分类静态字典与后端 FoodCategoryEnum 核对）；内置行可编辑/停用/删除，自定义行按钮置灰
- [x] 10.3 `npm run build` 通过

## 11. 饮食记录查看页 + 演示残留清理

- [x] 11.1 grep 确认 DemoChart/DemoWebSocket/WsStatusCard/utils/ws/view/home/view/register/view/websocket 无残留引用
- [x] 11.2 新建 `api/adminDiet.ts` 与 `view/diet-records/index.vue`（用户关键字/日期范围/餐别筛选，只读无操作列）
- [x] 11.3 `git rm` 演示残留 7 文件（view/home、view/websocket、view/register、DemoChart、DemoWebSocket、WsStatusCard、utils/ws）
- [x] 11.4 `npm run build` 通过

## 12. 联调验收与归档

- [ ] 12.1 埋点闭环：游客进入 → 浏览页面 → 添加记录 → track_event 有 login_guest/page_view/record_add 且 user_type=GUEST；断网重连队列补发
- [ ] 12.2 聚合幂等：手工 POST aggregate 重跑两次行数不翻倍；看板数据与手工 SQL 核对一致
- [ ] 12.3 食物库维护：新增 F201 顺延/同名 40903/停用 C 端不可见/自定义置灰
- [ ] 12.4 饮食记录筛选与用户详情抽屉展示正确；非 ADMIN 路由守卫拦截 + 接口 403
- [x] 12.5 `mvn compile` + 前端/小程序构建通过；`openspec validate admin-analytics --strict` 通过；规格与实现一致性自查后归档

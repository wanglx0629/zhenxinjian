# admin/stats Specification

## Purpose
为管理员提供运营数据看板：总览卡片（今日 DAU 实时/昨日 DAU/MAU/累计）、DAU 趋势、功能与页面排行、手工重跑聚合补数；今日实时查明细表，昨日及以前查聚合表；每日 01:00 定时幂等聚合。
## Requirements
### Requirement: 总览与趋势查询

系统 SHALL 提供管理端统计接口（`/api/admin/stats/**`，仅 ADMIN 可访问）：overview 返回今日 DAU（实时 COUNT(DISTINCT user_id) 查 track_event，user_id 非 NULL）、昨日 DAU（查 stat_daily_active，无行为 0）、近 30 天 MAU（track_event 去重）、累计用户与累计饮食记录；active-trend 按 days（1-90，默认 30）查 stat_daily_active 返回 DAU/游客 DAU/新增用户序列。活跃统计 MUST 以 create_time 为准。

#### Scenario: overview 今日实时

- **WHEN** 管理员查询 overview，当日 track_event 有 3 个去重用户事件
- **THEN** todayDau=3（实时明细口径），yesterdayDau 取昨日聚合行

#### Scenario: active-trend 查聚合表

- **WHEN** 管理员查询 active-trend?days=7
- **THEN** 返回 stat_daily_active 近 7 天（截至昨日）序列，按 stat_date 升序

#### Scenario: 参数越界

- **WHEN** days=0 或 days=91 或 limit=51
- **THEN** 拒绝，返回 40902

### Requirement: 功能与页面排行

系统 SHALL 提供 event-rank（stat_event_daily 按 stat_date 范围 SUM(pv)/SUM(uv) 分组排序取 TOP N）与 page-rank（track_event 中 page_view 按 page 分组 COUNT/COUNT(DISTINCT user_id) 排序取 TOP N）；days MUST 1-90、limit MUST 1-50。

#### Scenario: 功能排行 TOP N

- **WHEN** 管理员查询 event-rank?days=7&limit=10
- **THEN** 返回近 7 天聚合按 pv 降序至多 10 条（eventCode/eventName/pv/uv）

#### Scenario: 页面排行按 page 聚合

- **WHEN** 管理员查询 page-rank?days=7&limit=10
- **THEN** 返回 page_view 事件按 page 分组的 pv/uv TOP 10

### Requirement: 每日聚合与手工重跑

系统 SHALL 每日 01:00 定时聚合昨日 track_event → stat_daily_active（dau/guest_dau/new_user）与 stat_event_daily（pv/uv）；聚合 MUST 幂等：先软删当日旧行再插新行（软删表禁普通 UNIQUE）；MUST 提供 `POST /admin/stats/aggregate?date=` 手工重跑指定日期（未来日期拒绝 40902）。

#### Scenario: 重跑幂等

- **WHEN** 管理员对同一日期连续调用两次 aggregate
- **THEN** 两张聚合表该日期各仅一行（第二次软删重插），行数不翻倍

#### Scenario: 非管理员访问

- **WHEN** 非 ADMIN 用户调用任一 /admin/stats 接口
- **THEN** 返回 403


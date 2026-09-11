# Design — 532 碳水渐降推进（P08/P09 + 经期管理）

## Context

现有 532 目标仅取 `user_body` 目标快照（`DietRecordService.summary` 直接读 `targetCarb/targetProtein/targetFat/targetKcal`），无推进层。`BodyCalcService` 产出 532 基线（碳 50/蛋 30/脂 20），`CycleCalcService` 已示范「计算引擎 + 服务层」分离与 `BigDecimal` 克数计算。小程序侧 `calculator.ts` 已含 `menstrualPhase`、`isPlateau`、`plan532Stages` 参考实现（仅前端预览）。本变更把经期四阶段判定、平台判定、532 推进目标公式后端化（不变量 I4「计算口径唯一」）。建表遵守开发规范 §4：`delete_flag` + `@TableLogic`、业务表 `status`、活跃唯一走生成列。

## Goals / Non-Goals

**Goals:**

- 经期管理（跨模式通用）：设置 CRUD + 四阶段判定 + 上浮值；男性/未开启空态
- 体重记录（按日幂等覆盖 + 永久留存）+ 平台判定 + 下调/恢复（持久化 + 调碳日志）
- 532 四阶段计划卡 + 今日目标公式化（基线 + 平台下调 + 经期上浮，蛋白脂肪不变）
- `diet/progress` 目标分发改造（532 推进目标、碳循环叠加经期）；「我的」页入口

**Non-Goals:**

- 自动体重曲线 / 全自动复盘（宪法 Never 一期不做）
- 碳循环周期日目标回改（周期日目标创建时固化不回改，D1 口径延续）
- 532 严格自然月状态机（本变更四阶段为展示卡，阶段 4 平台触发式）

## Decisions

### D1 表结构：`user_menstrual` + `weight_record` + `adjust_log`

- `user_menstrual`：`id / user_id / enabled / period_start_date / cycle_len / period_days / status / delete_flag / 审计列`；生成列 `active_user_id = IF(delete_flag=0, user_id, NULL)` + 唯一索引保证每用户一条活跃记录（同 `user_body` / `user_reminders`）
- `weight_record`：`id / user_id / record_date / weight / status / delete_flag / 审计列`；物理上允许同日多条（软删后重录），业务上「同日期幂等覆盖」由服务层先软删当日再插入实现；索引 `(user_id, record_date, delete_flag)`
- `adjust_log`：`id / user_id / action(1下调 2恢复) / trigger_weight / created_at`；仅追加写，软删保历史，无唯一约束（同 `reminder_send_log` 思路）

备选「体重与身体档案合并在 user_body」被否：I4 要求「计算口径唯一 + 数据永久留存」，体重记录需按日多版本留存供平台判定与历史追溯，不能像档案那样单行覆盖。

### D2 计算引擎分离：`MenstrualCalcService` + `Taper532Service`

- `MenstrualCalcService`（无状态计算器）：`resolvePhase(gender, enabled, startDate, L, D, today)` → 阶段枚举 + 上浮值；口径与 `calculator.ts.menstrualPhase` 一致（dayIdx = diff mod L + 1，未来按第 1 天），后端为唯一真源
- `Taper532Service`：四阶段卡（静态结构 + 阶段 2 经期说明注入）、今日目标公式（基线快照 + 下调 + 经期上浮）、平台判定（`isPlateau` 后端化：近 7 天 ≥2 条，max−min < 0.3）

### D3 平台下调状态持久化：读档案扩展 vs 独立字段

下调态与触发参考体重存 `user_body`（新增 `is_adjusted(0/1)` + `trigger_weight`），因为下调目标随档案快照展示且「一档 −20/−80」是全局单值。备选存 `weight_record.join` 派生被否：下发需要触发时刻参考体重，单独持久化更清晰且可追溯；`adjust_log` 只做动作留痕不做状态源。

### D4 今日目标公式（克数 double 存储，1 位小数）

```
今日碳水 = 基线碳水 + (is_adjusted ? −20 : 0) + 经期上浮碳水
今日热量 = 基线热量 + (is_adjusted ? −80 : 0) + 经期上浮热量
今日蛋白/脂肪 = 基线不变
```
上浮与下调均为克数/整 kcal，用 `BigDecimal` 相加后 `setScale(1)` 防漂移；热量取整。

### D5 平台判定与恢复的触发时机

- 下调触发：增/改体重记录后，若未下调态且 `isPlateau` 成立 → 置 `is_adjusted=1`、记 `trigger_weight=当日体重`、写 `adjust_log(下调)`
- 恢复触发：增/改体重记录后，若下调态且 `trigger_weight − 当日体重 ≥ 0.3` → 置 `is_adjusted=0`、清 `trigger_weight`、写 `adjust_log(恢复)`
- 「禁止连续多轮下调」由 `is_adjusted` 单次门闩保证（已下调态跳过再次触发）

### D6 错误码 408xx 与配置

- 40801 `MENSTRUAL_SETTING_INVALID` 经期设置非法（L/D 越界/起始日未来）
- 40802 `WEIGHT_INVALID` 体重记录非法（越界/未来日期）
- 无需新增外部配置；经期/体重均无第三方依赖

### D7 前端：独立页面，复用既有 store/api 分层

- `pages/taper/plan.vue`（P08 四阶段计划卡）、`pages/weight/index.vue`（P09 体重记录 + 平台提示 + 调碳日志 + kg/斤切换）、`pages/menstrual/index.vue`（经期设置）；新增 `api/weight.ts`、`api/menstrual.ts`、`api/taper.ts` 与 `store/weight.ts`（低频，经期/计划走 api 直调不建 store）
- 首页/记录页目标值由后端 `summary` 返回推进口径，前端仅展示，不再本地 `macro532` 叠加（后续与 `calculator.ts` 对拍仅作离线兜底）；首页头卡加经期阶段徽标（仅女性+已开启），532「看计划」改跳 P08
- 「我的」页入口 odShow 同步，经期入口按性别显隐
- 体重单位换算纯前端：后端始终存取 kg，`1 斤 = 0.5kg`，录入/展示按用户当前单位换算

## Risks / Trade-offs

- [体重记录与平台判定随量增大扫描压力] → 判定只取「近 7 天」Top-N 查询（`ORDER BY record_date DESC LIMIT 7`），不做全表；一期体量小
- [经期判定依赖本地日期 diff 的口径（未来按第 1 天）与前端一致] → 后端区间以 `LocalDate.now()` 服务端为准，口径与 `calculator.ts` 对拍
- [下调态存 `user_body` 与「档案修改归档留痕」耦合] → 下调字段不改动档案历史归档语义（`is_adjusted/trigger_weight` 仅随体重复核更新，不触发历史版本归档）
- [经期起始日/周期长度被反复修改导致 dayIdx 跳变] → 判定为幂等推导函数，不落中间快照，天然无一致性风险

## Migration Plan

1. 执行 `sql/change8_taper_532.sql`（建三表 + user_body 加 `is_adjusted`/`trigger_weight` 列）
2. 部署后端（新接口 + summary 分发改造；无存量数据迁移，下调态默认 0）
3. 发布小程序新版本（P08/P09 + 经期设置页 + 我的页入口）

回滚：下线页面入口；`is_adjusted` 默认 0 不影响既有 summary；表保留无副作用。

## Open Questions

- ~~平台「恢复」判定~~ → 已裁决：较触发参考体重下降 ≥0.3kg 即解除，无条件窗口
- ~~经期管理是否首页展示~~ → 已裁决：首页头卡加经期阶段徽标（仅女性+已开启）
- ~~体重单位~~ → 已裁决：kg/斤双单位，后端存 kg、前端换算（1 斤 = 0.5kg）
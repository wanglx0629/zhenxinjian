# Proposal — 碳循环模式（周期计划 + 图片公式日型分配 + 模式切换）

## Why

MVP 双模式计算（532 / 碳循环）目前只有 532 落地，`diet/summary` 的 `mode` 恒为 532，P05 模式选择页形同虚设。碳循环是 PRD 验收第 5 条「图片公式金标验算」的核心考点（当前 57kg→目标 55kg、cfc=0.8、周期 7 天 → 高/中/低碳日 碳水+脂肪 = 241/23、153/49、72/77，蛋白 86g/天），也是小白用户「有变化、能坚持」的关键玩法，必须补齐。

## What Changes

- `user_body` 新增 `mode` 列（1=532 / 2=碳循环，默认 1），档案保存时校验；模式切换走独立接口而非档案保存
- 新建 `carb_cycle_plan`（周期计划：周期天数 7–14、cfc 0.8/1.0、起始日期、状态进行中/已完成/已终止）与 `carb_cycle_day`（每日日型快照：日序、日型高/中/低、三宏与热量目标、运动日标记）两张表
- 后端碳循环计算引擎（唯一真源）：碳水池 = 目标体重×2.5×N、脂肪池 = 目标体重×cfc×N、每日蛋白 = 当前体重×1.5；日型分配 高 50%/2、中 35%/2.2、低 15%/2，周期天数 ≠7 时按权重和归一线性放大，周期总量守恒 ±1g；默认轮播模板 高·中·低·低·中·高·低
- 运动日标记：运动日优先占用高碳位，高碳位占满后占中碳位，不改动非运动日日型
- 模式切换（P16）：切换即终止当前进行中周期、清空当期进度（G4），目标模式为碳循环时引导新建周期；532 侧无周期概念直接生效
- `diet/summary` 按 `mode` 分发目标来源：532 → 档案目标快照（现状不变）；碳循环 → 当日日型目标（蛋白固定 + 碳/脂按日型），无进行中周期时返回空态标记
- 小程序：P05 模式选择页、P06 碳循环周期设置页（天数/cfc/运动日）、P07 碳循环计划页（周期日历 + 今日日型高亮）、P16 切换模式确认弹窗
- 游客迁移补齐：`CarbCyclePlanMigrator` 迁移进行中周期及每日计划，游客过期清理逻辑删除
- **BREAKING**（仅内部接口）：`GET /api/diet/summary` 响应的 `mode` 不再恒为 532，随用户当前模式返回 1/2；无进行中碳循环周期时目标字段为空态

范围外（后续变更）：532 四阶段推进与平台期调碳（P08/P09）、经期管理与四阶段上浮（含碳循环经期叠加）、首页 Dashboard（P02）、提醒（P13）、我的页（P14）。

## Capabilities

### New Capabilities

- `body/cycle-plan`: 碳循环模式全生命周期——模式选择与切换（终止清空）、周期创建（天数/cfc/运动日）与图片公式日型分配、周期计划查询、进行中周期终止；含游客迁移归属

### Modified Capabilities

- `diet/progress`: 「当日实时累计」目标值来源从单一档案快照改为按模式分发（532→档案快照；碳循环→当日日型目标），无进行中周期时目标为空态

## Impact

- **DB**：`sql/change6_carb_cycle.sql`（`user_body` 加 `mode` 列 + 新建 `carb_cycle_plan` / `carb_cycle_day` 两表，均带 `delete_flag`/`status`/审计列）
- **后端**：`BodyProfile` PO/DTO/VO 加 mode；新 `CyclePlanController/Service/Mapper`；`DietRecordService.buildSummary` 按 mode 分发；新 `CarbCyclePlanMigrator`；`ExceptionConstant` 启用 406xx 段位
- **小程序**：`pages/mode/select.vue`（P05）、`pages/cycle/setting.vue`（P06）、`pages/cycle/plan.vue`（P07）、P16 确认弹窗组件；`api/cycle.ts` + `store/cycle.ts`；`pages/record/index.vue` 按 mode 展示对应目标口径
- **验收金标**：当前 57kg / 目标 55kg / cfc=0.8 / 周期 7 天 → 高碳 240.6/23.1、中碳 153.1/49.0、低碳 72.2/77.0（±0.1g）、每日蛋白 85.5g、周期总量守恒 ±1g

# Change 2 — 身体数据录入与核心计算（BMR/TDEE/缺口/宏量）

## Why

登录/游客身份（Change 1）已就绪，但用户进入小程序后没有任何可填的身体档案，「算得准」这一核心能力完全缺失：后端无 `user_body` 表、无任何计算服务，BMR/TDEE/宏量目前只存在于小程序端 `calculator.ts`（其文件头已声明「核心计算以后端为准」）。本变更把核心计算后置到后端作为唯一真源（不变量 I4），打通「录档案 → 出结果」闭环，完成 MVP v1 Phase 1。

## What Changes

**后端（zhenxinjian-backend）**

- 新建 `user_body` 表（迁移脚本走仓库根 `sql/changeN` 手工编号风格）：性别/年龄/身高/体重/目标体重/活动系数/缺口 X/cfc + 计算结果快照（BMR/TDEE/基准热量/目标三宏），必备 `delete_flag`（`@TableLogic`）+ `status`，每用户活跃档案唯一（生成列活跃唯一约束，同 users 表做法）
- 按 ADR-0002 留存历史版本：`user_body_history`（或等效机制），每次修改档案时旧值整体归档，当前值覆盖
- 新增身体档案接口：`PUT /api/body/profile`（录入/修改一体，幂等覆盖）+ `GET /api/body/profile`（查当前档案与计算结果）
- 新增计算服务 `BodyCalcService`：BMR（Mifflin-St Jeor 男女两式）→ TDEE（活动系数 1.2/1.375/1.55/1.725）→ 基准热量 = TDEE − X（X ∈ {200,300,400,500} 默认 200）→ 532 宏量（碳 50%/蛋 30%/脂 20%，4/4/9 折算，克数保留 1 位小数 double 存储）
- 后端兜底校验：年龄 12–80 / 身高 100–250 / 体重 25–200 / 目标体重 25–200 且 ≤ 当前体重（容差 +0.1kg）；超界返回明确业务错误
- 低热量风险提示：基准热量低于 1200（女）/1500（男）时在结果中携带风险标记，不阻断保存
- 游客数据迁移补齐：`GuestDataMigrator`/`UserRecordMigrator` 增加 `user_body` 归属迁移（兑现 Change 1 遗留的「身体数据迁移随 Change 2 补齐」）
- `ExceptionConstant` 新增身体数据文案，业务错误码启用 403xx 段位（401xx 微信 / 402xx 游客已占用）

**小程序（zhenxinjian-uniapp）**

- P03 身体数据页：性别/年龄/身高/体重/目标体重/活动系数 4 档/缺口 X 自选（默认 200），前端先校验（复用 `constants.ts` 的 `RANGES`/`ACTIVITY`/`DEFICIT_OPTIONS`），实时预览计算（`calculator.ts` 离线兜底），提交以后端结果为准
- P04 代谢结果页：BMR/TDEE/基准热量/目标三宏展示 + 低热量风险提示 + 免责声明（不可移除，不变量 I3）
- 新增 `api/body.ts` 封装与 body 相关 store（或扩展 user store），首页 P02 后续读取档案结果（首页展示属后续 change，本变更只落数据通路）
- 首次录入完成后引导进入模式选择（P05 属后续 change，本变更仅预留入口）

## Capabilities

### New Capabilities

- `body/profile`: 身体数据档案——录入/修改/查询当前档案，前后端双重区间校验（后端兜底拒绝），每用户唯一活跃档案，修改留历史版本
- `body/core-calc`: 核心计算——BMR（Mifflin-St Jeor）/TDEE（4 档活动系数）/基准热量（TDEE − X，X 四档默认 200）/532 宏量（50/30/20，4/4/9，1 位小数）以后端为唯一真源；低热量风险标记；结果页内置免责声明

### Modified Capabilities

（无——`auth/guest-mode` 的「业务数据登录后自动迁移」为既有需求，本次仅补齐 `user_body` 的迁移实现，无需求变更）

## Impact

- **依赖**：无新增后端 maven / 前端 npm 依赖
- **代码**：backend 新增 `controller/BodyController`、`service/BodyProfileService` + `BodyCalcService`、`mapper/UserBodyMapper(+History)`、`domain/po|dto|vo` 若干、`common/enums` 新增性别/活动系数/缺口档位字典枚举；uniapp 新增 `pages/body/profile.vue`、`pages/body/result.vue`、`api/body.ts`，`pages.json` 注册两页
- **数据**：`sql/` 新增 `user_body` + 历史表迁移脚本；`users` 表不变
- **口径真源**：`docsFile/projectFile/02-业务规则与公式速查.md`（48 项对拍口径）；小程序 `calculator.ts` 降级为展示预览与离线兜底
- **前置**：依赖 Change 1（登录/游客身份）已归档；首页 Dashboard、模式选择、食物库等后续 change 依赖本变更的档案与计算结果

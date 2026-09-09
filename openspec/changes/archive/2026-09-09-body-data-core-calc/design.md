# Design — 身体数据录入与核心计算

## Context

现状（调研确认）：后端无 `user_body` 相关表/实体/接口，计算逻辑仅存在于小程序 `src/utils/calculator.ts`（48 项对拍口径，文件头已声明「核心计算以后端为准」）。既有约定：迁移脚本走仓库根 `sql/changeN_*.sql` 手工编号（已用到 change2）；业务错误码段位 401xx（微信）/402xx（游客）已占用；身份取用 `UserContext.getUserId()`；游客迁移框架 `GuestDataMigrator`/`UserRecordMigrator` 已就绪（Change 1 遗留 user_body 迁移待补）。ADR-0002 已定：`user_body` 保存当前档案，修改时覆盖当前值并留存历史版本。动机见 proposal.md。

## Goals / Non-Goals

**Goals:**

- `user_body`（当前档案，每用户活跃唯一）+ `user_body_history`（历史版本）两表及迁移脚本
- `PUT /api/body/profile`（录入/修改一体）+ `GET /api/body/profile`（档案+结果快照/空态）
- 后端 `BodyCalcService`：BMR/TDEE/基准热量/532 宏量，与前端 48 项对拍口径一致
- 游客档案迁移补齐（含历史表归属变更）
- 小程序 P03 录入页 + P04 结果页（含免责声明）

**Non-Goals:**

- 首页 Dashboard 展示档案/进度（后续 change）
- 模式选择（碳循环/532 计划生成）、cfc 切换交互、体重记录与调碳（后续 change；`cfc` 列本变更预建、默认 0.8）
- 管理后台身体数据管理页（一期边界未确认）
- 历史版本的小程序端查询界面（接口留存，界面后续）

## Decisions

### D1 当前档案 + 独立历史表，而非单表多版本

`user_body` 每用户仅一条活跃记录（修改即 UPDATE），旧值整体 INSERT 进 `user_body_history`。备选：单表多版本（每次修改插新行、旧行状态失效）。选前者：与 users 表「生成列活跃唯一」模式一致、当前档案查询无版本过滤开销、唯一约束简单（`user_id_active` 生成列）；历史表追加写天然满足 ADR-0002 留痕。

### D2 保存时同步计算并持久化结果快照，而非查询时实时算

`user_body` 冗余存储 BMR/TDEE/基准热量/目标三宏（double）。理由：后续模式计划（碳循环/532）与首页需引用「承诺值」快照，落库保证周期内口径冻结；且 ADR-0002 明确 user_body 含 BMR/TDEE/缺口字段。保存与快照更新同一事务，避免档案与结果不一致。

### D3 取整口径与前端对拍对齐

热量类（BMR/TDEE/基准热量）四舍五入取整 kcal；宏量克数保留 1 位小数 double 存储、UI 展示整数克。验算基准（速查文档定稿）：女 30/162/55/1.375/缺口 200 → BMR 1252、TDEE 1721、基准 1521、碳 190.1/蛋 114.1/脂 33.8。后端单测直接移植前端 48 项对拍断言中的核心用例，防口径漂移。

### D4 业务错误码启用 403xx 段位

401xx 微信 / 402xx 游客已占用，身体数据段位 403xx（如 40301 目标体重超限、40302 缺口档位非法）。文案入 `ExceptionConstant`，参数校验失败走既有 `GlobalExceptionHandler` 的 `PARAM_VALID_FAIL` 通路；交叉校验（目标体重 ≤ 当前体重 +0.1）在 Service 层抛 `BusinessException`。

### D5 性别以 user_body 自有列为准，不回写 users.gender

users.gender 属账号资料（默认 0 未知），游客不会维护；业务计算以 `user_body.gender` 为口径。按建表规约新建 `GenderEnum`/`ActivityLevelEnum`/`DeficitOptionEnum`（`common/enums/`，code+desc+of）。

### D6 前端预览与后端真源分工

P03 录入页用 `calculator.ts` + `constants.ts`（RANGES/ACTIVITY/DEFICIT_OPTIONS 已存在）做提交前校验与实时预览；提交后一律以后端返回快照渲染 P04。离线/接口失败时 calculator.ts 兜底展示并标注非最终结果。`calculator.ts` 不删除（后续 532/碳循环计划页仍用其预览）。

### D7 游客迁移：归属改 user_id，不复制行

`UserRecordMigrator` 增加 `user_body`、`user_body_history` 两表 `user_id` 归属变更（UPDATE ... WHERE user_id = 游客ID），幂等（重复执行影响 0 行）。正式用户已有活跃档案时，游客活跃档案降级并入历史表，不覆盖正式用户当前值（兑现 spec 迁移冲突场景）。

## Risks / Trade-offs

- [Java 与 TS 浮点/取整实现差异导致口径漂移] → 后端单测移植对拍验算基准（D3），差异 ±1kcal/±0.1g 内断言失败即阻断
- [结果快照与档案同事务写入失败留下不一致] → 单事务包裹；保存接口幂等可重试，重试整体覆盖
- [历史表长期膨胀] → 一期单人年修改频次极低（<百行），接受；后续可加归档任务
- [游客并发保存与唯一约束冲突] → 生成列活跃唯一兜底，catch `DuplicateKeyException` 转为覆盖重试，不抛 500

## Migration Plan

1. 新增 `sql/change3_user_body.sql`：`user_body` + `user_body_history` 建表（均含 `delete_flag`、`status`、审计列；`user_body` 加 `user_id_active` 生成列 + 活跃唯一索引），与 change1/2 一样手工执行
2. 部署顺序：先执行 SQL，再发后端，再发小程序（接口向后兼容，无旧客户端依赖）
3. 回滚：下线接口与页面 → `DROP TABLE user_body_history, user_body`；无存量数据迁移，风险低

## Open Questions

（无）

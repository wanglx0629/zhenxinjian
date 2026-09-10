# Design — 饮食记录（增删改 + 当日累计 + 三色进度 + 微调建议）

## Context

后端软删 + 枚举字典 + `Result`/`ExceptionConstant` 样板已在 Change 2/3/4 固化；游客迁移装配点 `GuestDataMigrator`（`migrate(guestId, formalId)` / `purge(guestId)`，登录流程与定时清理自动收集所有实现 Bean）可直接扩展。目标值来源 `user_body` 活跃档案已含目标三宏快照与基准热量（Change 2）。食物快照源 `foods` 表每 100g 值 `DECIMAL(5,1)`（Change 4，D2）。目标见 proposal「Why」，行为契约见 specs 两份 delta。

## Goals / Non-Goals

**Goals:**

- `diet_records` 单表承载三类来源（内置食物/自定义食物/手动输入），按日按餐别 CRUD 一套通路
- 食物库来源记录冗余快照，编辑按快照重算，与食物表后续变更解耦
- 当日累计 SQL 聚合，对照 `user_body` 目标快照给出达成率
- 游客记录迁移/清理接入既有 `GuestDataMigrator` 装配点

**Non-Goals:**

- 首页 P02 看板完整形态（后续「首页 Dashboard」change，本变更仅交付 record 页内进度与建议 + summary 接口）
- 碳循环按日型动态目标（后续碳循环 change；本期目标恒取 `user_body` 532 基准快照）
- 提醒联动「已记录不重复推送」（I12，随提醒 change 落地；本变更接口已可按日查记录数，不预留额外口径）
- 单位换算（个/碗/勺）、误删恢复（二期）

## Decisions

**D1：单表 `diet_records`，来源枚举区分，快照列冗余。**

```sql
user_id BIGINT NOT NULL, record_date DATE NOT NULL, meal_type TINYINT NOT NULL,  -- 1早2午3晚4加餐
source TINYINT NOT NULL,            -- 1内置食物 2自定义食物 3手动输入
food_id BIGINT NULL,                -- 手动输入为 NULL；食物软删后不回查
food_name VARCHAR(64) NOT NULL,     -- 快照：提交时刻名称（手动输入即用户填写名）
carb_100g/protein_100g/fat_100g DECIMAL(5,1) NULL, kcal_100g INT NULL,  -- 快照：每 100g 值
amount_g DOUBLE NOT NULL,           -- 食物来源=克数；手动输入=1（占位，实际值直接见摄入列）
carb_g/protein_g/fat_g DOUBLE NOT NULL, kcal INT NOT NULL,              -- 实际摄入
remark VARCHAR(100) NULL,
delete_flag + status + 审计列（通则）,
KEY idx_user_date (user_id, record_date, delete_flag)
```

快照列对食物来源 NOT NULL、手动输入 NULL；`food_id` 仅作溯源参考，查询展示一律走快照。分「食物记录表 + 手动记录表」两案被否：累计/分组需 UNION，无收益。

**D2：摄入克数 `DOUBLE`，每 100g 快照 `DECIMAL(5,1)`。**

遵循通则「DOUBLE 存克数」（摄入值随记录累计）；快照口径与 `foods` 表一致（Change 4 D2）。换算在 Service 层以 `BigDecimal` 计算后转 double 入库：克数 × 每 100g 值 ÷ 100，克数保留 1 位小数、热量取整（与 Change 2 计算口径对齐）。

**D3：编辑食物来源记录按快照重算，不回查食物表。**

编辑只允许改餐别/份量/备注（不允许换食物——换食物 = 删旧增新，避免快照语义混乱）；份量变更按记录内快照每 100g 值重算摄入，食物被改/删不影响（I8）。手动输入记录可改名称与三宏热量，仍过 ±10% 守恒校验。

**D4：累计为 SQL `SUM` 聚合，不在内存汇总。**

`SELECT meal_type, SUM(carb_g), SUM(protein_g), SUM(fat_g), SUM(kcal) FROM diet_records WHERE user_id=? AND record_date=? AND delete_flag=0 GROUP BY meal_type`；单日记录量级数十条，聚合成本可忽略。目标值取 `user_body` 活跃档案快照（`user_id_active` 生成列那条），未建档返回 `recorded=false` 空态（同 Change 2 档案空态模式）。

**D5：错误码 405xx 段位。**

40501 份量非法、40502 宏量区间非法、40503 能量不守恒、40504 记录不存在/不可操作（越权同码不泄露存在性）、40505 未来日期、40506 食物不存在或已删除（下单时食物校验）。文案入 `ExceptionConstant`，段位顺延 401–404。

**D6：游客迁移——`DietRecordMigrator implements GuestDataMigrator`，纯 UPDATE 改归属。**

饮食记录无跨用户唯一约束（不同于档案活跃唯一），`migrate` 即 `UPDATE diet_records SET user_id=formalId WHERE user_id=guestId AND delete_flag=0`（幂等：重复执行影响行数为 0）；`purge` 为按 `guestId` 逻辑删除。注册为 Bean 即被既有登录流程/清理任务自动装配，无需改登录代码。

**D7：小程序数据通路——`api/diet.ts` + diet store，record 页承载进度与建议。**

- `pages/record/add.vue`：餐别段选默认按本地时段（spec 口径），食物经路由参数从 P10 详情带入（`foodId` + 克数），手动输入为同页 Tab/模式切换；提交后 `reLaunch` 回记录页
- `pages/record/index.vue`：日期导航（今日为锚，右箭超过今日禁用）+ 餐别分组列表 + 进度区；`onShow` 重拉记录与 summary 保证实时
- 三色/超标量/建议文案为纯前端渲染逻辑（数据已由 summary 接口给齐：实际、目标、达成率），建议文案常量入 `constants.ts`，与原型口径一致

**D8：目标值不区分减脂模式。**

本期 `user_body` 快照即 532 基准目标；碳循环落地后由该 change 扩展 summary 接口（按日型返回当日目标），本变更接口出参结构预留 `mode` 字段（恒 `532`）避免二次 Breaking。

## Risks / Trade-offs

- [快照冗余导致食物改名不同步] → 有意为之（I8 历史完整）；记录展示即「当时吃了什么」，符合直觉
- [DOUBLE 累计浮点漂移] → 摄入入库前 BigDecimal 定精度；SUM 后展示保留 1 位小数，达成率按 double 比较阈值（80/100%）误差可忽略
- [并发提交同日记录] → 无唯一约束冲突，天然安全；删除/编辑按 id + user_id 双条件，越权不落
- [游客迁移与正式用户记录混合] → 饮食记录无冲突场景，直接改归属（D6）；不重复 Change 2 的降级归档复杂度
- [未来日期仅服务端拦截] → 前端禁用右箭为体验层，服务端 40505 为兜底；时钟差导致「今日」不一致时以服务端日期为准

## Migration Plan

1. 执行 `sql/change5_diet_records.sql` 建表（dev 库手工执行，同前四次变更流程）
2. 部署后端（新表无存量数据，无数据迁移）；确认 `DietRecordMigrator` 被登录流程装配（`getBeansOfType(GuestDataMigrator)` 自动收集）
3. 小程序发版：P10 详情「加入记录」→ P11 → P12 链路可用；旧占位页被替换
4. 回滚：单表独立无外键，`DROP TABLE diet_records` + 回退代码即完全回退（仅 dev 环境；生产尚未上线）

## Open Questions

（无——餐别时段划分、建议文案、目标口径均已在 proposal/spec 定稿；首页看板复用 summary 的字段需求在 Dashboard change 再定，不影响本接口向后兼容扩展）

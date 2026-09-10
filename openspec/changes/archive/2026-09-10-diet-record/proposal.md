# Change 5 — 饮食记录（增删改 + 当日累计 + 三色进度 + 微调建议）

## Why

登录/游客（Change 1）、身体档案/核心计算（Change 2）与食物库（Change 4）已就绪，但「记得快」闭环仍缺失最后一环：小程序记录页（P12）与添加饮食页（P11）均为占位页，后端无 `diet_records` 表、无记录接口，用户无法把食物库里的食物落成当日饮食，首页（P02）宏量进度与超标预警（F09/F26）也无数据可依。本变更建立后端权威饮食记录并打通「选食物/手动输入 → 记录 → 当日累计 → 进度与超标建议」闭环，兑现 MVP v1 Phase 2 的饮食记录部分（原型 P11/P12，首页看板增强为后续 change 复用本接口数据）。

## What Changes

**后端（zhenxinjian-backend）**

- 新建 `diet_records` 表（迁移脚本 `sql/change5_diet_records.sql`，沿用仓库根手工编号风格）：用户 + 日期 + 餐别 + 来源（食物库/自定义食物/手动输入）+ 食物快照（名称/每 100g 三宏与热量冗余存储，不变量 I8）+ 份量克数 + 实际摄入三宏与热量（double 存克数）+ 备注；必备 `delete_flag`（`@TableLogic`）+ `status`；`(user_id, record_date)` 查询索引
- 记录接口：新增（选食物按份量换算落快照 / 手动输入保底直接填三宏热量）/ 编辑（仅当日及历史本人记录）/ 删除（软删，二次确认由前端负责）/ 按日查询列表（按餐别分组）
- 当日累计接口：`GET /api/diet/summary?date=` 返回当日已摄入合计（三宏 + 热量）与目标值（取 `user_body` 当前活跃档案的目标三宏快照与基准热量），及各项达成率；未录入身体档案返回空态标记（前端隐藏进度条引导先录档案）
- 后端兜底校验：份量克数 > 0 且有上限、手动输入宏量区间合法 + 能量守恒 ±10%（同 I11）、仅可操作本人记录（I9）、食物快照以提交时刻食物库值为准（食物后续改/删不影响历史记录）
- `ExceptionConstant` 新增饮食记录文案；业务错误码启用 405xx 段位（401xx 微信 / 402xx 游客 / 403xx 身体 / 404xx 食物库已占用）
- 新建字典枚举：`MealTypeEnum`（1 早餐 2 午餐 3 晚餐 4 加餐）、`DietRecordSourceEnum`（1 内置食物 2 自定义食物 3 手动输入）
- 游客迁移补齐：新建 `DietRecordMigrator implements GuestDataMigrator`（沿用 Change 2 `BodyDataMigrator` 装配模式），登录时游客 `diet_records` 改归属正式用户（幂等 UPDATE），过期清理任务同步逻辑删除

**小程序（zhenxinjian-uniapp）**

- P11 添加饮食页（新建 `pages/record/add.vue`）：餐别段选（早/午/晚/加餐）+ 智能推荐（按当前时段默认选中，可改）；食物来源两路——从食物库带入（P10 详情试算后「加入记录」入口，克数可改）或手动输入保底（名称 + 三宏 + 热量，前端先做区间与 ±10% 守恒校验，提交以后端为准）；实时换算预览（每 100g 值 × 克数 ÷ 100）
- P12 当日记录页（替换 `pages/record/index.vue` 占位）：日期切换（今日/前后翻日，未来日期禁入）+ 按餐别分组列表 + 增删改入口（删除二次确认）+ 当日实时累计
- 进度条三色（F09/F26）：三宏 + 总热量四项进度条，达成率 80–100% 绿 / <80% 黄 / >100% 红并标「已超标 XXg/XXkcal」；超标时展示微调建议卡（按超标项动态生成，文案口径取自高保真原型：脂肪超标→换清蒸水煮做法；碳水超标→主食减半/换糙米红薯；蛋白略超→提示无大碍压回脂碳；总热量超→散步 20–30 分钟 + 不跳餐；附「非医疗建议」免责注）
- 新增 `src/api/diet.ts` 与 diet store；P10 食物详情增加「加入记录」跳转 P11（带入食物与克数）

**口径假设（记录在案）**

- 智能推荐餐别 = 按本地当前时段默认选中（05:00–10:00 早餐 / 10:00–15:00 午餐 / 15:00–20:30 晚餐 / 其余加餐），仅作默认值，用户可自由改选；PRD 未给出更细的推荐算法
- 进度目标值取 `user_body` 目标三宏快照（532 基准口径）；碳循环模式按日型动态目标属后续碳循环 change 范围，本变更不区分模式
- 首页 P02 看板（F09/F26 完整形态）在后续「首页 Dashboard」change 中实现；本变更交付 P12 记录页内的进度与超标建议，并暴露 summary 接口供首页复用
- 单位换算「个/碗/勺」沿用 Change 4 结论：本期仅克数；备注为可选纯文本（上限 100 字）

## Capabilities

### New Capabilities

- `diet/record`: 饮食记录——按日按餐别增删改查；食物库带入（快照冗余）或手动输入保底；份量克数换算实际摄入；软删保历史；本人数据隔离；游客记录随登录迁移归属
- `diet/progress`: 当日累计与进度——当日三宏+热量实时合计、对照 `user_body` 目标快照的达成率；三色进度（80–100% 绿 / <80% 黄 / >100% 红 + 已超标量）；超标微调建议文案；未建档空态

### Modified Capabilities

（无——`food/library` 仅新增「加入记录」入口跳转，不改食物库自身需求；`auth/guest-mode` 迁移框架不变，仅新增一个 migrator 实现接入既有装配点）

## Impact

- **依赖**：无新增后端 maven / 前端 npm 依赖
- **代码**：backend 新增 `controller/DietController`、`service/DietRecordService`（+ 累计汇总）、`mapper/DietRecordMapper`、`domain/po|dto|vo` 若干、`common/enums` 新增餐别/来源枚举、`service/migration/DietRecordMigrator`；uniapp 新建 `pages/record/add.vue`、重写 `pages/record/index.vue`、新增 `api/diet.ts` + diet store，改动 `pages/food` 详情加入口
- **数据**：`sql/` 新增 `change5_diet_records.sql`（`diet_records` 表）；既有表不变
- **口径真源**：MVP v1 §饮食记录 / §验收；高保真原型 P11/P12（超标建议文案）；不变量 I1（永久留存）/ I8（快照冗余）/ I9（数据归属）/ I11（能量守恒 ±10%）
- **前置**：依赖 Change 1/2/4 已归档；后续「首页 Dashboard」（P02）与「提醒」（已记录当日不再推送，I12）依赖本变更的记录与累计接口

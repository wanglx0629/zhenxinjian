# Design — 碳循环模式（周期计划 + 图片公式 + 模式切换）

> 作者: wanglx

## 1. 设计目标与边界

落地 PRD 碳循环模式（图片公式定稿口径）：周期创建 → 三大池计算 → 日型分配 → 计划查询 → 模式切换终止。后端为唯一计算真源（G1），前端仅展示。532 四阶段/经期/平台期调碳、首页 Dashboard、提醒均不在本变更内。

## 2. 数据模型（`sql/change6_carb_cycle.sql`）

### 2.1 `user_body` 加列

| 列 | 类型 | 说明 |
| -- | ---- | ---- |
| `mode` | TINYINT NOT NULL DEFAULT 1 | 减脂模式：1=532 / 2=碳循环 |

存量行默认 1，无需回填。

### 2.2 `carb_cycle_plan`（周期计划）

| 列 | 类型 | 说明 |
| -- | ---- | ---- |
| `id` | BIGINT PK AI | |
| `user_id` | BIGINT NOT NULL | 归属用户 |
| `cycle_days` | INT NOT NULL | 周期天数 7–14 |
| `cfc` | DECIMAL(2,1) NOT NULL DEFAULT 0.8 | 脂肪系数 0.8/1.0 |
| `start_date` | DATE NOT NULL | 起始日期（创建当日） |
| `end_date` | DATE NOT NULL | 结束日期 = start + N − 1 |
| `weight_snapshot` | DECIMAL(5,1) NOT NULL | 创建时当前体重快照 |
| `target_weight_snapshot` | DECIMAL(5,1) NOT NULL | 创建时目标体重快照 |
| `carb_pool` | DECIMAL(7,1) NOT NULL | 碳水池 g |
| `fat_pool` | DECIMAL(7,1) NOT NULL | 脂肪池 g |
| `daily_protein` | DECIMAL(5,1) NOT NULL | 每日蛋白 g |
| `status` | TINYINT NOT NULL DEFAULT 1 | 1进行中 2已完成 3已终止 |
| `delete_flag` + 审计列 | | 规约标配 |

索引：`idx_user_status(user_id, status, delete_flag)`。进行中唯一性由服务层保证（新建先终止旧），不建唯一索引（软删表规约）。

### 2.3 `carb_cycle_day`（每日日型计划）

| 列 | 类型 | 说明 |
| -- | ---- | ---- |
| `id` | BIGINT PK AI | |
| `plan_id` | BIGINT NOT NULL | 所属周期 |
| `user_id` | BIGINT NOT NULL | 冗余归属（迁移/清理直改） |
| `day_index` | INT NOT NULL | 日序 1..N |
| `day_date` | DATE NOT NULL | 日历日 |
| `day_type` | TINYINT NOT NULL | 1高碳 2中碳 3低碳 |
| `is_sport` | TINYINT NOT NULL DEFAULT 0 | 运动日 |
| `carb_g` / `protein_g` / `fat_g` | DECIMAL(5,1) NOT NULL | 当日三宏目标 |
| `kcal` | INT NOT NULL | 当日热量目标（4/4/9 取整） |
| `status` + `delete_flag` + 审计列 | | 规约标配 |

索引：`idx_plan(plan_id, delete_flag)`、`idx_user_date(user_id, day_date, delete_flag)`。

字典枚举（`common/enums/`）：`DietModeEnum`（1/2）、`CycleDayTypeEnum`（1/2/3）、`CyclePlanStatusEnum`（1/2/3），均 code+desc+of。

## 3. 计算引擎（`CycleCalcService`，唯一真源）

### 3.1 三大池

```
carbPool    = targetWeight × 2.5 × N          // 目标体重！
fatPool     = targetWeight × cfc × N
dailyProtein= currentWeight × 1.5             // 当前体重！
```

BigDecimal 全程，精度 10 位；落库 setScale(1, HALF_UP)。

### 3.2 日型分配（除数 = 权重 × N/7）

日型占比与权重：高（碳50%/脂15%，w=2.0）、中（碳35%/脂35%，w=2.2）、低（碳15%/脂50%，w=2.0）。

```
高碳日碳水 = carbPool × 50% ÷ (2.0 × N/7)
中碳日碳水 = carbPool × 35% ÷ (2.2 × N/7)
低碳日碳水 = carbPool × 15% ÷ (2.0 × N/7)
（脂肪同法：高 15%、中 35%、低 50%，除数同为权重 × N/7）
```

N=7 时退化为 PRD 原式（50%÷2.0 等），金标逐位命中；除数随 N 线性放大 ⇒ 同一日型单日目标与 N 无关（营养学一致），N=14 时池翻倍、日数翻倍、单日不变。

**合计守恒**：模板 7 日结构使各日合计 = 池 × K（K碳水 = 2×0.5/2 + 2×0.35/2.2 + 3×0.15/2 ≈ 1.0432；K脂肪 = 2×0.15/2 + 2×0.35/2.2 + 3×0.5/2 ≈ 1.2182）。逐日 setScale(1) 的累计漂移 ≤ 0.7g（14 日 × 0.05g），天然落在 ±1g 容差内，无需调尾。热量 = 当日克数（四舍五入后）碳×4+蛋×4+脂×9，setScale(0, HALF_UP)。

### 3.3 日型排布与运动日

1. 默认模板 `[H,M,L,L,M,H,L]` 循环取 N 天得基础序列
2. 运动日集合 S（按 dayIndex）：依次重排为剩余最高优先级日型（H 位用尽→M 位），其余位置按基础序列剔除已用日型后保持相对顺序填充
3. 约束：|S| ≤ N；运动日 dayIndex ∈ [1, N]，越界 40604

算法等价描述：运动日按序抢占模板中 H 位（再 M 位）；非运动日按原模板顺序填入剩余日型。

### 3.4 金标对拍

57/55/0.8/7 无运动日 → 池 962.5/308/85.5；单日 H 240.6/23.1、M 153.1/49.0、L 72.2/77.0（±0.1g）；合计守恒 ±1g。单测断言逐位对拍（与 `calculator.ts` 及速查表 §9 一致）。

## 4. 接口设计（`CyclePlanController`，返回 `Result`，注释含「作者: wanglx」）

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/api/cycle/plans` | 创建周期（入参 cycleDays/cfc/sportDays[]；先终止进行中旧周期；档案未建档 40601） |
| GET | `/api/cycle/plans/current` | 进行中周期 + 逐日计划 + 今日定位；无则空态 `plan=null` |
| GET | `/api/cycle/plans/{id}` | 历史周期详情（本人校验，越权 40605） |
| POST | `/api/cycle/plans/current/terminate` | 手动终止当前周期 |
| PUT | `/api/body/mode` | 切换模式（入参 mode；切出碳循环自动终止进行中周期 G4；mode 非法 40602） |

`ExceptionConstant` 启用 406xx：40601 未建档 / 40602 模式或参数非法 / 40603 周期不存在 / 40604 运动日越界或过多 / 40605 越权或不可操作。

### 4.1 `diet/summary` 分发改造

`DietRecordService.buildSummary`：读 `user_body.mode` →
- mode=1：现状（档案快照）
- mode=2：查 `carb_cycle_day`（user_id + day_date + delete_flag）命中进行中周期的当日行 → 目标=该行三宏/kcal；未命中 → `noProfile=true` 同空态口径（VO 已有该字段，复用）
- VO `mode` 字段返回实际值（不再恒 1）

## 5. 游客迁移（`CarbCyclePlanMigrator implements GuestDataMigrator`）

- `migrate`：两表 `user_id` 幂等 UPDATE；冲突处理——正式用户已有进行中周期时，迁入的进行中周期置为已终止（spec「至多一个进行中」）
- `purge`：两表按游客 user_id 逻辑删除
- 注册 Bean 由登录/清理任务自动装配（同 BodyData/DietRecord 模式）

## 6. 小程序设计

| 页面 | 路径 | 要点 |
| ---- | ---- | ---- |
| P05 模式选择 | `pages/mode/select.vue` | 双卡片（532/碳循环）+ 当前模式标记；点碳循环且无周期 → 先 `PUT /api/body/mode` 再跳 P06；有进行中周期直接跳 P07 |
| P06 周期设置 | `pages/cycle/setting.vue` | 天数步进 7–14、cfc 段选 0.8/1.0（回填上次值）、运动日星期多选（≤ 周期天数）；提交 → `POST /api/cycle/plans` → 跳 P07 |
| P07 周期计划 | `pages/cycle/plan.vue` | 逐日卡片列表（日序/日期/日型色块 H 红 M 蓝 L 灰/三宏+kcal/运动标）+ 今日高亮吸顶 + 池总量头卡；空态引导 P06 |
| P16 切换确认 | 组件 `ModeSwitchConfirm` | 文案「切换将终止当前碳循环周期并清空进度」；确认 → `PUT /api/body/mode` |

数据通路：`api/cycle.ts` + `store/cycle.ts`（currentPlan/days/todayIndex/creating）；`record/index.vue` 与 `diet store` 无需改——summary 已按 mode 分发，前端仅按 `mode` 字段展示对应文案（532「今日目标」/ 碳循环「今日高/中/低碳日」标签）。

## 7. 关键决策记录

- **D1 目标快照口径**：周期每日目标创建时固化为行数据（不引用档案动态值），档案变更不回改——与 diet_records 快照同哲学，避免历史失真
- **D2 运动中唯一约束走服务层**：软删表不建唯一索引（规约），新建/迁移双入口均先终止旧周期
- **D3 空态复用 `noProfile`**：碳循环无周期与未建档共用空态标记字段，前端一套空态组件两处复用，不新增错误码
- **D4 守恒免调尾**：逐日四舍五入累计漂移 ≤0.7g，天然落在 ±1g 容差内，不做末日调整——避免金标低碳日 72.2 被调成 72.1，保证金标逐位可断言
- **D5 mode 列放 `user_body` 而非独立表**：模式是档案级单值属性，随活跃档案快照天然带「活跃唯一」语义，无需新表

## 8. 测试策略

- 单测：图片公式金标（7 天/14 天/cfc=1.0 三组）、守恒 ±1g、运动日抢占（含 H 位占满降级 M）、模板循环 N≠7、切换终止、越权、迁移幂等与冲突、purge
- 接口级：创建→查询→summary 分发→切换→再分发全链路；无周期空态；未来日期不在本变更范围（diet-record 已覆盖）
- 金标验收：PRD 验收第 5 条逐位对拍 + 前端 `calculator.ts` 同口径联查（仅展示兜底）

# 食物库分类全量切换 TFDA 18 大类 — Design

## Context

见 proposal.md（Why：分类颗粒度与来源口径对齐）。当前状态与关键事实：

- `FoodCategoryEnum`（01–10）是分类唯一字典源：`/api/foods/categories` 直接吐 `values()`，小程序与管理后台字典均以它为准；`foods.category_code/category_name` 存量约 1930 条（内置 200 + TFDA 1730，共建存量仅 8 条软删测试数据）。
- TFDA raw 数据（`data-import/tfda/raw/data_*.json` × 18）**每条自带 `category` 字段**（中文类名，如「蔬菜類」），18 类归属无需从文件名或规则推断；`tfda_clean.py` 目前将其经 `CATEGORY_MAP`+条件规则压映射到 01–10 后丢弃原始归属。
- 已入库 TFDA 条目保留分析编号（`foods.code` 如 `E7400301`），可与 raw 按编号精确关联回填。
- 内置 200 按 10 类分布：01:30 / 02:25 / 03:14 / 04:20 / 05:14 / 06:38 / 07:9 / 08:26 / 09:10 / 10:14。
- 小程序食物库已是「顶部横滑分类标签条 + 全部」形态（`pages/food/index.vue` scroll-x + cat-item），与市面主流一致，本 change 不改形态、只换字典与配色；投稿表单为 picker 选择（`custom-edit.vue`，默认 `categoryCode='10'`）。
- `FoodThumb.vue` 内置 10 分类色块字典；管理后台 `constants/dicts.ts` 硬编码 `FOOD_CATEGORY_OPTIONS`（01–10）；小程序离线兜底数据 `src/data/foods.ts` 为内置 200 副本（含 10 类 category 值）。
- 流程约束：`food/import` 能力由未归档的 `food-curation-part2` 引入，本 change 的 import delta MODIFIED 其 requirement——**实施与归档必须在 part2 归档之后**（见 proposal Impact）。

## Goals / Non-Goals

**Goals：**

- 分类字典切换为 TFDA 原始 18 大类（编码 01–18、繁转简类名），全库食物分类值与字典一致。
- TFDA 导入管线不再压映射，保留原始 18 类；已入库条目按分析编号幂等回填。
- 内置 200（离线兜底副本同步）按规则+人工裁决归类到 18 类。
- 三端（小程序/管理后台/后端）字典、筛选、投稿选择、占位色块全部 18 化；筛选交互维持市面常用的横滑标签条形态。
- 存量刷数走 change16 SQL，幂等、可回滚、可重放部署。

**Non-Goals：**

- 不重命名/美化 TFDA 类目（保持官方口径，仅繁转简，「糕饼点心类」「加工调理食品及其他类」原样保留）。
- 不引入二级分类/双分类体系（10 类彻底退役）。
- 不改动营养口径、搜索/分页/审核/共建状态机等与分类无关的行为。
- 不重做食物库页面整体布局（仅分类条 18 项与色块）。

## Decisions

### D1 分类编码与顺序：01–18 顺延，按「主食→蛋白→蔬果→加工」产品习惯排列

raw 数据无官方字母码，仅中文类名。系统编码用 `01`–`18`（VARCHAR 兼容、与旧 01–10 结构一致），顺序按饮食记录产品常用习惯（主食优先、复合加工殿后），全部 18 类：

| 编码 | 类名（繁转简） | raw 条数 |
| ---- | ---- | ---- |
| 01 | 淀粉类 | 48 |
| 02 | 谷物类 | 81 |
| 03 | 肉类 | 157 |
| 04 | 鱼贝类 | 331 |
| 05 | 蛋类 | 71 |
| 06 | 乳品类 | 83 |
| 07 | 豆类 | 41 |
| 08 | 蔬菜类 | 300 |
| 09 | 菇类 | 56 |
| 10 | 藻类 | 21 |
| 11 | 水果类 | 242 |
| 12 | 坚果及种子类 | 39 |
| 13 | 油脂类 | 43 |
| 14 | 糖类 | 12 |
| 15 | 糕饼点心类 | 116 |
| 16 | 调味料及香辛料类 | 151 |
| 17 | 饮料类 | 72 |
| 18 | 加工调理食品及其他类 | 316 |

备选：按台湾官方 A–R 字母序——弃用，因 raw 无字母码且官方序对大陆用户无感知价值。顺序仅影响展示（枚举声明顺序+字典），后续调整成本极低。

### D2 18 类归属来源：raw `category` 字段直读，不做规则推断

`tfda_clean.py` 新增 `CATEGORY_18 = {'澱粉類': '01', …}` 固定映射，清洗时 `to_simplified(food['category'])` → 系统编码，替代现 `CATEGORY_MAP`/`BEAN_SOY`/`PROCESSED_RULES` 整套压映射逻辑；豆类不再拆分、加工调理类不再按关键词打散，官方口径原样保留（营养校验/去重/繁转简逻辑不变）。

### D3 内置 200 归类：脚本规则映射 + 人工裁决清单

新增一次性脚本 `python/builtin_recat.py`：按「旧 10 类 → 候选 18 类子集」先验 + 名称关键词规则归类（如 03 拆蛋/乳：名含「蛋」→05、含「奶/乳/酪/酸奶」→06；07 拆菇/藻；10 拆油/糖/调味/饮料；01 拆淀粉/谷物：名含「土豆/红薯/山药/芋/粉」→01 淀粉，其余→02 谷物），关键词边界用 raw 各类实际成员名单校准（如蜂蜜在糖类、南瓜归属）。无把握条目输出 `data-import/builtin_recat/manual_review.json` 由人工逐条定（预计 ≤30 条），裁决后回填。产物：更新 `foods_200.json` 的 category 字段 + 归类报告。

备选：全部 200 条人工归类——弃用，规则可自动覆盖约 85%+，人工只兜边界。

### D4 存量刷数：change16 用映射临时表 UPDATE JOIN，先备份后刷数

`sql/change16_food_category18.sql`：

1. 备份表 `foods_category15_backup AS SELECT id, code, category_code FROM foods`（回滚依据，验证期后再清理）；
2. `CREATE TEMPORARY TABLE cat18_map(code VARCHAR(32), category_code VARCHAR(2), category_name VARCHAR(50))`，映射数据由 python 脚本产出（内置 F001–F200 来自 D3 结果、TFDA 分析编号来自 D2 结果，以 SQL INSERT 形式拼入脚本文件）；
3. `UPDATE foods f JOIN cat18_map m ON f.code=m.code SET f.category_code=m.category_code, f.category_name=m.category_name`；
4. `schema_migrations` 补账 version=16；脚本幂等（重刷同值无变化）。

共建存量（软删测试数据 8 条）：直接并入映射（按其原 category_code 旧→新先验转换，人工确认）。

备选：启动期 Java 回填（Initializer 里做）——弃用，刷数是一次性迁移动作，应走 SQL 迁移账本而非常驻代码。

### D5 初始化器与部署形态

- `FoodLibraryInitializer`（@Order(1)）：`foods_200.json` 已是 18 类版，就绪检查（source=1 计 200）不变；全新空库直接导入 18 类，无需刷数。
- `TfdaFoodInitializer`（@Order(2)）：`foods_tfda.json` 重跑产出（category 为 18 类），就绪检查（data_batch 计 1730）不变；对拍口径从「A05002 稻米 10 类」改为 18 类样本。
- change16 仅存量库需要；全新部署 = 跑全部 change + 启动自导入即 18 类。

### D6 三端字典与 UI

- 后端：`FoodCategoryEnum` 重写为 18 项（code+desc），`/api/foods/categories`、投稿/管理保存的分类校验（`of()` 合法性）自动适配。
- 小程序：`pages/food/index.vue` 分类条维持横滑 chips + 「全部」，项数 10→18（chips 逐个横滑，不折行宫格——18 项宫格占屏过高）；`FoodThumb.vue` 色块字典扩为 18 项（V2 色板同族浅底/深字，label 用 2 字简称：淀粉/谷物/肉/鱼贝/蛋/乳/豆/蔬菜/菇/藻/水果/坚果/油脂/糖/糕点/调味/饮料/加工，非法编码走既有 DEFAULT 兜底）；`custom-edit.vue` picker 选项随接口字典变化，默认 `categoryCode` 从 `'10'` 改为 `'18'`（共建典型为自制复合食物）；`custom-list.vue`/`detail.vue` 分类名展示随 VO；`src/data/foods.ts` 离线兜底副本同步 18 类 category 值。
- 管理后台：`constants/dicts.ts` `FOOD_CATEGORY_OPTIONS` 重写为 18 项（与后端枚举对齐），`view/foods/index.vue` 筛选/标签自动跟随。

### D7 兼容与展示细节

- 列表行 `food-alias` 位、详情页分类名等展示 `category_name` 全称（如「调味料及香辛料类」），不截断改写。
- 老客户端兼容：分类列表接口返回的是字典全集，老版本小程序（若线上已发）拿到 18 类后筛选功能不破坏（code 值传递查询，服务端按值过滤）；无需灰度接口。
- 历史饮食记录不受影响（`diet_records` 不存分类值，食物快照冗余不含分类，仅展示时按 foods 现值）。

## Risks / Trade-offs

- [18 类中「加工调理食品及其他类」316 条成为最大杂烩 tab，大陆用户对部分类名不熟] → 已由用户拍板接受（保持官方口径）；分类条横滑 + 搜索叠加缓解浏览成本。
- [内置 200 关键词归类有边界误判（如南瓜：淀粉 or 蔬菜）] → 规则用 raw 成员名单校准 + 无把握条目进人工裁决清单，产物带报告可复核。
- [刷数 UPDATE 影响存量 1930+ 行，误刷不可逆] → 刷数前 SELECT 备份表留档；映射表来自编号精确关联（TFDA）与人工确认（内置），非模糊匹配；重放幂等。
- [14 糖类仅 12 条，tab 近乎空] → 接受（官方口径完整性优先）；热门/搜索不依赖分类密度。
- [两 change 归档顺序约束] → part2 先归档（已实施完毕、458 测试绿），本 change 后实施/归档；proposal 已声明。

## Migration Plan

1. **前置**：`food-curation-part2` 归档（delta 并入主 specs）。
2. **数据**：D3 脚本归类内置 200（人工清单裁决）→ D2 重跑 `tfda_clean.py` → 生成 18 类版 `foods_200.json`/`foods_tfda.json` + D4 映射数据。
3. **后端**：枚举 18 化 + 初始化器对拍口径 + 单测改造（先红后绿，TDD）。
4. **SQL**：本地库执行 change16（备份→刷数→补账），验证抽查（分类分布与 raw/归类报告一致）。
5. **前端**：小程序 + 管理后台字典/色块/默认值改造，type-check + build 过。
6. **回归**：后端 `mvn test` 全绿；三端手测（分类条 18 项切换、搜索叠加、投稿选择、后台筛选、离线兜底数据分类）。
7. **回滚**：代码走 git revert；数据用备份表反向 UPDATE（旧 code→旧类码）+ 删除 v16 补账行。

## Open Questions

- 18 类展示顺序（D1 表）如需调整，实现期确认即可，不影响 spec/任务结构。
- 内置 200 人工裁决清单（预计 ≤30 条）在 D3 脚本产出后由用户逐条确认，属实现期输入。

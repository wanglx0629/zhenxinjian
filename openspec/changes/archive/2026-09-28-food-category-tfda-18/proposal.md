# 食物库分类全量切换 TFDA 18 大类（food-category-tfda-18）

## Why

食物库数据主体（1730 条 TFDA 导入食物）的原始分类是 TFDA 官方 18 大类，当前清洗管线为适配自有 10 大分类将其压缩映射，导致「加工調理食品及其他類」（316 条）等官方类目被打散、分类颗粒度与来源口径不一致。应改为以爬虫的 TFDA 18 大类为基础分类体系，全部食物按 18 类原样展示，同时分类筛选 UI/交互对齐市面上主流饮食记录产品的常用形态。

## What Changes

- **BREAKING**：分类字典从自有 10 大类（01 谷薯杂豆·主食 … 10 油脂·调味·饮品）全量切换为 TFDA 原始 18 大类（编码 01–18 顺延，官方口径顺序），`foods.category_code/category_name` 存量刷数。
- 导入管线反转：`tfda_clean.py` 不再把 TFDA 18 类压映射到 10 类，改为繁转简后保留原始 18 类作为本系统分类；重跑清洗产出并按 TFDA 分析编号回填已入库条目。
- 内置 200 条与存量用户共建食物按 18 类归类（名称关键词规则 + 少量人工裁决），保证全库分类值与 18 类字典一致。
- 后端 `FoodCategoryEnum` 18 化，分类校验、分类列表接口、初始化器对拍口径同步切换；新增 change16 刷数 SQL（幂等、可回溯）。
- 小程序食物库分类筛选交互对齐市面常用形态（顶部可横滑分类标签条 + 「全部」入口，单选分类、可与搜索叠加），`FoodThumb` 占位色块从 10 分类色扩展为 18 分类色。
- 共建投稿表单分类选择改为 18 项；食物详情/列表/我的食物分类展示随字典切换。
- 管理后台食物页分类筛选字典与来源标签同步 18 化。

## Capabilities

### New Capabilities

（无——分类字典与展示行为归入既有能力的 requirement 变更）

### Modified Capabilities

- `food/library`：「分类筛选与分类列表」requirement 由 10 大分类改为 18 大分类（含市面常用筛选交互约定）；离线兜底数据与热门清单分类值随口径更新。
- `food/import`：「可复跑的导入管线与去重」requirement 的分类映射口径反转——由「TFDA 18 大类映射到本系统 10 类」改为「繁转简后保留 TFDA 原始 18 大类直接作为本系统分类」，并要求对已入库条目按分析编号关联回填。

## Impact

- **后端**：`FoodCategoryEnum` 重写（18 项）、`FoodService`/`AdminFoodService`/`CustomFoodService` 分类相关校验与字典接口、`FoodLibraryInitializer`/`TfdaFoodInitializer` 对拍与就绪口径、`sql/change16_food_category18.sql` 存量刷数与补账。
- **数据管线**：`python/tfda_clean.py` 重跑（保留原始分类字段）、`foods_200.json`/`foods_tfda.json` 分类值更新、存量 1930+ 条刷数。
- **小程序**：`pages/food/index.vue` 分类筛选交互、`components/FoodThumb.vue` 18 色块、`custom-edit.vue` 分类选择、`detail.vue`/`custom-list.vue` 分类展示、`api/food.ts`/`store/food.ts` 类型。
- **管理后台**：`constants/dicts.ts` 分类字典、`view/foods/index.vue` 筛选与标签。
- **流程依赖**：本 change 修改 `food/import` 能力中由 `food-curation-part2` 新增的 requirement，实施与归档 MUST 在 `food-curation-part2` 归档之后进行（其 delta 先并入主 specs），避免两层未归档 delta 叠加冲突。

# Tasks — 食物库（内置 200 条 + 搜索筛选 + 自定义食物）

## 1. 数据库迁移

- [x] 1.1 编写 `sql/change4_foods.sql`：`foods` 表（食物编号 code F001–F200 / 分类编号+名称 / 名称 / 别名 / 碳水 / 蛋白 / 脂肪 DECIMAL(5,1) / 能量 INT / 常用单份克数 serving / 来源 source 1内置 2自定义 / 归属 user_id / `delete_flag` + `status` + 审计列），按 design D3 建生成列 `name_active` 活跃唯一（`CONCAT(user_id,':',name)` 仅自定义生效），`code` 建唯一约束兜底导入幂等
- [x] 1.2 本地执行迁移并验证：活跃唯一约束生效（同用户插第二条活跃同名自定义食物报错，内置食物不受约束影响）、逻辑删除列默认值正确、`code` 唯一生效

## 2. 后端：枚举与数据层

- [x] 2.1 新建字典枚举（`common/enums/`，code+desc+of 规约）：`FoodSourceEnum`（1内置 2自定义）、`FoodCategoryEnum`（01–10 十大分类，顺序固定）
- [x] 2.2 新建 PO：`Food`（`@TableLogic` 逻辑删除、营养值 `BigDecimal`）；DTO（自定义食物新增/编辑入参 + Bean Validation 区间注解）与 VO（食物详情含每 100g 营养值与 serving、搜索列表项、分类项、试算结果）
- [x] 2.3 新建 `FoodMapper`（MyBatis-Plus BaseMapper）

## 3. 后端：内置数据导入

- [x] 3.1 将 `foods_200.json` 拷入 `src/main/resources/`，定义导入用 Jackson 反序列化模型（与真源字段对齐）
- [x] 3.2 实现 `FoodLibraryInitializer`（`ApplicationRunner`）：启动时判断内置食物数 ≥200 则跳过，否则解析 JSON（断言条数恰为 200，否则拒绝启动并报错）批量插入，捕获 `DuplicateKeyException` 跳过已有编号，日志输出导入计数
- [x] 3.3 导入验证：首次启动 200 条全量落库且与真源一致（F001 大米碳水 77.9 / F002 米饭碳水 25.9 抽样对拍）；二次重启幂等不重复、内置仍恰为 200 条；与 `foods.ts` 抽样对拍一致

## 4. 后端：食物库查询服务

- [x] 4.1 `FoodService.search`：关键字对名称+别名模糊匹配（`LIKE '%kw%'`），默认返回内置 + 当前用户自定义，叠加分类筛选，分页（单页 ≤50）返回总数；空关键字返回空集不报错
- [x] 4.2 `FoodService.categories`：返回 10 大分类有序列表（编号+名称）
- [x] 4.3 `FoodService.hot`：`FoodHotConstant` 静态编号清单（≤12 条高频减脂食物）按 code 查库返回
- [x] 4.4 `FoodService.detail`：返回食物详情（名称/别名/分类/每 100g 三宏与能量/常用单份克数）
- [x] 4.5 `FoodService.calc`：按「每 100g 值 × 克数 ÷ 100」换算指定克数的三宏与能量，克数区间 1–10000

## 5. 后端：自定义食物服务

- [x] 5.1 `ExceptionConstant` 新增食物库文案；`CommonConstant` 启用 404xx 段位（40401 名称重复 / 40402 宏量非法 / 40403 能量不守恒 / 40404 食物不存在或不可操作 / 40405 无权限操作他人食物）
- [x] 5.2 `CustomFoodService.save`：名称归属唯一（`DuplicateKeyException` 转 40401 不抛 500）+ 宏量区间（非负且各 ≤100）+ 能量守恒（`|4c+4p+9f−kcal|/max(kcal,1) ≤ 10%`，`BigDecimal` 判定）后端兜底校验；新增/编辑一体
- [x] 5.3 `CustomFoodService.remove`：软删除（`@TableLogic`），永不物理删除
- [x] 5.4 `CustomFoodService.listMine`：仅返回当前用户活跃自定义食物
- [x] 5.5 数据归属校验：编辑/删除仅限本人自定义食物（越权返回 40405）；内置食物写请求一律拒绝（40404）

## 6. 后端：控制器

- [x] 6.1 `FoodController`：搜索 / 分类列表 / 热门 / 详情 / 试算 查询接口，返回 `Result`，注释含「作者: wanglx」
- [x] 6.2 `CustomFoodController`（或并入 FoodController）：自定义食物新增 / 编辑 / 删除 / 我的列表接口，返回 `Result`

## 7. 小程序：数据通路

- [x] 7.1 新增 `src/api/food.ts`：搜索 / 分类 / 热门 / 详情 / 试算 / 自定义增删改查封装 + 类型定义
- [x] 7.2 新增 food store：搜索结果 / 分类 / 详情 / 降级标记；后端请求失败时切本地 `FOODS` 数组降级检索，联网恢复后自动切回（design D8）

## 8. 小程序：P10 食物库页

- [x] 8.1 重写 `pages/food/index.vue`：搜索框（防抖 300ms）+ 分类筛选 + 历史搜索（本地存储，上限 10 条）+ 热门搜索 + 结果列表分页
- [x] 8.2 食物详情页：每 100g 营养值展示 + 默认份量克数输入，实时换算三宏与能量；本期仅查看与试算，「写入饮食记录」入口留待 Change 5
- [x] 8.3 `pages.json` 注册食物详情与自定义食物路由

## 9. 小程序：自定义食物页

- [x] 9.1 新建自定义食物录入/编辑页：名称 + 每 100g 三宏 + 能量，前端先做区间与 ±10% 守恒校验，提交以后端为准
- [x] 9.2 新建「我的自定义」列表：可编辑 / 删除（删除二次确认，软删），删除后从列表与搜索消失

## 10. 联调与验收

- [x] 10.1 后端 `mvn test` 通过（导入幂等 / 搜索别名命中「白米饭」/ 分类叠加搜索 / 分页 / 自定义守恒与归属校验单测）；小程序 `npm run build:mp-weixin` 编译无错
- [x] 10.2 按 spec 场景走查：首次导入 200 条（F001–F200）→ 重复启动幂等 → 别名搜索「白米饭」命中米饭 → 分类 08 + 「桃」叠加筛选 → 蔬菜 38 条分页 20+18 → 热门清单非空有界 → 详情 150g 试算米饭碳水 38.85 → 后端失败降级本地 → 新增自定义成功 → 同名重复 40401 → 能量不守恒 40403 → 编辑他人 40405 → 删除内置 40404 → 软删后不可见
- [x] 10.3 更新验收记录，准备 `openspec verify`

## 验收记录（2026-09-09）

- 后端 `mvn test`：55 用例全绿（含 `FoodLibraryInitializerTest` 5 例、`CustomFoodServiceTest` 12 例）；小程序 `npm run build:mp-weixin` 与 `vue-tsc --noEmit` 均通过
- 场景走查脚本对真实后端（8080）跑 23 项断言全部 PASS：分类有序 / 别名搜索 / 名称片段 / 空关键字 / 分类叠加 / 蔬菜分页 20+18 无重复 / 热门有界 / 详情 / 150g 试算碳水 38.85 / 250g 换算 / 克数越界拒绝 / 新增成功 / 同名 40401 / 不守恒 40403 / 负值 40402 / 编辑他人 40405 / 编辑自己成功 / 删除内置 40404 / 他人数据隔离 / 软删后不可见
- DB 层核验：软删记录保留（`delete_flag=1`）、内置恰为 200 条、F001 碳水 77.9 / F002 碳水 25.9 与真源一致

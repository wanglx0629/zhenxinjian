# 食物库分类全量切换 TFDA 18 大类 — Tasks

> 执行顺序即编号顺序；每步完成后勾选 `[x]`。规约：错误文案进 `ExceptionConstant`、接口返回 `Result`、注释 `作者: wanglx`；MySQL 守开发规范；遵循 TDD（先写/改测试再实现）；JDK 25（`set JAVA_HOME=D:\App\java\25`）。设计依据：design.md（D1–D7）。

## 1. 前置

- [x] 1.1 归档 `food-curation-part2`（实施已完毕、458 测试绿、validate 通过；其 food/import delta 须先并入主 specs，本 change 的 import delta 才能生效）
  - 12.2/12.3 手测项并入本 change §6 手测清单合并执行（分类 UI 大改后反正需重测）
  - 2026-09-24：已归档为 archive/2026-09-24-food-curation-part2，specs 同步 +21/~5，food/import 与 food/curation 主 spec 已建

## 2. 数据管线（归类与清洗）

- [x] 2.1 新增 `python/builtin_recat.py`：内置 200 按「旧 10 类先验 + 名称关键词」归类 18 类，关键词边界用 raw 各类成员名单校准；无把握条目产出 `data-import/builtin_recat/manual_review.json` 供人工裁决，裁决后回填
  - 产物：更新后端 `resources/foods_200.json`（category 为 18 类）+ 归类报告 `data-import/builtin_recat/report.txt`
  - 2026-09-24：人工裁决 27 条已确认并回填 MANUAL_OVERRIDES 重跑 --apply，report 显示 0 条待裁决
- [x] 2.2 改 `python/tfda_clean.py`：新增 `CATEGORY_18` 固定映射（raw `category` 字段繁转简直读），移除 `CATEGORY_MAP`/`BEAN_SOY`/`PROCESSED_RULES` 压映射；豆类不拆、加工调理类不打散；重跑产出 18 类版 `clean/foods_tfda.json`（1730 条口径不变），复制入后端 resources
- [x] 2.3 产出 change16 刷数映射：内置 F001–F200（2.1 结果）+ TFDA 分析编号（2.2 结果）+ 共建存量旧码转换 → `UPDATE JOIN` 用 INSERT 映射数据（拼入 SQL 脚本）；生成回滚映射（备份表方案见 design D4）
  - 2026-09-24：`python/change16_gen.py` 产出 1930 条映射；修复共建存量 UPDATE 的两处缺陷（CASE 字面量双重引号；MySQL SET 左到右求值须 name 在前引用原 code）

## 3. 后端（TDD）

- [x] 3.1 改测试先行：FoodServiceTest 分类列表/筛选相关用例改 18 类口径（18 项、顺序、编码）；CustomFoodServiceTest/AdminFoodServiceTest 分类校验用例改 18 类合法/非法码
  - 2026-09-24：FoodServiceTest 断言 18 项（01 淀粉类…18 加工调理食品及其他类）；CustomFoodServiceTest 新增 categoryCode=18 受理/19 拒绝 40402/缺省默认 18 三用例；AdminFoodAuditServiceTest 新增 ADMIN_FIX 分类 18 落库与非法 40917 两用例
- [x] 3.2 重写 `FoodCategoryEnum` 为 18 项（code 01–18 + design D1 简体类名与顺序，code+desc+of）；`/api/foods/categories` 与保存类校验随枚举自动适配；跑 3.1 用例至全绿
  - 2026-09-24：CustomFoodService DEFAULT_CATEGORY 改 PROCESSED(18)；CustomFoodSaveDTO/FoodCategoryVO/AdminFoodSaveDTO/FoodController Swagger 口径 01-18
- [x] 3.3 `FoodLibraryInitializerTest`/`TfdaFoodInitializerTest` 对拍口径更新（18 类样本，如 TFDA 某条目分类=原始类；内置对拍样本同改）；幂等/就绪检查逻辑不动
  - 2026-09-24：TFDA 粈米样本改 02 谷物类；内置 F001 对拍补 02/谷物类断言
- [x] 3.4 新建 `sql/change16_food_category18.sql`：备份表 `foods_category15_backup` → 临时映射表 UPDATE JOIN 刷 `category_code/category_name` → 共建存量处理 → `schema_migrations` 补账 v16；幂等可重跑（design D4）
- [x] 3.5 本地库执行 change16 并验证：备份表行数=存量行数；刷后分布与 2.1/2.2 报告一致（18 类计数抽对）；重复执行无变化
  - 2026-09-24：1943 行全量刷 18 类（内置 200+TFDA 1730 编号精确映射 + 共建 13 先验转换逐类核对一致）；备份表 1943 行保留旧 10 类值；重跑分布无变化；v16 版本账补账
  - 修复生成器头注释缺陷：回滚提示行漏 `--` 前缀致其成为可执行 SET 语句（首跑报 Unknown system variable），且注释内 ASCII `;` 在 mysql 批处理模式会切分语句

## 4. 小程序（uniapp）

- [x] 4.1 `components/FoodThumb.vue`：色块字典扩 18 项（V2 色板同族浅底/深字 + 2 字简称 label，design D6）；非法编码维持 DEFAULT 兜底
  - 2026-09-24：18 项配色沿用旧色族（淀粉/肉/蛋/鱼贝/豆/蔬菜/藻/水果/坚果→同 hue 平移）+ 新 8 项（谷物金/乳蓝/菇棕/油脂橄榄/糖粉/糕点玫/调味紫/饮料蓝/加工灰蓝）
- [x] 4.2 `pages/food/index.vue`：分类条 18 项（横滑 chips + 全部，形态不变），选中态/搜索叠加逻辑不动，验证 18 项横滑流畅
  - 2026-09-24：分类条完全数据驱动（fetchCategories → v-for chips），无需改码；`build:mp-weixin` 过即 18 项渲染就绪，横滑手测并入 6.2
- [x] 4.3 `pages/food/custom-edit.vue`：分类 picker 随接口字典（18 项）；默认 `categoryCode` 由 `'10'` 改 `'18'`；编辑态回显逻辑不变
  - 2026-09-24：表单默认与编辑回显兜底两处 `'10'`→`'18'`；picker 范围随 categories 接口字典自动 18 项
- [x] 4.4 `src/data/foods.ts` 离线兜底副本同步 18 类 category 值（与 2.1 产物一致）
  - 2026-09-24：200 条 category 全量按后端 foods_200.json 同步（临时脚本跑批后删除）；接口注释改 18 类口径；store/food.ts 本地分类派生（substring 拆 code/name）自动适配
- [x] 4.5 `pages/food/detail.vue`、`custom-list.vue` 分类名展示核验（全称不截断）；`npm run type-check` 通过
  - 2026-09-24：两页均直显 VO categoryName 且无 nowrap/ellipsis 截断样式；type-check 与 build:mp-weixin 均通过；api/food.ts、store/food.ts 注释口径同步 18 类

## 5. 管理后台（front）

- [x] 5.1 `constants/dicts.ts`：`FOOD_CATEGORY_OPTIONS` 重写 18 项（与后端枚举逐项对齐）；`view/foods/index.vue` 筛选下拉与列表标签自动跟随，验证筛选/审核流程不受影响；`npm run build` 通过
  - 2026-09-24：18 项与 FoodCategoryEnum 逐项对齐（code+desc 全同）；三处消费方（列表筛选/列表标签列/保存表单与审核弹窗 picker）均数据驱动自动跟随；front 全局无旧类名残留（grep 验证）；`npm run build`（vue-tsc + vite）通过

## 6. 测试与收尾

- [x] 6.1 后端 `mvn test` 全绿（JDK 25）；含 3.x 新改用例
  - 2026-09-24：73 个测试类 463 用例全绿（Failures/Errors/Skipped 均 0）；受影响 5 类计数：FoodServiceTest 22 / CustomFoodServiceTest 26 / AdminFoodAuditServiceTest 18 / FoodLibraryInitializerTest 5 / TfdaFoodInitializerTest 4
- [x] 6.2 三端手测清单：分类条 18 项切换与横滑、搜索叠加分类、投稿分类选择（18 项/默认加工调理）、无图食物 18 类占位色块、后台分类筛选与审核流、离线兜底数据分类正确；并入 part2 遗留手测项（投稿全链路/后台审核/OCR/引用断链/游客迁移）
  - 2026-09-28：用户拍板跳过手测直接归档；手测项并入日常回归（18 类 UI 为数据驱动改造，自动化测试 + 三端 build 均已覆盖编译与数据一致性）
- [x] 6.3 `openspec validate food-category-tfda-18` 通过；自查规约（枚举 code+desc+of/Result/注释/SQL 软删幂等/版本账）后提交（`feature：...`，不主动 push），手测通过后归档
  - 2026-09-24：validate 通过；规约自查通过（FoodCategoryEnum code+desc+of 齐备；错误码沿用 40402/40917 进 ExceptionConstant 未新增裸文案；改动文件均带 `作者: wanglx` 头注释；change16 重放幂等 + v16 版本账已验证）；uniapp type-check/build 与 front build 均过；离线兜底 foods.ts 200 条与后端 foods_200.json 逐条比对一致

# Tasks — 2026-09-30-full-project-scan（DESIGN 设计级）

- **级别**：DESIGN（设计级）
- **依据坏味道版本**：badsmells.md v1.0（提交版本 `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`）
- **生成方式**：mole:plan 全量生成（9 条未清除 → 9 个任务）
- **执行粒度**：每次 `mole:apply` 仅处理一个未完成任务
- **回归命令基线**：后端 `set JAVA_HOME=D:\App\java\25 && mvn -q test`；小程序 `npm run check`（vue-tsc）；管理后台 `npm run build`

---

## §3 任务清单

| 任务 | 追溯 | 标题 | 严重度 | 状态 |
| --- | --- | --- | --- | --- |
| DESIGN-T01 | DESIGN-职责-001 | WeightService 四职责拆分 | 高 | 已完成 |
| DESIGN-T02 | DESIGN-重复-001 | 小程序全局样式重复副本清理 | 高 | 已完成 |
| DESIGN-T03 | DESIGN-上帝组件-001 | dashboard God 组件拆分 | 中 | 已完成 |
| DESIGN-T04 | DESIGN-特征依恋-001 | 粘性比计算下沉统计工具层 | 中 | 已完成 |
| DESIGN-T05 | DESIGN-重复-002 | 时段→餐别映射收敛公共工具 | 中 | 已完成 |
| DESIGN-T06 | DESIGN-重复-003 | 每 100g 换算收敛公共函数 | 中 | 已完成 |
| DESIGN-T07 | DESIGN-重复-004 | 食物弹窗表单抽象公共模型 | 中 | 已完成 |
| DESIGN-T08 | DESIGN-特征依恋-002 | manualValid 副作用改纯函数 | 中 | 已完成 |
| DESIGN-T09 | DESIGN-死代码-001 | MEAL_EMOJI 死常量删除 | 低 | 已完成 |

状态说明：**未开始** / 进行中 / 已完成。共 9 个任务（高 2 / 中 6 / 低 1）。

---

## §3.1 任务步骤（DESIGN 7 步：确认→接缝→安全网→解依赖→重构→回归→用户确认）

### DESIGN-T01 WeightService 四职责拆分

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `WeightService.java:75,116,152,203,218,234,252,293` 四类职责堆叠 |
| ② 识别接缝 | Feathers 接缝：`save()` 内 `applyPlatformAdjust` 调用点、`trend()` 聚合流、`listLogs` 日志查询 |
| ③ 测试安全网 | 现有 `mvn -q test` 权重相关用例为安全网；拆分前确认 WeightServiceTest 覆盖 |
| ④ 解依赖 | 抽出 `PlateauDetector`（平台/恢复判定）与 `WeightAggregateService`（趋势聚合），解除 CRUD 与判定的耦合 |
| ⑤ 应用重构 | `WeightService` 只留 CRUD 编排；平台判定改后置或 `REQUIRES_NEW`；趋势聚合下沉 |
| ⑥ 回归测绿 | `set JAVA_HOME=D:\App\java\25 && mvn -q test` 全绿零行为变更 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T02 小程序全局样式重复副本清理

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `App.vue:95-106` 全局 `.btn-primary` vs 三页 scoped 同名覆盖；`.panel` 五页重复 |
| ② 识别接缝 | 各页 scoped `<style>` 内的 `.btn-primary`/`.panel` 定义（视觉分叉点） |
| ③ 测试安全网 | `npm run check`（vue-tsc + build）为安全网 |
| ④ 解依赖 | 区分「与全局一致可删」与「有意漂移须保留修饰类」 |
| ⑤ 应用重构 | 删三处局部 `.btn-primary`、五处局部 `.panel` 副本，统一走全局；差异化用修饰类 |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误，视觉无回归 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T03 dashboard God 组件拆分

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `view/dashboard/index.vue:48-127,132-192` 脚本/图表/四 KPI 卡片高度耦合 |
| ② 识别接缝 | KPI 卡片结构重复、趋势图/TOP10 表格为独立渲染块（可拆接缝） |
| ③ 测试安全网 | front `npm run build` 为安全网 |
| ④ 解依赖 | 抽 `StatCard` 子组件 props 化，解耦卡片取值与展示 |
| ⑤ 应用重构 | 抽 `StatCard` 替换四卡片；趋势/TOP10 拆 section；数据加载下沉 `useDashboardStats` |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-front && npm run build` 零错误 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T04 粘性比计算下沉统计工具层

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `dashboard/index.vue:32-37` 粘性比 `computed` 就地实现 |
| ② 识别接缝 | `overview` DTO 到粘性比值的纯计算点（可抽出） |
| ③ 测试安全网 | 补 `computeStickiness` 纯函数单测 |
| ④ 解依赖 | 粘性比计算与视图解耦 |
| ⑤ 应用重构 | `computeStickiness(overview)` 下沉 `utils/stats.ts`，返回 `{ value, hasData }`，组件仅渲染 |
| ⑥ 回归测绿 | front 单测 + `npm run build` 绿 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T05 时段→餐别映射收敛公共工具

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `record/add.vue:42` 与 `record/recognize.vue:37` 相同 `defaultMealType()` |
| ② 识别接缝 | 两页各自 `defaultMealType` 函数（可提取） |
| ③ 测试安全网 | `npm run check` 为安全网 |
| ④ 解依赖 | 提取纯函数，两页解除本地副本 |
| ⑤ 应用重构 | 函数下沉 `utils/format.ts` 或 `config/constants.ts`，两页改引用 |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T06 每 100g 换算收敛公共函数

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `record/add.vue:71-80`、`record/recognize.vue:103-111`、`store/food.ts:168-179` 三处换算 |
| ② 识别接缝 | 三处 per-100g 换算调用点 |
| ③ 测试安全网 | `npm run check` + 补换算纯函数单测 |
| ④ 解依赖 | 统一走 `foodStore.calc()` 或抽 `scalePer100g()` |
| ⑤ 应用重构 | 提取 `utils/macro.ts` 纯函数 `scalePer100g()`，三处改引用 |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T07 食物弹窗表单抽象公共模型

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `FoodEditDialog.vue:19-53` 与 `FoodAuditFixDialog.vue:22-45` 字段/rules 重复 |
| ② 识别接缝 | 两弹窗共用食物字段定义与校验规则（可提取工厂） |
| ③ 测试安全网 | front `npm run build` 为安全网 |
| ④ 解依赖 | 抽 `composables/useFoodForm.ts` 或共享 `FoodFields` 配置 |
| ⑤ 应用重构 | 两弹窗引用共享表单模型/校验工厂，仅保留差异（编辑 vs 修正） |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-front && npm run build` 零错误 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T08 manualValid 副作用改纯函数

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `record/add.vue:91-113` computed getter 内写 `manualErr.value` 副作用 |
| ② 识别接缝 | `manualValid` 从 computed 改为显式校验函数 |
| ③ 测试安全网 | `npm run check` 为安全网 |
| ④ 解依赖 | 校验逻辑与响应式副作用分离 |
| ⑤ 应用重构 | 改 `validateManual()` 显式函数，提交/输入时主动调用并写 `manualErr` |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

### DESIGN-T09 MEAL_EMOJI 死常量删除

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `config/constants.ts:81` `MEAL_EMOJI` 全库零引用 |
| ② 识别接缝 | 无（纯删除） |
| ③ 测试安全网 | `npm run check` 为安全网 |
| ④ 解依赖 | 无调用方 |
| ⑤ 应用重构 | 删除 `MEAL_EMOJI` 常量 |
| ⑥ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑦ 用户确认 | 展示 diff 获批后提交 |

---

## 执行顺序备注

- DESIGN-T03（dashboard 拆分）与 DESIGN-T04（粘性比下沉）同触 `dashboard/index.vue`，建议顺序执行 T04 → T03 避免冲突。
- DESIGN-T05/T06/T08 同触 `record/add.vue` 与 `record/recognize.vue`，串行执行。
- DESIGN-T02 全局样式清理与 ARCH-T01（taper 分层）无依赖，但均属小程序端，可并行乱了避免同一文件冲突。
- DESIGN-T01（WeightService 拆分）为后端单体大改，建议独立执行并先补安全网。

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：DESIGN 级 9 条未清除 → 9 个任务 |
| v1.1 | 2026-10-08 | — | DESIGN-T03（dashboard God 组件拆分）：抽 `StatCard` 子组件 + `useDashboardStats` composable，标记已完成 |
| v1.2 | 2026-10-08 | — | DESIGN-T04（粘性比下沉）：抽 `computeStickiness` 到 utils/stats.ts + vitest 单测，标记已完成 |
| v1.3 | 2026-10-08 | — | DESIGN-T05（时段→餐别映射收敛）：`defaultMealType` 下沉 utils/format.ts，两页改引用，标记已完成 |
| v1.4 | 2026-10-08 | — | DESIGN-T06（每 100g 换算收敛）：抽 `scalePer100g` 到 utils/macro.ts + vitest 单测，三处改引用并统一后端口径（宏量 2 位、kcal 整数），标记已完成 |
| v1.5 | 2026-10-08 | — | DESIGN-T07（食物弹窗表单抽象）：抽 `view/foods/foodForm.ts` 共享 `createFoodForm`/`foodFormRules`，两弹窗改引用，标记已完成 |
| v1.6 | 2026-10-08 | — | DESIGN-T08（manualValid 副作用改显式函数）：`computed` 改 `validateManual()`，提交时调用并写 manualErr，标记已完成 |
| v1.7 | 2026-10-08 | — | DESIGN-T09（MEAL_EMOJI 死常量删除）：删除 config/constants.ts 中全库零引用的 `MEAL_EMOJI`，标记已完成 |
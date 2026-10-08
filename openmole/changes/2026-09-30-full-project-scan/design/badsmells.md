# Badsmells — 2026-09-30-full-project-scan（DESIGN 设计级）

- **级别**：DESIGN（设计级）
- **范围**：仓库全量（后端 + 小程序 + 管理后台三端）
- **提交版本**：`4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`
- **识别方式**：三端三路并行静态侦察（Fowler 主 + PHAME 补：上帝类/重复代码/特征依恋/霰弹式修改/不当亲密/数据泥团等）；本级别为首次扫描
- **检出语言**：Java、TypeScript、Vue SFC

---

## §2.0 索引

| BS-ID | 类别 | 严重度 | 状态 |
| --- | --- | --- | --- |
| DESIGN-职责-001 | 职责混合 | 高 | 已消除 |
| DESIGN-重复-001 | 重复代码 | 高 | 已消除 |
| DESIGN-上帝组件-001 | 上帝组件 | 中 | 已消除 |
| DESIGN-特征依恋-001 | 特征依恋 | 中 | 已消除 |
| DESIGN-重复-002 | 重复代码 | 中 | 已消除 |
| DESIGN-重复-003 | 重复代码 | 中 | 已消除 |
| DESIGN-重复-004 | 重复代码 | 中 | 已消除 |
| DESIGN-特征依恋-002 | 特征依恋 | 中 | 已消除 |
| DESIGN-死代码-001 | 冗余 | 低 | 已消除 |

状态说明：**未清除** / 已消除 / 部分残余。共 9 条（高 2 / 中 6 / 低 1）。

---

## §2.1 明细

### DESIGN-职责-001 — WeightService 四职责混合（上帝 Service）

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 职责混合（上帝类） |
| 严重度 | 高 |
| 症状 | `WeightService` 单类同时承担体重 CRUD（save/list/remove）、平台/恢复判定（isPlateau/applyPlatformAdjust）、趋势聚合（trend/resolveTrendStart）、平台调碳日志（listLogs/insertLog），四类职责按「体重入口」而非「职责」堆叠；`save()` 内联触发 `applyPlatformAdjust` 占事务时长 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/WeightService.java:75`（save）、`:116`（list）、`:152`（trend）、`:203`（remove）、`:218`（listLogs）、`:234`（isPlateau）、`:252`（applyPlatformAdjust）、`:293`（insertLog） |
| 根因 | 新功能（体重随时记录 + 每日聚合 + 趋势）在单服务内持续堆叠，未按职责切分 |
| 影响 | 体重域改动牵连平台判定与趋势，回归面大；平台判定较重（多查库）在 save 事务内串联执行，并发保存时平台态可能被后续写覆盖 |
| 修复建议 | 拆 `PlateauDetector`（平台/恢复判定）与 `WeightAggregateService`（趋势聚合），WeightService 只做 CRUD 编排；平台判定改后置处理或 `REQUIRES_NEW` |

### DESIGN-重复-001 — 小程序全局样式被同名 scoped 类覆盖（视觉口径漂移）

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 重复代码（霰弹式修改） |
| 严重度 | 高 |
| 症状 | `App.vue` 全局 `.btn-primary` 为琥珀绿渐变 CTA（`$zhenxinjian-gradient-cta` + 阴影），但 reminder/custom-list/auth 三页 scoped 又同名覆盖为纯绿（`$zhenxinjian-primary`），色值语义漂移（渐变 vs 纯色）；`.panel` 亦在 5 页 scoped 重写，丢掉阴影、padding 24/28/32 不一 |
| 证据 | `apps/zhenxinjian-uniapp/src/App.vue:95-106`（全局 `.btn-primary`）vs `pages/reminder/index.vue:461-469`、`pages/food/custom-list.vue:310-318`、`pages/auth/guide.vue:268-275`；`.panel`：`App.vue:67-74` vs `pages/reminder/index.vue:386`、`pages/record/add.vue:395`、`pages/record/recognize.vue:257`、`pages/food/index.vue:352`、`pages/food/detail.vue:199` |
| 根因 | 上轮 B-T29 已收敛全局样式，但新增页未遵循、继续复制旧 `.panel`/`.btn-primary` |
| 影响 | 同名按钮/卡片在不同页外观不一致；改全局样式时局部定义不跟随，形成隐藏分叉 |
| 修复建议 | 删除三处局部 `.btn-primary`、五处局部 `.panel` 副本，统一走全局真源；确需差异化用修饰类（如 `.btn-primary--solid`）显式声明 |

### DESIGN-上帝组件-001 — dashboard/index.vue 上帝组件回潮

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 上帝组件（兼重复模板） |
| 严重度 | 中 |
| 症状 | dashboard 页面近 400 行，混合脚本/图表 option 内联/4 张 KPI 卡片/趋势图/双 TOP10 表格/粘性比环；4 张 KPI 卡片（今日 DAU/昨日 DAU/MAU/累计用户·记录）是 4 段几乎相同的 `el-col > .stat-card > .stat-body > .stat-icon + .stat-main` 结构，仅图标/文案/取值/渐变不同 |
| 证据 | `apps/zhenxinjian-front/src/view/dashboard/index.vue:48-127`（脚本与图表 option 内联）、`:132-192`（4 张 KPI 卡片重复模板） |
| 根因 | 数据看板为近期新增，直接在一页组件内平铺所有板块，未像 users/foods 那样拆分可复用块 |
| 影响 | 单文件职责过多、可读性可测性下降；新增 KPI 或趋势图继续线性膨胀，重蹈已被治理的 God 组件覆辙 |
| 修复建议 | 抽 `StatCard` 卡片子组件（props: icon/label/value/sub/suffix/gradient）替换 4 段重复模板；趋势图/TOP10 表格拆独立 section；数据加载下沉 `useDashboardStats` |

### DESIGN-特征依恋-001 — DAU/MAU 粘性比计算耦合在视图组件

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 特征依恋 |
| 严重度 | 中 |
| 症状 | 粘性比（今日 DAU / 近 30 天 MAU，封顶 100，`mau<=0` 兜底 0）直接写在视图组件的 `computed` 中，与展示混合；业务口径散为独立 `computed`，无法被单测或其它复用点（导出/告警）共享 |
| 证据 | `apps/zhenxinjian-front/src/view/dashboard/index.vue:32-37`（`stickiness = computed(() => Math.min((overview.value!.todayDau / mau) * 100, 100))`） |
| 根因 | `api/stats.ts` 只承载 DTO 与请求函数，指标口径被就地实现 |
| 影响 | 口径无法单测；封顶阈值、MAU 窗口调整须进组件改 |
| 修复建议 | `computeStickiness(overview)` 收敛到 stats 工具层（如 `utils/stats.ts`），返回 `{ value, hasData }` 语义化结构，组件仅渲染 |

### DESIGN-重复-002 — 时段→餐别映射双份实现

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 重复代码（霰弹式修改） |
| 严重度 | 中 |
| 症状 | `defaultMealType()`（05–10早/10–15午/15–20:30晚/其余加餐）在 `record/add.vue` 与 `record/recognize.vue` 两文件逐字重复实现 |
| 证据 | `apps/zhenxinjian-uniapp/src/pages/record/add.vue:42` 与 `apps/zhenxinjian-uniapp/src/pages/record/recognize.vue:37` |
| 根因 | 时段→餐别业务规则未收敛公共工具 |
| 影响 | 规则调整须改两处，存在漂移风险 |
| 修复建议 | 提取到 `utils/format.ts` 或 `config/constants.ts`，两页共用 |

### DESIGN-重复-003 — 食物每 100g 换算三处各自实现

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 重复代码 |
| 严重度 | 中 |
| 症状 | 「每 100g × 克数 ÷ 100」换算 + 四舍五入在 `record/add.vue`（foodPreview）、`record/recognize.vue`（scale）、`store/food.ts`（localCalc）三处各自实现，精度口径（1 位 vs 2 位、kcal 取整）不一致风险 |
| 证据 | `apps/zhenxinjian-uniapp/src/pages/record/add.vue:71-80`、`pages/record/recognize.vue:103-111`、`store/food.ts:168-179` |
| 根因 | 换算逻辑本属单一业务规则，未提取纯函数 |
| 影响 | 重复代码；精度口径三处漂移 |
| 修复建议 | 统一走 `foodStore.calc()`，或提取 `utils/macro.ts` 纯函数 `scalePer100g()` 供预览复用 |

### DESIGN-重复-004 — 管理后台食物弹窗表单与校验高度重复

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 重复代码 |
| 严重度 | 中 |
| 症状 | `FoodEditDialog` 与 `FoodAuditFixDialog` 的表单字段（name/categoryCode/alias/carb/protein/fat/kcal/serving）与 `rules`（name/categoryCode/carb/protein/fat/kcal 六项必填 + 长度）几乎逐字相同，`resetForm`/`open` 赋值/`handleSubmit` 校验骨架亦重复，仅上限数值与 payload 组装略有差异 |
| 证据 | `apps/zhenxinjian-front/src/view/foods/components/FoodEditDialog.vue:19-53` vs `apps/zhenxinjian-front/src/view/foods/components/FoodAuditFixDialog.vue:22-45` |
| 根因 | 两个弹窗从同一列表页拆出后未进一步抽象公共表单模型 |
| 影响 | 修改食物字段须双份同步，易漏改造成校验不一致 |
| 修复建议 | 抽共享食物宏量字段定义/校验规则工厂（如 `composables/useFoodForm.ts`），两份弹窗只保留差异 |

### DESIGN-特征依恋-002 — 响应式副作用写在 computed getter 内

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 特征依恋（副作用非纯函数） |
| 严重度 | 中 |
| 症状 | `manualValid` 作为 `computed`，其 getter 内直接写 `manualErr.value = ''`（副作用赋值），用于清除错误提示 |
| 证据 | `apps/zhenxinjian-uniapp/src/pages/record/add.vue:91-113`（`computed(() => { manualErr.value = '' …; return ... })`） |
| 根因 | 用 computed 承载输入校验+副作用，违背 computed 纯计算语义 |
| 影响 | 响应式依赖追踪下触发时机不可预期，错误提示状态不稳定（被 computed 重求值覆盖） |
| 修复建议 | 改为显式 `validateManual()` 函数，在提交/输入时主动调用并写 `manualErr` |

### DESIGN-死代码-001 — MEAL_EMOJI 常量全库零引用

| 字段 | 内容 |
| --- | --- |
| 级别 | DESIGN |
| 类别 | 冗余（死代码） |
| 严重度 | 低 |
| 症状 | `MEAL_EMOJI`（`{1:'🍳',2:'🍱',3:'🌙',4:'🍎'}`）全库仅定义处出现，餐别图标已整体迁移到 SVG（home/record 页的 `MEAL_ICONS`），emoji 口径已废弃 |
| 证据 | `apps/zhenxinjian-uniapp/src/config/constants.ts:81` |
| 根因 | 餐别图标迁移 SVG 后，emoji 常量未清理 |
| 影响 | 死代码误导后续开发者使用废弃的 emoji 口径 |
| 修复建议 | 删除该常量 |

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：DESIGN 级首次全项目扫描，检出 9 条（高 2 / 中 6 / 低 1） |
| v1.1 | 2026-10-08 | — | DESIGN-上帝组件-001（dashboard God 组件）经 DESIGN-T03 拆分，标记已消除 |
| v1.2 | 2026-10-08 | — | DESIGN-特征依恋-001（粘性比计算耦合视图）经 DESIGN-T04 下沉 utils/stats.ts，标记已消除 |
| v1.3 | 2026-10-08 | — | DESIGN-重复-002（时段→餐别双份实现）经 DESIGN-T05 收敛 utils/format.ts，标记已消除 |
| v1.4 | 2026-10-08 | — | DESIGN-重复-003（每 100g 换算三处实现）经 DESIGN-T06 收敛 utils/macro.ts，标记已消除 |
| v1.5 | 2026-10-08 | — | DESIGN-重复-004（食物弹窗表单重复）经 DESIGN-T07 收敛 view/foods/foodForm.ts，标记已消除 |
| v1.6 | 2026-10-08 | — | DESIGN-特征依恋-002（computed 内副作用）经 DESIGN-T08 改显式 `validateManual()`，标记已消除 |
| v1.7 | 2026-10-08 | — | DESIGN-死代码-001（MEAL_EMOJI 死常量）经 DESIGN-T09 删除，标记已消除 |
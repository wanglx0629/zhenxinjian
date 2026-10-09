# Tasks — 2026-09-30-full-project-scan（IMPL 实现级）

- **级别**：IMPL（实现级）
- **依据坏味道版本**：badsmells.md v1.0（提交版本 `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`）
- **生成方式**：mole:plan 全量生成（15 条未清除 → 15 个任务）
- **执行粒度**：每次 `mole:apply` 仅处理一个未完成任务
- **回归命令基线**：后端 `set JAVA_HOME=D:\App\java\25 && mvn -q test`；小程序 `npm run check`（vue-tsc）；管理后台 `npm run build`

---

## §3 任务清单

| 任务 | 追溯 | 标题 | 严重度 | 状态 |
| --- | --- | --- | --- | --- |
| IMPL-T01 | IMPL-安全-001 | `.last("LIMIT "+var)` SQL 拼接治理 | 高 | 已完成 |
| IMPL-T02 | IMPL-安全-002 | pageUsers 用户脱敏统一 | 中 | 已完成 |
| IMPL-T03 | IMPL-语言惯用法-001 | `.setSql` 列名硬编码收敛 | 中 | 已完成 |
| IMPL-T04 | IMPL-语言惯用法-002 | MySQL 方言编号生成可移植化 | 中 | 已完成 |
| IMPL-T05 | IMPL-语言惯用法-003 | 角色静默降级改白名单校验 | 中 | 已完成 |
| IMPL-T06 | IMPL-语言惯用法-004 | 时间字段填充策略统一 | 中 | 已完成 |
| IMPL-T07 | IMPL-语言惯用法-005 | 缓存值体积估算精准化 | 低 | 未开始 |
| IMPL-T08 | IMPL-魔法数字-001 | 容量/阈值魔法数字收敛 | 中 | 未开始 |
| IMPL-T09 | IMPL-异常-001 | 空 catch 吞异常补日志 | 低 | 未开始 |
| IMPL-T10 | IMPL-空值-001 | 枚举 of 返回 null 改 Optional | 低 | 未开始 |
| IMPL-T11 | IMPL-硬编码-001 | 管理后台色值收敛 CHART_PALETTE | 中 | 未开始 |
| IMPL-T12 | IMPL-硬编码-002 | 小程序品牌色建立 TS 色板 | 中 | 未开始 |
| IMPL-T13 | IMPL-参数-001 | picker 事件 any 类型窄化 | 低 | 未开始 |
| IMPL-T14 | IMPL-类型-001 | 列表页 query 绑定 PageQueryBase | 低 | 未开始 |
| IMPL-T15 | IMPL-死代码-001 | store isLoggedIn 死代码清理 | 低 | 未开始 |

状态说明：**未开始** / 进行中 / 已完成。共 15 个任务（高 1 / 中 8 / 低 6）。

---

## §3.1 任务步骤（IMPL 6 步：确认→补测→测绿→重构(语言感知)→回归→用户确认）

### IMPL-T01 `.last("LIMIT "+var)` SQL 拼接治理

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `ReminderService:82`、`WeightService:124`、`AdminStatsService:118,146`、`AdminDietService:76`、`CustomFoodService:111`、`GuestCleanupBatchExecutor:46` |
| ② 补测 | 补 WeightService.list limit 上界/边界测试 |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | `WeightController` limit 加 `@Max` 或 service 封顶；有界场景收敛 `boundedSelect(wrapper, limit)` 封装 |
| ⑤ 回归测绿 | `set JAVA_HOME=D:\App\java\25 && mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T02 pageUsers 用户脱敏统一

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `UserServiceImpl:129-130` 不脱敏、`:263-267` mask 未处理 phone |
| ② 补测 | 补 pageUsers 脱敏断言测试 |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 抽 `toUserVO(User, boolean masked)` 工厂，pageUsers/getUserById 统一按 admin 判定脱敏，默认隐藏 phone |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T03 `.setSql` 列名硬编码收敛

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `ReminderService:151,167` `.setSql("subscribe_credit = subscribe_credit + 1/- 1")` |
| ② 补测 | 补 credit 自增/自减行为测试 |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 列名抽常量；或改独立 mapper increment update |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T04 MySQL 方言编号生成可移植化

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `AdminFoodService:176` `CAST(SUBSTRING(code,2) AS UNSIGNED)` |
| ② 补测 | 补编号生成逻辑测试（当前方言不可测，重构后覆盖） |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 抽 `CodeSequenceService` 用表级/分布式自增；或 mapper XML `databaseId` 区分方言 |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T05 角色静默降级改白名单校验

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `JwtAuthenticationFilter:122-125` 非 ADMIN 一律置 ROLE_USER |
| ② 补测 | 补角色非法值告警/拒绝测试 |
| ③ 测绿 | 现有 `mvn -q test` 通过（含 SecurityStartupCheckerTest） |
| ④ 应用重构手法 | 白名单校验：ADMIN 保持，其余按库值映射，非法值 WARN 并拒绝或走默认 |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T06 时间字段填充策略统一

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `BodyProfileService:146`（setUpdateTime）、`AdminFoodService:121,143`（setSubmitTime）与 MetaObjectHandler 并存 |
| ② 补测 | 确认现有测试覆盖时间字段写入 |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 更新统一经 `updateWrapper.set(updateTime, ...)` 或 Handler 内对非空 updateTime 强制刷新 |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T07 缓存值体积估算精准化

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `CacheValueGuard:28` JSON 串化估算体积 |
| ② 补测 | 补大对象类型校验测试 |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 改字段级估算，或仅对已知大对象类型校验，阈值与 valueEncoder 字节对齐 |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T08 容量/阈值魔法数字收敛

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `ScheduleConfig:47-49`、`GuestCleanupTask:23`、`ReminderService:203-204`（255 截断）等裸值 |
| ② 补测 | 无（纯常量抽取，语义不变） |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 容量数字抽具名常量或 `@ConfigurationProperties`，阈值集中可配 |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T09 空 catch 吞异常补日志

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `WeightService:188-190` `catch (NumberFormatException ignored)` |
| ② 补测 | 无（日志补充，语义不变） |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | 空 catch 补 debug 说明；resolveTrendStart 补 WARN |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T10 枚举 of 返回 null 改 Optional

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `FoodCategoryEnum:91-100` `of` 返回 null |
| ② 补测 | 补 of 未匹配分支测试 |
| ③ 测绿 | 现有 `mvn -q test` 通过 |
| ④ 应用重构手法 | `of` 改 `Optional.ofNullable` 包装，调用方显式处理空态 |
| ⑤ 回归测绿 | `mvn -q test` 全绿 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T11 管理后台色值收敛 CHART_PALETTE

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `dashboard/index.vue:76,85,293-305` 硬编码 `#00AC7C`/`#FFB020` 绕过 `echarts-theme.ts:9` CHART_PALETTE |
| ② 补测 | 无（纯引用替换） |
| ③ 测绿 | front `npm run build` 通过 |
| ④ 应用重构手法 | series 色统一从 `CHART_PALETTE[0]/[1]` 取；渐变复用 CSS 变量或集中 theme 导出 |
| ⑤ 回归测绿 | `cd apps/zhenxinjian-front && npm run build` 零错误 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T12 小程序品牌色建立 TS 色板

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `EnergyRing.vue:28-29`、`constants.ts:87`、`pages.json`、`reminder/index.vue:238` 三处色值 |
| ② 补测 | 无（纯常量收敛） |
| ③ 测绿 | `npm run check` 通过 |
| ④ 应用重构手法 | 建 `BRAND_COLORS` TS 色板，组件/模板 `:color` 引用；pages.json 注释对齐 uni.scss |
| ⑤ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T13 picker 事件 any 类型窄化

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `custom-edit.vue:80,97` `handleEnergyUnitChange(e: any)` 等 |
| ② 补测 | vue-tsc 类型检查即验证 |
| ③ 测绿 | `npm run check` 通过 |
| ④ 应用重构手法 | 改 `e: { detail: { value: number } }` 或抽 `utils/uni-types.ts` 窄化 |
| ⑤ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T14 列表页 query 绑定 PageQueryBase

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `users/index.vue:22-28`、`foods/index.vue:35-44` query 字面量未绑定 `PageQueryBase` |
| ② 补测 | vue-tsc 类型检查即验证 |
| ③ 测绿 | front `npm run build` 通过 |
| ④ 应用重构手法 | 各页定义 `interface XxxListQuery extends PageQueryBase` 显式传入 usePageQuery，淘汰逐个 `as` 断言 |
| ⑤ 回归测绿 | `cd apps/zhenxinjian-front && npm run build` 零错误 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

### IMPL-T15 store isLoggedIn 死代码清理

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `store/user.ts:17,55` `isLoggedIn` 全库零消费 |
| ② 补测 | 无（纯删除或改 getter） |
| ③ 测绿 | front `npm run build` 通过 |
| ④ 应用重构手法 | 若无用途删除；若保留改 `computed(() => !!token.value)` 并接入守卫/MainLayout |
| ⑤ 回归测绿 | `cd apps/zhenxinjian-front && npm run build` 零错误 |
| ⑥ 用户确认 | 展示 diff 获批后提交 |

---

## 执行顺序备注

- IMPL-T01（.last 拼接总治）与 ARCH-T04（WeightService limit 封顶）重叠，建议 IMPL-T01 统一处理，ARCH-T04 作为其上界子项合并执行。
- IMPL-T02/T05/T06/T10/T07/T09 均为后端独立点，互不冲突。
- IMPL-T12/T13 均触小程序 `custom-edit.vue`/`EnergyRing.vue`，可合并一次处理。
- IMPL-T11/T14/T15 均属管理后台，不冲突可并行。

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：IMPL 级 15 条未清除 → 15 个任务 |
| v1.1 | 2026-10-08 | — | IMPL-T04（MySQL 方言编号生成可移植化）：`nextCode` 排序 `CAST(SUBSTRING(...) AS UNSIGNED)` 改 `LENGTH(code) DESC, code DESC` 可移植表达式，新增顺延测试，标记已完成 |
| v1.2 | 2026-10-09 | — | IMPL-T05（角色静默降级改白名单校验）：`JwtAuthenticationFilter` 抽 `normalizeRole` 白名单校验，非法值 WARN 后安全降级 USER，新增非法角色降级测试，标记已完成 |
| v1.3 | 2026-10-09 | — | IMPL-T06（时间字段填充策略统一）：`MetaObjectHandler.updateFill` 改 `setFieldValByName` 强制刷新 updateTime，删除 `BodyProfileService` 手动 `setUpdateTime`，标记已完成 |
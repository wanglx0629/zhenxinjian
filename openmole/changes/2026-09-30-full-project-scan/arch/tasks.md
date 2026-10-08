# Tasks — 2026-09-30-full-project-scan（ARCH 架构级）

- **级别**：ARCH（架构级）
- **依据坏味道版本**：badsmells.md v1.0（提交版本 `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`）
- **生成方式**：mole:plan 全量生成（4 条未清除 → 4 个任务）
- **执行粒度**：每次 `mole:apply` 仅处理一个未完成任务
- **回归命令基线**：后端 `set JAVA_HOME=D:\App\java\25 && mvn -q test`（apps/zhenxinjian-backend）；小程序 `npm run check`（apps/zhenxinjian-uniapp，vue-tsc）；管理后台 `npm run build`（apps/zhenxinjian-front，vue-tsc + vite）

---

## §3 任务清单

| 任务 | 追溯 | 标题 | 严重度 | 状态 |
| --- | --- | --- | --- | --- |
| ARCH-T01 | ARCH-层次-001 | 小程序 taper 页写接口下沉 store | 高 | 已完成 |
| ARCH-T02 | ARCH-内聚-001 | EnergyRing 跨端几何/令牌收敛共享 | 中 | 已完成 |
| ARCH-T03 | ARCH-层次-002 | 管理后台配置值类型字典单一真源 | 中 | 已完成 |
| ARCH-T04 | ARCH-边界-001 | WeightService.list limit 加上界封顶 | 中 | 已完成 |

状态说明：**未开始** / 进行中 / 已完成。共 4 个任务（高 1 / 中 3 / 低 0）。

---

## §3.1 任务步骤（ARCH 8 步：确认→影响分析→安全网→模式→迁移计划(可豁免)→增量执行→回归→用户确认）

### ARCH-T01 小程序 taper 页写接口下沉 store

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `pages/taper/plan.vue:10,65` 直调 `api/taper.ts` 的 `generateTaper532Plan`，而 `store/taper.ts` 仅封 `fetch()` |
| ② 影响分析 | 写路径绕过 store，生成后须手工 `fetch()` 回流；影响 P08 计划生成链路与分层一致性 |
| ③ 测试安全网 | 现有 `npm run check`（vue-tsc）为安全网；确认 taper 生成无既有单测可补 |
| ④ 选择架构模式 | 页面→store→api 分层：写接口下沉 store action 内部自动回流 |
| ⑤ 迁移计划 | 可豁免（单文件小改） |
| ⑥ 增量执行 | `store/taper.ts` 新增 `generate()` action（调 `generateTaper532Plan` + 回流 `fetch()`）；`plan.vue` 改调 `taperStore.generate()` |
| ⑦ 回归测绿 | `cd apps/zhenxinjian-uniapp && npm run check` 零错误 |
| ⑧ 用户确认 | 展示 diff 获批后提交 |

### ARCH-T02 EnergyRing 跨端几何/令牌收敛共享

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `front/src/component/EnergyRing.vue:63,73-74,93-94` 与 `uniapp/src/components/EnergyRing.vue:28-31,96-102` 环几何/令牌同源 |
| ② 影响分析 | 环参数/渐变/阈值双端独立维护，改口径须双端同步 |
| ③ 测试安全网 | 双端 `npm run check` / `npm run build` 为安全网 |
| ④ 选择架构模式 | 共享纯计算层 + 两端窄渲染适配（跨端共享常量/纯函数） |
| ⑤ 迁移计划 | 可豁免（抽取常量 + 纯函数） |
| ⑥ 增量执行 | 抽共享 `r/viewBox/LEAF/AMBER` 常量与 pct/over 归一化纯函数；两端组件改引用，保留 SVG 直绘 vs data-uri 差异 |
| ⑦ 回归测绿 | uniapp `npm run check` + front `npm run build` 双绿 |
| ⑧ 用户确认 | 展示 diff 获批后提交 |

### ARCH-T03 管理后台配置值类型字典单一真源

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `api/adminConfig.ts:53-64` 常量 + `view/configs/index.vue:186-190` 五条 el-option 硬编码 |
| ② 影响分析 | 后端 `ConfigValueTypeEnum` 码变更须三处同步，破坏字典单一真源 |
| ③ 测试安全网 | front `npm run build`（vue-tsc）为安全网 |
| ④ 选择架构模式 | 字典层单一真源：`constants/dicts.ts` 统一承载 |
| ⑤ 迁移计划 | 可豁免（单页 + 单 api 文件） |
| ⑥ 增量执行 | `constants/dicts.ts` 新增 `CONFIG_VALUE_TYPE_OPTIONS/MAP`；`configs/index.vue` 与 `adminConfig.ts` 统一引用，删内联常量与硬编码 option |
| ⑦ 回归测绿 | `cd apps/zhenxinjian-front && npm run build` 零错误 |
| ⑧ 用户确认 | 展示 diff 获批后提交 |

### ARCH-T04 WeightService.list limit 加上界封顶

| 步骤 | 内容 |
| --- | --- |
| ① 确认坏味道 | 复核 `WeightController.java:50` limit 无 `@Max`、`WeightService.java:123-124` 直接 `.last("LIMIT "+limit)` |
| ② 影响分析 | 违反 AGENTS.md「无上限 selectList」Never 条款；limit 告警失控可拖垮 DB |
| ③ 测试安全网 | 后端 `mvn -q test` 为安全网；补 limit 边界测试 |
| ④ 选择架构模式 | 对齐 `AdminStatsService.checkLimit` 模式（有界校验 + 常量封顶） |
| ⑤ 迁移计划 | 可豁免（单方法 + 单 controller） |
| ⑥ 增量执行 | controller 加 `@Max` 或 service 加 `WIGHT_LIST_LIMIT_MAX` 常量封顶；`.last` 拼接收敛受控 |
| ⑦ 回归测绿 | `set JAVA_HOME=D:\App\java\25 && mvn -q test` 全绿 |
| ⑧ 用户确认 | 展示 diff 获批后提交 |

---

## 执行顺序备注

- 四任务无硬依赖，可独立执行。
- ARCH-T01 与 DESIGN-特征依恋/重复类任务不冲突，但 ARCH-T01 属架构分层，宜先于小程序端 DESIGN 任务执行。
- ARCH-T04 与 IMPL-安全-001（`.last` 拼接总治）同触 `.last` 议题，建议 ARCH-T04 先行或合并评估。

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：ARCH 级 4 条未清除 → 4 个任务 |
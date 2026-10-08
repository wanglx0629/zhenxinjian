# Badsmells — 2026-09-30-full-project-scan（ARCH 架构级）

- **级别**：ARCH（架构级）
- **范围**：仓库全量（后端 + 小程序 + 管理后台三端）
- **提交版本**：`4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`
- **识别方式**：三端三路并行静态侦察（后端 Java / 小程序 UniApp / 管理后台 Vue3），交叉复核上一轮已清零条目是否回漂；高危证据逐条核验行号
- **检出语言**：Java、TypeScript、Vue SFC

---

## §2.0 索引

| BS-ID | 类别 | 严重度 | 状态 |
| --- | --- | --- | --- |
| ARCH-层次-001 | 层次 | 高 | 已消除 |
| ARCH-内聚-001 | 内聚 | 中 | 已消除 |
| ARCH-层次-002 | 层次 | 中 | 已消除 |
| ARCH-边界-001 | 边界 | 中 | 已消除 |

状态说明：**未清除** / 已消除 / 部分残余。共 4 条（高 1 / 中 3 / 低 0）。

---

## §2.1 明细

### ARCH-层次-001 — 小程序 taper 页绕过 store 直调写 API（读写分层不一致）

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 层次 |
| 严重度 | 高 |
| 症状 | P08 计划页读路径走 store（`taperStore.fetch()`），写路径（生成 532 计划）却绕过 store 直调 `api/taper.ts` 的 `generateTaper532Plan()`；`store/taper.ts` 只封了读 `fetch()`，没有封写 `generate()`，同一页面两套分层 |
| 证据 | `apps/zhenxinjian-uniapp/src/pages/taper/plan.vue:10`（`import { generateTaper532Plan } from '@/api/taper'`）、`:65`（`await generateTaper532Plan()`）vs `apps/zhenxinjian-uniapp/src/store/taper.ts:16-20`（仅有 `fetch()`） |
| 根因 | 已有 taper store 却只沉淀了读状态，写接口未下沉 |
| 影响 | 生成成功后须手工 `await taperStore.fetch()` 回流（plan.vue:67），遗漏即 store 状态滞后；业务写与表现层耦合，违背「pages→store→api」分层约定（上轮 B-T06 刚收敛过该约定） |
| 修复建议 | `useTaperStore` 新增 `generate()` action（内部调 `generateTaper532Plan` 并回流 `fetch()`），页面只调 `taperStore.generate()` |

### ARCH-内聚-001 — EnergyRing 能量环跨端复制（仅渲染层分叉）

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚（兼演进） |
| 严重度 | 中 |
| 症状 | 管理后台 `EnergyRing.vue`（Web 内联 SVG）与小程序 `components/EnergyRing.vue`（SVG data-uri）的环几何与渐变逐行同源：`viewBox=120`、`r=52`、`C=2πr`、`stroke-linecap=round`、`rotate(-90 60 60)`、绿→琥珀渐变（`#00AC7C`→`#FFB020`）完全一致，仅 SVG 渲染手段不同 |
| 证据 | `apps/zhenxinjian-front/src/component/EnergyRing.vue:63`（`viewBox="0 0 120 120"`）、`:73-74`（渐变 stop）、`:93-94`（linecap/rotate）vs `apps/zhenxinjian-uniapp/src/components/EnergyRing.vue:28-29`（`LEAF='#00AC7C'`/`AMBER='#FFB020'`）、`:31`（`viewBox=120`）、`:96-102`（linecap/rotate/渐变） |
| 根因 | 小程序不支持内联 SVG，迫使渲染层分叉，但环几何/令牌/超标阈值本可下沉共享，未复用 |
| 影响 | 环参数、渐变、超标阈值（`>100`）已出现两处独立维护，改口径须双端同步（霰弹式修改）；注释声明「同源同语义」却仍物理复制 |
| 修复建议 | 抽出共享纯计算层（环几何/令牌/pct·over 归一化），两端仅保留窄渲染适配；至少把 `r=52/viewBox=120/LEAF/AMBER` 收敛为共享常量 |

### ARCH-层次-002 — 管理后台字典单一真源被绕过（配置值类型三处表征）

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 层次 |
| 严重度 | 中 |
| 症状 | 上轮 B-T23 已建 `constants/dicts.ts` 作为「与后端枚举一一对应的字典层」，但新增的「配置值类型」枚举（1字符串/2数字/3布尔/4JSON/5密文）却定义在 `api/adminConfig.ts`，且 `configs/index.vue` 的 `<el-select>` 又硬编码同一份五条 `<el-option>`，未复用任何已有字典能力 |
| 证据 | `apps/zhenxinjian-front/src/api/adminConfig.ts:53-59`（`CONFIG_VALUE_TYPE_OPTIONS`）、`:62-64`（`valueTypeLabel` 兜底）vs `apps/zhenxinjian-front/src/view/configs/index.vue:186-190`（五条 `el-option` 硬编码） |
| 根因 | 新页面未遵循已建立的字典层规约，枚举选项就近塞进 api 文件 + 模板局部硬编码 |
| 影响 | 后端 `ConfigValueTypeEnum` 码变更须三处同步；`dictMap`/`dictLabel` 既有能力被闲置，破坏「字典单一真源」演进方向 |
| 修复建议 | 在 `constants/dicts.ts` 新增 `CONFIG_VALUE_TYPE_OPTIONS/MAP`，`configs/index.vue` 与 `api/adminConfig.ts` 统一引用，删除 api 层内联常量与模板硬编码 |

### ARCH-边界-001 — WeightService.list limit 无上界直拼 SQL

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 中 |
| 症状 | 体重记录查询接口的 `limit` 参数无上界校验，直接 `query.last("LIMIT " + limit)`；`limit` 来自 `@RequestParam(required=false) Integer limit`，未加 `@Max` 也未在 service 内封顶，可传 `Integer.MAX_VALUE` |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/controller/WeightController.java:50`（`@RequestParam(required = false) Integer limit`）→ `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/WeightService.java:123-124`（`if (limit != null && limit > 0) query.last("LIMIT " + limit)`） |
| 根因 | 新功能（体重趋势）沿用了 `.last()` 快捷限流，未与既有 `checkLimit` 模式（AdminStatsService 的 1..50 封顶）保持一致 |
| 影响 | 违反 AGENTS.md「无上限 selectList」Never 条款；`limit` 上限失控时单次全表扫描可拖垮 DB |
| 修复建议 | 对齐 `AdminStatsService.checkLimit` 模式：controller 加 `@Max` 或 service 内加 `WIGHT_LIST_LIMIT_MAX` 常量封顶，并把 `.last("LIMIT "+n)` 收敛为受控拼接或分页插件 |

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：三端 ARCH 复查（含上轮已清零条目回漂检查），检出 4 条（高 1 / 中 3） |
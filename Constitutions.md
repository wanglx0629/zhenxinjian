# Constitutions.md — 员工投资申报系统（ygtz）

> 本项目的最高原则。所有 AI Agent 与协作者**必须**遵守本文件。

## 1. 上下文（Context）

- **项目名称**：员工投资申报系统（ygtz）— 工作空间别名 `tzsb-all`。
- **目标**：为员工提供投资申报的移动端 H5 申报入口，并提供 PC 管理后台进行合规审核与数据管理，后端提供统一数据服务。
- **关键干系人**：申报员工（移动端用户）、合规审核人员（PC 后台）、后台数据维护人员、监管/合规部门。
- **仓库形态**：Monorepo 聚合，根目录下 `apps/` 内含四个子项目：
  - `apps/emp-shr-mobile`：移动端 H5（Vue 3 + Vite + Pinia + Vant 4），**维护中**。
  - `apps/emp-shr-web`：PC 管理后台（Vue 2 + Webpack 3 + Vuex + Element UI），**维护中**。
  - `apps/emp-shr-backend`：Java 后端服务（Spring Boot + MyBatis-Plus + Oracle），**维护中**。
  - `apps/emp-shr-backend.wiki`：子项目技术文档仓库。
- 工作区已有大量既有文档，见 `docsFile/`（含 PROJECT_CONFIG.yml、权限设计、代码审查报告等）与 `apps/emp-shr-backend.wiki/`。
- 团队流程约束（日周月报规则）见 `RULES.md`，编码规范与后端分层规范见 `AGENT.md`。

## 2. 技术栈（Tech Stack）

| 层 | 选择 |
|----|------|
| 前端（移动端） | Vue 3.4 + Vite 5 + Pinia + Vant 4 + vue-router 4 + axios（rem 适配：amfe-flexible + postcss-pxtorem） |
| 前端（PC 后台） | Vue 2.5 + Webpack 3 + Vuex + Element UI 2.15 + ECharts（老项目，仅维护） |
| 后端 | Java + Spring Boot 2.1.15 + Maven 多模块（`gjzqDbGlf-Service`）+ MyBatis-Plus + 多数据源 |
| 数据库 | Oracle（含层级查询 `connect by` / `sys_connect_by_path`） |
| 缓存 | 未检测到（默认 None，如引入 Redis 需先记录 ADR） |
| 部署 | 内网环境部署（详见 `docsFile/6.上线手册模板/`），未采用容器化 |
| 包管理 | npm（前端两个子项目）、Maven（后端） |

## 3. 架构分层（Architecture Layers）

**后端（`com.gjzq`）**：分层 MVC 架构。

- 模块划分：`modules/{app,com,eboss,glr,glrba,glrlc,hr,job,mobile,ods,sys,task,th,tool,wehcat,zg}`。
- 分层职责：**Controller**（HTTP 适配，不写业务）→ **Service 接口 + ServiceImpl**（业务编排/校验/事务）→ **Mapper**（数据访问，复杂查询在 SQL 完成）→ **Entity/DTO/VO**。
- 公共设施：`common/`（annotation、aspect、mvc、exception、validator、xss、encWapper）、`datasource/`（多数据源）、`config/`、`template/`。
- **两套体系并存**：新模块使用新 MVC 体系（`PageRequest<T>`/`PageResponse<T>` + 类型化 DTO/VO）；老模块保留 `R` + `Map<String,Object>` + `PageUtils` 旧体系。**新代码一律采用新体系，不混用**（详见 `AGENT.md` 后端分层架构节）。

**前端**：按路由/业务模块组织（`src/views` 页面 + `src/api` 接口 + `src/components` 组件 + 状态管理 store）。

## 4. 工具状态（Tooling Status）

| 工具 | 状态 | 安装命令 |
|------|------|---------|
| OpenSpec CLI | ✅ 已安装（1.8.0） | `npm install -g @fission-ai/openspec` |
| Superpowers | ✅ 可用（`.agents/skills/superpowers-zh/`） | 按 agentic 工具机制安装 |
| OpenMole（BDR 重构） | ✅ 已安装（0.9.0） | `npm install -g openmole` |
| Khufu（khufu-kit） | ✅ 已安装（0.4.0），配置见 `.khufu/khufu.yaml` | `npm install -g khufu-kit` |

## 5. SDD 双循环开发流程与门禁（SDD Dual-Loop Development Process & Gates）

> 开发流程是严格的、带门禁的双循环。每一步**必须**完整完成并经用户确认后才进入下一步。循环由本宪法检测到的工具驱动（OpenSpec、Superpowers、OpenMole、Khufu）。未安装的工具，以对应人工操作代替。

**门禁规则：** 仅当当前步骤完成**且**用户确认后，才进入下一步。在步骤 **a**，询问用户是否以**自动模式**运行。

### 标准模式（门禁 a → i）
`Qwen3.8-Max` `Doubao-Seed-Evolving` `DeepSeek-V4-Pro 正式版` `GLM-5.3` `Kimi-K3` `Doubao-Seed-Evolving`
- **a. 探索（Explore）** — 在开始任何实现之前，通过 `/opsx:explore` 斜杠指令进入探索模式。给定需求描述后，AI 会扮演思维伙伴的角色，深入分析问题背景、梳理多种解决方案并评估各方案的风险与收益。→ **人工评审**（非自动）→ b。（主模型：`Qwen3.8-Max`,备模型：`Doubao-Seed-Evolving`）
- **b. 提议（Propose）** — 运行 OpenSpec `propose`（`openspec propose` / `/opsx:propose`）创建带规划产物的 change（proposal、specs、design、tasks）。→ **人工评审**（非自动）→ c。（主模型：`DeepSeek-V4-Pro 正式版`,备模型：`GLM-5.3`）
- **c. 头脑风暴（Brainstorm）** — 对提案运行 Superpowers **brainstorming**，探索意图、需求与设计备选。→ **人工评审**（非自动）→ d。（主模型：`Kimi-K3`,备模型：`Doubao-Seed-Evolving`）
- **d. 计划（Plan）** — 运行 Superpowers **writing-plans** 将头脑风暴输出转为实施计划。→ **人工评审**（非自动）→ e。（主模型：`Doubao-Seed-Evolving`,备模型：`Qwen3.8-Max`）
- **e. 执行（Execute）** — 选择 Superpowers 执行方式（**SubAgent** 或 **inline**）。询问用户是否启用**原子提交**。所有任务执行完成后，用户确认 → f。（主模型：`GLM-5.3`,备模型：`DeepSeek-V4-Pro 正式版`）
- **f. 测试金字塔（Test pyramid）** — 依次运行：`khufu-ut` → `khufu-it` → `khufu-api` → `khufu-e2e`。**可选** — 用户可跳过。确认或跳过 → g。（主模型：`DeepSeek-V4-Pro`,备模型：`GLM-5.3`）
- **g. 重构（BDR）** — 询问用户是否按 **OpenMole BDR**（Big Deal Refactoring）要求重构既有代码。**可选** — 用户可跳过。确认或跳过 → h。确认后按 BDR 全流程命令依次执行：（主模型：`DeepSeek-V4-Pro 正式版`,备模型：`Doubao-Seed-Evolving`）
  - **g1. `/openmole-explore`** — 识别坏味道（级别：ARCH / DESIGN / IMPL），产出 `badsmells.md`。
  - **g2. `/openmole-plan`** — 将「未清除 / 部分残余」坏味道拆解为任务（B-Txx），产出 `tasks.md`。
  - **g3. `/openmole-verify`** — 差分验证 `badsmells.md` 与 `tasks.md` 覆盖一致性。**须在 `plan` 之后、`apply` 之前执行**（发现冲突先改文档，不先改代码）。
  - **g4. `/openmole-apply`** — 执行重构任务，**每次仅一个**，循环直至全部完成；写操作须先展示 diff 并经用户确认（写操作门禁不可豁免）。
  - **g5. `/openmole-archive`** — 检查完成度（badsmells 无未清除、tasks 无 `[ ]`）并归档 change 至 `openmole/changes/archive/`。
  - 执行序：`explore → plan → verify → apply（循环）→ archive`。→ 全部完成后 → h。
- **h. 验证（Verify）** — 运行 OpenSpec `verify`（`openspec verify` / `/opsx:verify`）。**必须执行。** 用户确认结果 → i。**自动模式下**，若验证报告问题，自动修复并重新验证。（主模型：`DeepSeek-V4-Pro 正式版`,备模型：`GLM-5.3`）
- **i. 归档（Archive）** — 运行 OpenSpec `archive`（`openspec archive` / `/opsx:archive`）归档已完成 change 并合并 spec 更新。**必须执行。**（主模型：`DeepSeek-V4-Pro 正式版`,备模型：`GLM-5.3`）

### 自动模式（a → b → c → d → e → h → i）

当用户在步骤 **a** 选择**自动模式**时，循环无需逐步人工评审自动运行，使用精简序列：

`a（explore）` → `b（propose）` → `c（brainstorm）` → `d（plan）` → `e（execute）` → `h（verify，自动修复问题）` → `i（archive）`。

步骤 **f**（测试金字塔）与 **g**（BDR 重构）在自动模式下**跳过**。

# Constitutions.md — 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档名称 | 臻心减项目宪法（Constitutions） |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 适用范围 | 本文档为项目最高原则，所有 AI Agent（Trae / Claude Code / Codex / OpenCode 等）与人类贡献者**必须**遵守 |

> **文档定位**：本项目采用 SDD（Spec-Driven Development，规格驱动开发）与 Harness Engineering（驾驭工程）方法论。本文件是全部约束资产的最高真源；下层资产（`docs/harness/`、`docs/knowledge/`、`docs/prd/`）均不得与本文件冲突。

***

## 目录

1. [项目上下文](#1-项目上下文)
2. [技术栈](#2-技术栈)
3. [架构分层](#3-架构分层)
4. [工具状态](#4-工具状态)
5. [硬性边界（Always / Never）](#5-硬性边界always--never)
6. [SDD 双循环开发流程与门禁](#6-sdd-双循环开发流程与门禁)

***

## 1. 项目上下文

**臻心减**是一款帮助健身新人小白开启生活化减脂第一步的**微信小程序**（私人陪伴式互联网健身搭子），由 create-luote 脚手架生成的三端工程承载：

- **用户端小程序**（`apps/zhenxinjian-uniapp`）：面向 C 端用户，核心能力为「计算 + 食物库 + 记录 + 提醒」，一套代码可发布 H5 / 微信小程序。
- **PC 运营管理后台**（`apps/zhenxinjian-front`）：面向运营人员，用户管理、食物库维护、数据查看。
- **后端服务**（`apps/zhenxinjian-backend`）：统一提供 REST API、认证、核心计算、数据持久化。

**目标**：把 BMR/TDEE/宏量计算、532 碳水渐降与碳循环两种减脂模式、食物库与饮食记录封装成「零学习成本」的工具。

**干系人**：1 名产品经理 + 1 名开发；项目节奏无硬性时间要求，以打磨产品体验为核心。

**需求真源**（优先级从高到低）：

1. `MRD-PRD/臻心减-V1.1-最终版PRD-开发交付版.docx`（9/3 图片公式定稿，开发交付唯一口径）
2. `MRD-PRD/臻心减_PRD_V1.1_含食物库.docx`（数据模型、埋点、食物库规则）
3. `MRD-PRD/臻心减小程序V1.1高保真原型/`（16 页可交互原型，P01–P16 / F01–F27）
4. `doscFile/`（项目介绍 / 技术栈说明 / 开发规范）

> 任何需求变更须由 PM 更新 PRD 后同步至 [docs/prd/](./docs/prd/README.md)，代码实现以最新 PRD 与原型为准。

***

## 2. 技术栈

| 层 | 选型 |
|----|------|
| 用户端 | UniApp 3（Vue 3 + TypeScript 4.9 + Vite 5 + Pinia + Sass），主发微信小程序 |
| 管理后台 | Vue 3.4 + TypeScript 5.4 + Vite 5 + Element Plus 2.7 + Pinia + Axios + ECharts |
| 后端 | Java 17 + Spring Boot 3.4.5（Web / Security / Validation / AOP / WebSocket） |
| ORM | MyBatis-Plus 3.5.6（逻辑删除、自动填充、分页） |
| 数据库 | MySQL 8（utf8mb4 + InnoDB） |
| 缓存 | Redis 5+（Lettuce）+ JetCache 2.7.7（本地 Caffeine + 远程 Redis 两级） |
| 认证 | Spring Security + JJWT 0.12.5；小程序端微信 `openid` 授权登录 |
| 接口文档 | SpringDoc OpenAPI 2.8.8（Swagger UI） |
| 对象存储 | MinIO（自建）/ 阿里云 OSS（默认关闭，按需开启） |
| AI（预留） | Spring AI Alibaba 1.0.0.2（DashScope qwen-plus），一期不做 AI 识物 |
| 包管理 | Maven（后端）/ npm（前端两端） |

> 部署形态为单体应用（jar + Nginx 反向代理），小程序生产环境**必须 HTTPS** 并在微信公众平台配置合法域名。

***

## 3. 架构分层

### 3.1 后端（`apps/zhenxinjian-backend`，根包 `cn.zhenxinjian`）

```
controller → service（接口）→ service/impl（实现）→ mapper
对象模型：po（表映射）/ dto（入参）/ query（查询入参）/ vo（出参）
横切：common（ai / cache / constant / exception / query / result / utils）、config、security、websocket
```

- Controller 只做参数接收与结果返回，**不写业务、不写 SQL**。
- 事务 `@Transactional` 写在 Service 实现类 public 方法；多表一致性写操作必须加事务。
- 依赖方向只允许自上而下；`common` 不依赖业务包。

### 3.2 管理后台（`apps/zhenxinjian-front`）

路由级 feature 模块：`view/<功能>` + `api/<资源>.ts`（一资源一文件）+ `component/`（通用组件）+ `store/`（Pinia）。

### 3.3 用户端小程序（`apps/zhenxinjian-uniapp`）

`pages.json` 声明式路由 + `pages/<页面>` + `api/` + `components/` + `store/`；跨端优先使用 `uni.*` API，禁止直接操作浏览器 DOM。

***

## 4. 工具状态

| 工具 | 状态 | 说明 / 安装 |
|------|------|------------|
| OpenSpec CLI | **已安装**（全局） | `openspec/` 工作区已初始化（schema: spec-driven）；安装：`npm install -g @fission-ai/openspec` |
| Superpowers | **可用** | brainstorming / writing-plans / executing-plans 等技能已在 Agent 技能列表中 |
| OpenMole（BDR） | **已安装** v0.9.0 | `openmole/` 工作区已初始化；安装：`npm install -g openmole` |
| Khufu（khufu-kit） | **已安装** | `.khufu/khufu.yaml` 已配置：UT=JUnit5、IT=spring-boot-test、API=rest-assured、E2E=playwright；UT 覆盖率 line≥80 / branch≥70，IT line≥60 / branch≥50；安装：`npm install -g khufu-kit` |

> ⚠️ **已知缺口**：脚手架 AGENTS.md 引用的 `.agents/skills/luote-scaffold/scripts/gen-crud.js`（CRUD 样板生成脚本）当前**不存在**——`.agents/skills/` 已被 OpenSpec 技能覆盖。在其恢复（可从 create-luote 重新生成）之前，新增 CRUD 按本宪法与 [Java 代码规范](./docs/knowledge/code-standard/java/standard.md) 手写，并遵守其中全部分层/软删/Result 约定。

***

## 5. 硬性边界（Always / Never）

### Always（必须）

- 错误文案统一进 `ExceptionConstant`；接口统一返回 `Result`（`Result.ok` / `Result.fail`）。
- **核心计算逻辑后置到后端**：BMR / TDEE / 碳循环图片公式 / 532 占比 / 体重调碳等一切健康计算，前端仅可做实时预览，**以后端计算为准**，前端参数不可篡改结果。
- 人体数据区间**前后端双重校验**（越界前端标红、后端拒绝）：年龄 12–80、身高 100–250cm、体重 25–200kg、目标体重 25–200kg 且 ≤ 当前体重。
- 碳循环日型除数 `2 / 2.2 / 2` 固定写死，周期 ≠ 7 天按比例放大；**禁止**用 `nH+1` 等动态值重算。
- 营养克数后端按 `double` 存储防累计漂移，前端展示四舍五入到整数克；周期总量守恒允许 ±1g。
- MySQL / Redis 遵守 [开发规范](./doscFile/03-开发规范.md) §4、§5（软删 + 活跃唯一约束、会话 Key 必须 TTL、无 BigKey）。
- 管理端接口加 `@PreAuthorize("hasRole('ADMIN')")`。
- 文件头注释：`作者: luote (luote) - https://luote996.cn`；注释另起一行。

### Never（禁止）

- WebSocket Token 放入 query 参数（只允许子协议 / Authorization）。
- CORS / `Origin` 配置为 `*`。
- 硬编码密钥、JWT、密码（一律走配置 / 环境变量）。
- 无 TTL 的会话类 Redis Key、BigKey、金额用浮点（真实金额必须 `DECIMAL`）、软删表乱加普通 UNIQUE。
- Controller 拼 SQL、`${}` 拼接用户输入、无上限 `selectList` 对外。
- 修改数据库 Schema、核心类型（公共 Entity/DTO/VO）、认证鉴权、CI/CD 配置、删除已 commit 文件、修改 Harness 资产本身——以上高危操作**必须先做 Checkpoint 并经用户确认**。
- 一期范围外功能私自引入：拍照识物 / AI 识物 / 条形码、运动课程 / 社区 / 打卡、付费 / 广告 / 营销、自动体重曲线 / 全自动复盘 / 详细微量元素。

***

## 6. SDD 双循环开发流程与门禁

> 开发流程是严格的、带门禁的双循环。每一步必须**完全完成并经用户确认**后才能开始下一步。循环由本宪法第 4 节检测到的工具（OpenSpec、Superpowers、OpenMole、Khufu）驱动；若某工具未安装，则执行对应的替代手动动作。
>
> **门禁规则**：只有当前步骤完成**且**用户确认后，才允许进入下一步。在步骤 **a** 时，询问用户是否以**自动模式**运行。

### 标准模式（门禁 a → h）

- **a. 提案（Propose）** — 运行 OpenSpec `propose`（`openspec propose` / `/opsx:propose`）创建变更及规划产物（proposal、specs、design、tasks）。→ **人工评审**（非自动模式）→ b。
- **b. 头脑风暴（Brainstorm）** — 对提案运行 Superpowers **brainstorming**，探索意图、需求与设计备选方案。→ **人工评审**（非自动模式）→ c。
- **c. 计划（Plan）** — 运行 Superpowers **writing-plans** 将头脑风暴产出转化为实施计划。→ **人工评审**（非自动模式）→ d。
- **d. 执行（Execute）** — 选择 Superpowers 执行方式（**子代理** 或 **内联**）。询问用户是否启用**原子提交**。全部任务执行完毕后由用户确认 → e。
- **e. 测试金字塔（Test pyramid）** — 依次运行：`khufu-ut` → `khufu-it` → `khufu-api` → `khufu-e2e`。**可选**——用户可跳过。确认或跳过 → f。
- **f. 重构（BDR）** — 请用户按 **OpenMole BDR**（Big Deal Refactoring）要求重构既有代码。**可选**——用户可跳过。确认或跳过 → g。
- **g. 验证（Verify）** — 运行 OpenSpec `verify`（`openspec verify` / `/opsx:verify`）。**强制**。用户确认结果 → h。**自动模式**下若验证报告问题，自动修复并重新验证。
- **h. 归档（Archive）** — 运行 OpenSpec `archive`（`openspec archive` / `/opsx:archive`）归档已完成的变更并合并规格更新。**强制**。

### 自动模式（a → b → c → d → g → h）

当用户在步骤 **a** 选择自动模式时，循环自动运行，无需逐步人工评审，序列缩短为：

`a（提案）` → `b（头脑风暴）` → `c（计划）` → `d（执行）` → `g（验证，自动修复问题）` → `h（归档）`。

步骤 **e**（测试金字塔）与 **f**（BDR 重构）在自动模式下**跳过**。

***

> **文档版本**：V1.0
> **最后更新**：2026-09-04
> **维护**：臻心减项目组

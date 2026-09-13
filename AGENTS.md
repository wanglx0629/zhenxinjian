# AGENTS.md — 臻心减（zhenxinjian）约束资产总图

> 所有 AI Coding Agent（Trae / Claude Code / Codex / OpenCode / Copilot / Gemini 等）的统一入口与导航地图。从这里开始。

| 项目 | 内容 |
| ---- | ---- |
| 产品名称 | 臻心减（微信小程序 · 生活化减脂工具） |
| 文档版本 | V1.0（khufu-harness 生成） |
| 整理日期 | 2026-09-04 |

## 快速开始

- 最高原则（宪法 · SDD 双循环门禁）：[Constitutions.md](./Constitutions.md)
- Harness 约束三件套：[context-package](./docs/harness/context-package/) ·
  [tool-schema](./docs/harness/tool-schema/tool-schema.md) ·
  [eval-set](./docs/harness/eval-set/eval-set.md)
- 知识库：[arch（ADR + 前后端架构）](./docs/knowledge/arch/) ·
  [design-system](./docs/knowledge/design-system/design-system.md) ·
  [quality 测试策略](./docs/knowledge/quality/test-strategy.md) ·
  [code-standard](./docs/knowledge/code-standard/) ·
  [business-rule 不变量](./docs/knowledge/business-rule/invariants.md)
- 需求：[PRD 索引](./docs/prd/README.md)
- 项目自述：[项目介绍](./docsFile/01-项目介绍.md) · [技术栈说明](./docsFile/02-技术栈说明.md) · [开发规范](./docsFile/03-开发规范.md)

## 项目是什么

三端单体工程（代码统一在 `apps/` 目录）：

- `apps/zhenxinjian-backend`：Spring Boot 3.5.16 + Java 25 + MyBatis-Plus + MySQL 8 + Redis/JetCache
- `apps/zhenxinjian-front`：Vue 3 + Element Plus + Vite（PC 运营管理后台）
- `apps/zhenxinjian-uniapp`：UniApp（Vue 3）用户小程序（主发微信小程序，可发 H5）

## 常用命令

```bash
# 后端（端口 8080，上下文 /api；Swagger: /api/swagger-ui.html）
cd apps/zhenxinjian-backend && mvn spring-boot:run

# Web 管理后台（端口 5173，/api 代理到 8080）
cd apps/zhenxinjian-front && npm install && npm run dev

# UniApp H5 / 微信小程序（产物 dist/dev/mp-weixin 用微信开发者工具打开）
cd apps/zhenxinjian-uniapp && npm install && npm run dev:h5
cd apps/zhenxinjian-uniapp && npm run dev:mp-weixin
```

## 新增业务 CRUD

> 新增 CRUD 请按 [Java 代码规范](./docs/knowledge/code-standard/java/standard.md) 与 [开发规范](./docsFile/03-开发规范.md) 手写样板，并完整遵守分层、软删、`Result`/`ExceptionConstant`、活跃唯一约束等约定。

## 硬性边界（摘要，全文见宪法 §5）

### Always

- Git 提交信息：功能开发用 `feature：功能说明`；修 bug 用 `fix：问题说明`（详见 [开发规范 §10](./docsFile/03-开发规范.md)）
- 错误文案进 `ExceptionConstant`；接口返回 `Result`；注释：`作者: wanglx`（另起一行）
- 核心计算逻辑后置后端；人体数据区间前后端双重校验
- MySQL / Redis 遵守 [开发规范](./docsFile/03-开发规范.md) §4、§5
- **建表规约**：所有表必备 `delete_flag`（`@TableLogic` 逻辑删除）；业务表必备 `status` 状态列；列名跨表统一；建表同时必须新建对应字典枚举（`common/enums/`，code+desc+of）

### Never

- WebSocket Token 放 query；CORS / Origin 用 `*`；硬编码密钥、JWT、密码
- 无 TTL 会话 Key、BigKey、金额用浮点、软删表乱加普通 UNIQUE
- Controller 拼 SQL、`${}` 拼接用户输入、无上限 `selectList`
- 私自实现一期范围外功能（AI 识物 / 社交 / 付费等）

## 开发流程

一切变更走 [SDD 双循环门禁](./Constitutions.md#6-sdd-双循环开发流程与门禁)：
`openspec propose` → brainstorm → plan → execute →（可选）khufu 测试金字塔 →（可选）BDR → `openspec verify` → `openspec archive`。

## 如何更新这些资产

运行 `/khufu-harness`：带明确内容（自动分类写入）或不带内容（差异驱动修订，逐项确认）。

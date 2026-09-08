# Design — 环境基线

## Context

见 [proposal.md](./proposal.md) — Why。三端均为脚手架初始状态（业务代码为零），因此本变更的「设计」仅涉及配置决策与入库策略，无运行时行为设计。

## Goals / Non-Goals

**Goals:**

- 三端联调 URL 口径统一，`npm run dev` + `mvn spring-boot:run` 后开箱即通
- 计算口径 TS 资产与 V1.1 文档进入 git 历史，获得可回溯锚点
- 食物库导入数据源唯一化

**Non-Goals:**

- 不引入任何业务代码、建表、接口（属 Change 1+）
- 不做环境变量化改造（本地 dev 固定端口即可，生产部署属后续运维 change）
- 不清理 `MRD-PRD/` 原始档案中的冗余副本（仅声明真源，不删文件）

## Decisions

### D1. 端口统一方向：后端改回 8080，而非其余端点迁就 1215

- **理由**：8080 是 AGENTS.md、开发上手指南、front Vite 代理、uniapp `request.ts` 四处一致的既有口径，仅后端 `application.yml` 一处为 1215（疑似脚手架随机端口）。改一处 vs 改四处，且文档零改动。
- **备选**：全链路改 1215——需同步改 4 处配置/文档，且与既有文档口径冲突，弃。

### D2. 算法层 5 文件连同当前暂存区一并提交，拆两个 commit

- **commit A（文档基线）**：已暂存的 MRD-PRD / docs / doscFile 更新 + `common/ai/` 四个文件删除 + pom/application.yml 的既有改动（升级与 AI 依赖移除）。
- **commit B（口径资产）**：`apps/zhenxinjian-uniapp/src/{utils,config,data}/` 5 个 untracked 文件，commit message 注明「48 项对拍通过，作为后端计算服务对拍基准」。
- **理由**：文档/脚手架调整与口径资产入库是两类性质不同的变更，分开提交使 B 可被单独引用（后续 change 的对拍断言可指向该 commit hash）。

### D3. 食物库真源：`MRD-PRD/foods_200.json（开发导入用）.json`

- **理由**：文件名即标注「开发导入用」；JSON 结构与 `data/foods.ts` 的 FoodItem 字段一致（见 `doscFile/projectFile/04-食物库数据字典.md` 口径 D1–D6），后端可零转换导入。xlsx/csv 为人工预览与原始档案，不参与导入。
- **约束**：后端导入实现属 Change 4（food-library），本变更只做声明。

## Risks / Trade-offs

- [8080 端口被本机其他进程占用] → 启动失败时报错明确（Port already in use），按需临时改端口即可；不预留配置化。
- [commit B 中 foods.ts 与 json 真源存在字段口径漂移] → 04-数据字典已声明二者同源（同一批 200 条），入库前用脚本抽查 10 条核对 kcal/三宏字段一致性。
- [暂存区中 application.yml 既有改动（AI 移除相关）与 D1 端口改动混在同一文件] → 端口改动发生在工作区未暂存部分，`git add` 时按文件级暂存即可，无需拆 hunks。

## Migration Plan

纯本地配置与 git 操作，无部署迁移。回滚 = `git revert` 对应 commit。

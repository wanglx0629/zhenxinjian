# Change 0 — 环境基线（端口统一 + 口径资产入库）

## Why

MVP v1 开发启动前存在三处环境/资产隐患，若不在业务 change 之前清除，将直接阻塞联调并导致计算口径失去版本锚点：

1. **端口冲突**：后端 `application.yml` 实际端口为 **1215**，而 AGENTS.md、开发上手指南、front 的 Vite 代理、uniapp 的 `request.ts` 默认 BASE_URL 均指向 **8080**——三端首次联调必然失败。
2. **算法口径资产未入库**：小程序端 5 个 TS 文件（`calculator.ts` 443 行 / `constants.ts` / `validate.ts` / `format.ts` / `foods.ts` 200 条食物）已通过 48 项对拍，但全部处于 git untracked 状态，随时可能被误改误删且无版本可回溯。这是全项目唯一已完成的计算口径实现，后续后端计算服务需以此为对拍基准。
3. **食物库导入真源未定**：`MRD-PRD/` 下并存多份 200 条食物数据（xlsx / csv / json 若干副本），后端导入时若无唯一真源声明，易发生数据口径漂移。

## What Changes

- **端口统一**：后端 `application.yml` 的 `server.port` 从 1215 改回 **8080**（与全部文档、front 代理、uniapp 默认值对齐，改动面最小）；同步核对 front Vite 代理与 uniapp `BASE_URL` 无需再改。
- **口径资产入库**：将 `apps/zhenxinjian-uniapp/src/` 下的 `utils/calculator.ts`、`utils/validate.ts`、`utils/format.ts`、`config/constants.ts`、`data/foods.ts` 提交入库，作为计算口径的版本锚点。
- **食物库真源声明**：明确 `MRD-PRD/foods_200.json（开发导入用）.json` 为后端 foods 表导入的唯一真源，其余 xlsx/csv 为预览/原始档案。
- 顺带处理：当前工作区中已暂存的 V1.1 需求文档更新（MRD-PRD/、docs/、docsFile/ 等）与已删除的 `common/ai/` 四个文件，随本变更一并提交，使仓库回到「干净基线 + 算法层就绪」状态。

## Capabilities

### New Capabilities

（无——本变更不引入任何规格级行为）

### Modified Capabilities

（无——纯环境配置与资产入库，`skip_specs: true` 已在 `.openspec.yaml` 中设置）

## Impact

- `apps/zhenxinjian-backend/src/main/resources/application.yml`：`server.port` 1215 → 8080
- `apps/zhenxinjian-uniapp/src/{utils,config,data}/`：5 个文件由 untracked 转入库
- Git 工作区：暂存区现有文档变更 + 删除的 AI 校验类一并提交
- 不影响任何运行时业务行为（当前业务代码为零）

# 工具模式（Tool Schema）— AI Agent 可调用工具

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 适用范围 | 所有在本项目工作的 AI Coding Agent（Trae / Claude Code / Codex / OpenCode 等） |

定义 AI Agent 在本项目内可调用的工具、关键参数与权限层级。

| 工具 | 用途 | 关键参数 | 权限层级 |
| ---- | ---- | ---- | ---- |
| Read / Glob / Grep | 只读检索代码与文档 | 路径 / 模式 / 正则 | read |
| Write / Edit | 本地文件写入与编辑（项目范围内） | 路径 / 内容 | write |
| Bash（构建 / 测试） | `mvn spring-boot:run`、`mvn test`、`npm run dev`、`npm run build`、`npm run type-check`、`node` 脚本 | 命令串 | write |
| Bash（git 本地操作） | `git status` / `git diff` / `git add` / `git commit` | 命令串 | write（commit 需用户明确要求） |
| OpenSpec CLI | `openspec propose` / `verify` / `archive` / `list` / `validate` | 子命令 | write |
| OpenMole CLI | `openmole` BDR 重构流程 | 子命令 | write |
| Khufu CLI | `khufu-ut` / `khufu-it` / `khufu-api` / `khufu-e2e` 测试生成 | 技能 / 子命令 | write |
| Bash（网络 / 部署 / 远端） | `npm install`、`git push`、部署脚本、curl 外部服务 | 命令串 | privileged |
| Bash（破坏性） | `rm -rf`、`DROP TABLE`、`git reset --hard`、`git push --force` | 命令串 | privileged |
| 数据库变更 | 修改 Schema / 跑迁移脚本（含 `data.sql`） | SQL / 脚本 | privileged |

### 权限层级

- **read（只读）**：只读检视（Glob、Read、Grep），可自由使用。
- **write（本地写）**：项目范围内本地文件编辑（Write、Edit）与本地构建 / 测试 / 版本控制命令；高危路径（宪法 §5 列出的 Checkpoint 场景）仍需先经用户确认。
- **privileged（特权）**：外部 / 破坏性操作（联网安装依赖、push、部署、删除已 commit 文件、改库、force 系命令）——**必须先向用户确认**，未经确认一律禁止执行。

### 项目附加约束

1. **git 提交**：仅在用户明确要求时创建；不主动 push。
2. **删除操作**：删除 `apps/zhenxinjian.wiki/` 等整目录属破坏性操作，必须逐项确认。
3. **密钥访问**：Agent 不得读取并外传 `.env*`、`application-*.yml` 中真实凭据；文档中只引用变量名。
4. **临时脚本**：分析用临时脚本放 `.khufu/`（如 `_tmp_*.py`），任务结束后清理。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

# AGENTS.md

本文件面向**所有** AI Coding Agent（Cursor / Claude Code / Codex / Copilot / Gemini / Windsurf 等），不绑定某一厂商目录。

作者: luote (luote) · https://luote996.cn

## 项目是什么

create-luote 生成的三端工程（或本脚手架仓）：

- `{name}-backend`：Spring Boot 3
- `{name}-front`：Vue3 + Element Plus
- `{name}-uniapp`：UniApp（H5 + 微信小程序）
- `.agents/skills/luote-scaffold/`：跨 Agent 的任务 Skill + 可执行脚本

## 新增业务 CRUD（必须）

禁止手写整套样板。在项目根执行：

```bash
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知
```

自定义字段 / 端：

```bash
node .agents/skills/luote-scaffold/scripts/gen-crud.js Product --zh 商品 --fields "name:string:名称,price:long:价格分"
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知 --ends backend,front,sql
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知 --dry-run
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知 --no-admin
```

详细约定与触发说明：先读 `.agents/skills/luote-scaffold/SKILL.md`。

## Skill 发现路径

| 路径 | 用途 |
|------|------|
| `AGENTS.md`（本文件） | 全 Agent 通用入口 |
| `.agents/skills/luote-scaffold/` | Agent Skills 规范目录（主真源） |
| `.cursor/skills/luote-scaffold/` | Cursor 兼容镜像（内容与 `.agents` 相同） |

在 **create-luote 脚手架仓**改 Skill 时，只改 `templates/.agents/skills/luote-scaffold/` 与 `templates/AGENTS.md`。

## 硬性边界

### Always

- CRUD 走 `gen-crud.js`
- 注释：`作者: luote (luote) - https://luote996.cn`；注释另起一行
- 错误文案进 `ExceptionConstant`；接口返回 `Result`
- MySQL / Redis 遵守 `.agents/skills/luote-scaffold/mysql.md` 与 `redis.md`

### Never

- WebSocket Token 放入 query
- CORS / Origin 使用 `*`
- 硬编码密钥、JWT、密码
- 绕过脚本手写整套 CRUD（除非用户明确要求）
- 无 TTL 会话 Key、BigKey、金额用浮点、软删表乱加普通 UNIQUE

## 常用命令

```bash
# 后端
cd {name}-backend && mvn spring-boot:run

# Web
cd {name}-front && npm install && npm run dev

# UniApp H5
cd {name}-uniapp && npm install && npm run dev:h5
```

更多规范：`.agents/skills/luote-scaffold/conventions.md`  
MySQL：`.agents/skills/luote-scaffold/mysql.md`  
Redis：`.agents/skills/luote-scaffold/redis.md`  
联调清单：`.agents/skills/luote-scaffold/checklist.md`

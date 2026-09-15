# 项目配置表 + 敏感词过滤 — Proposal

| 项目 | 内容 |
| ---- | ---- |
| Change ID | 2026-09-15-config-sensitive-words |
| 类型 | feature（新基础设施表 + 内容安全组件） |
| 涉及端 | `apps/zhenxinjian-backend` + `apps/zhenxinjian-front`（PC 管理后台） |
| 涉及小程序 | 否（C 端无感知，仅输入被拦截时收到统一错误文案） |
| 关联任务 | 任务#3 敏感词过滤组件；为任务#4（OCR/LLM 的账号密码类配置）预置存储 |

## 1. 背景

- 项目目前所有配置走 `application*.yml` + 环境变量，组件账号密码类（如后续 OCR/LLM 的 API Key）与运营可调开关没有统一存放处；yml 改动需重新部署
- 任务#3 敏感词过滤组件尚无词库管理与过滤实现；用户自由文本输入点（自定义食物名/别名、饮食记录手动名称+备注、微信昵称、后台食物名/别名、后台用户昵称/备注）无内容安全防线
- 决策（已确认）：词库采用「内置文件 + 配置表开关/追加/排除」；命中统一拦截报错；配置表做 admin 管理接口 + 后台页面；敏感值加密存储

## 2. 目标

- 新建 `project_config` 配置表：统一存放组件账号密码类、运营开关类配置；`value_type` 区分类型，SECRET 类型 AES 加密落库
- 提供 `ConfigService`：JetCache 双层缓存读取 + admin CRUD（分页/新增/修改，key 活跃唯一）
- 敏感词过滤：内置 `sensitive-words.txt` 词库（随版本发布）+ 配置表追加/排除词，Hutool `WordTree`（DFA）匹配，开关可整体关闭；admin 改配置后自动重建
- Service 层 5 处文本输入点统一接入过滤，命中抛统一错误文案
- 后台新增「系统配置」管理页

## 3. 非目标

- 不做敏感词变体对抗（拆字/拼音/同音替换），仅精确 DFA 匹配，变体靠词库扩充
- 不做敏感词表级精细管理（单词条 CRUD），词库变更走发版或配置表追加/排除
- 不做配置变更审计流水、多环境配置隔离（按库天然隔离）
- 不接入 username/email 过滤（管理员账号有格式约束，邮箱有格式校验）
- 不迁移 yml 已有基础设施配置（数据源/Redis/MinIO 连接等仍走 yml+环境变量）

## 4. 范围

- SQL：`sql/change11_project_config.sql`（建表 + 种子配置 3 条）+ `data.sql` 基线同步
- 后端：枚举/常量/PO/Mapper、`ConfigService`（含加密）、`AdminConfigController`、`SensitiveWordFilter` 组件 + 词库文件、5 处 Service 接入、`ExceptionConstant` 新增文案
- 后台前端：`view/configs/index.vue` 管理页 + 路由/菜单 + `api/adminConfig.ts`
- 测试：ConfigService/敏感词过滤/Admin Controller 单测 + 现有 Service 测试构造器适配

## 5. 验收

- `tasks.md` 全部勾完
- 启动后 `project_config` 表存在且有 3 条种子配置；schema_migrations 补账 version=11
- admin 可分页查/新增/改配置；SECRET 值下发脱敏 `******`，改密文值需正确密钥
- 在自定义食物名、记录备注、微信昵称等任一输入点提交含敏感词文本 → 返回统一错误文案，不入库
- 配置 `sensitive.filter.enabled=false` 后同一文本可正常提交
- 全部单测通过（含现有测试适配）

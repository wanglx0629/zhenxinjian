# 代码规范 — Java（zhenxinjian-backend）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.1 |
| 编写日期 | 2026-09-04（V1.1 修订：2026-09-08 增补错误码分段 / 常量枚举 / 注释全量 / 分层强制规约） |
| 适用工程 | `apps/zhenxinjian-backend`（Java 25 + Spring Boot 3.5.16） |
| 详细真源 | [doscFile/03-开发规范.md](../../../doscFile/03-开发规范.md)（本文件为约束摘要） |

***

## 1. 风格与工具

- 遵循脚手架既定风格（无强制 formatter 配置；保持与现有代码一致）。
- 文件头注释：`作者: wanglx`；注释另起一行。
- Lombok 简化 POJO；函数 ≤ 50 行、文件 ≤ 800 行、嵌套 ≤ 4 层。

### 1.1 常量与枚举（禁止魔法值）

- **项目初期即建全量常量**：常用数字 / 字符串常量（类型标识、前缀、默认文案、createBy 来源标识等）统一进 `CommonConstant`（通用）或 `XxxConstant`（业务域）；业务代码禁止裸写 `0` / `1` / `"WECHAT"` 等字面量。
- 固定 3 个及以上取值、带行为分支的类型字段，定义业务枚举（实现于 `common/constant` 或 domain 层枚举类），枚举含 code + 描述。
- **字典枚举（强约束）**：业务表定义 `status` 等字典型字段时，**必须在定义表的同时**新建对应字典枚举类（统一放 `common/enums/`，命名 `XxxStatusEnum` / `XxxEnum`，含 `Integer code` + `String desc` + `of(code)` 查询）；先建枚举再写业务代码，取值一律引用枚举。存量示例：`UserStatusEnum`（0 冻结 / 1 正常 / 2 注销）。
- 用户名前缀等生成规则常量（`wx_` / `guest_`）、默认昵称文案（`微信用户` / `游客`）、操作来源标识（`wechat-login` / `register`…）一律常量化。

## 2. 命名

- 类 `PascalCase`（`XxxController` / `XxxService` / `XxxServiceImpl` / `XxxMapper` / `XxxPO`…实际脚手架 PO 无后缀，如 `User`）；变量 / 方法 `camelCase`；常量 `UPPER_SNAKE_CASE`。
- 表名小写复数蛇形（`users` / `foods` / `diet_records`）；列小写蛇形；PO 驼峰由 MP 自动映射。

## 3. 结构

- 分层：`controller → service（接口）→ service/impl → mapper`；对象四象限 `po / dto / query / vo`。
- Controller 只做参数接收与结果返回；业务在 Service；SQL 在 Mapper。
- `@Transactional` 写在 Service 实现类 public 方法（同类自调用不走代理，需拆分或注入自身代理）；多表一致性写必须事务。
- **依赖风格（B-T19 裁定）**：实现类直依赖为主，接口化仅限豁免清单——多实现/替换点（`SessionEvictor`、`GuestDataMigrator`、`StorageService`）或跨层稳定契约（`UserService`、`WechatAuthService`）；新增 Service 默认实现类直依赖。
- **controller / task 禁止直依赖 Mapper**：判定、查询一律下沉 Service 层，task 只编排与外呼；豁免 `FoodLibraryInitializer`（启动引导种子导入）与 `GuestCleanupBatchExecutor`（B-T07 批级事务主体）。

## 4. 错误处理与错误码

- 统一响应 `Result`（`Result.ok` / `Result.fail`）；业务异常抛 `BusinessException`，`GlobalExceptionHandler` 兜底——三路出口之外禁止 Controller / Service 手拼错误 JSON。
- 错误文案集中 `ExceptionConstant`，禁止业务代码散落字面量。
- **错误码分段**（5 位 = 3 位域码 + 2 位序号；扩展参考阿里巴巴 Java 开发手册错误码规约，保持全量、可扩容）：

| 段位 | 域 | 示例 |
| ---- | ---- | ---- |
| 200 | 成功 | `SUCCESS_CODE = 200` |
| 400xx | 参数 / 请求格式 | 预留（参数校验细分） |
| 401 | 未认证（登录态失效） | `TOKEN_EXPIRED` |
| 401xx | 第三方登录（微信） | 40101 code 无效 · 40102 第三方服务不可用 · 40103 openid 绑定冲突 |
| 402xx | 游客模式 | 40201 游客已到期 · 40202 游客标识无效 |
| 403 | 无权限 | `ACCESS_DENIED` |
| 429 | 频控 / 锁定 | `LOGIN_LOCKED` |
| 500 | 系统默认失败 | `FAIL_CODE = 500` |
| 500xx | 系统内部细分 | 预留 |

> 新增业务域先在此表申请段位；同域新增按序号顺延，禁止跨段复用。

- **第三方错误码只进不出**：微信 errcode 等第三方原始码仅在后端识别、`log.warn` 记录（含原始码便于排障）并归类映射为上表系统码；返回前端的必须且只能是系统码与系统文案，严禁透传第三方原始码 / 原始 message。
- 日志 `log.error("描述", e)`，禁止 `e.printStackTrace()`、空 catch；禁止打印完整 Token / 密码 / 身份证。

## 5. 数据访问（MySQL / MyBatis-Plus）

- 无 BaseEntity：`id` / `delete_flag` / `create_time` / `update_time` 直接写 PO；逻辑删除统一 `@TableLogic`（0 未删 / 1 已删，全局配置 `logic-delete-field: deleteFlag`）。
- **表字段强约束（跨表列名统一）**：所有表必备逻辑删除列 `delete_flag`；业务表另必备 `status` 状态列（TINYINT，取值语义见对应字典枚举）；列名在所有表中保持一致，方便记忆与联查。
- 查询用 `LambdaQueryWrapper`；参数 `#{}` / Wrapper，**禁止 `${}` 拼用户输入**；列表必须分页（`PageQuery` + `toPage()`）或设上限。
- 软删表唯一约束用生成列（活跃唯一，表达式用 `IF(delete_flag = 0, col, NULL)`），勿直接加普通 UNIQUE；营养克数 `DOUBLE` 存储、真实金额 `DECIMAL`。

## 6. 安全校验

- DTO 用 `@Valid` + JSR-303；人体数据等业务区间在 Service **二次兜底**校验（年龄 12–80 / 身高 100–250 / 体重 25–200 / 目标体重 ≤ 当前体重）。
- 管理端接口 `@PreAuthorize("hasRole('ADMIN')")`；当前用户经 `UserContext`；密码 BCrypt。
- Jackson `fail-on-unknown-properties=true` 保持开启。

## 7. 计算口径实现约定

- 一切健康计算**后端实现并落库**（ADR-0004）；公式做成纯函数（可单测），除数 `2 / 2.2 / 2` 等口径**固定写死**，禁止 `nH+1` 动态重算。
- 新增业务 CRUD：`gen-crud.js` 脚本恢复前按本规范 + [开发规范](../../../doscFile/03-开发规范.md) §3 手写完整样板（含 `data.sql` 与 `ExceptionConstant` 同步）。

## 8. 注释要求

- **全量 Javadoc**：所有方法（含接口方法、private 工具方法）必须有 Javadoc，说明用途、关键参数语义、返回值 / 抛出异常；省略的场景（getter/setter 等 Lombok 生成）除外。
- **关键语句行内注释**：安全校验、事务边界、并发兜底（唯一索引冲突复用）、第三方调用与错误归类、缓存失效、计算口径等关键句必须注释「为什么」，供代码 review 核对实现与意图一致。
- 公式类代码必须注释口径来源（如「PRD §2.4 图片公式 · 9/3 定稿」）；复杂业务规则（调碳兜底、守恒）必须说明。

***

> **文档版本**：V1.1　**最后更新**：2026-09-08　**维护**：臻心减项目组

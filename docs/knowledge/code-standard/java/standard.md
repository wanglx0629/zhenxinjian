# 代码规范 — Java（zhenxinjian-backend）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 适用工程 | `apps/zhenxinjian-backend`（Java 25 + Spring Boot 3.5.16） |
| 详细真源 | [doscFile/03-开发规范.md](../../../doscFile/03-开发规范.md)（本文件为约束摘要） |

***

## 1. 风格与工具

- 遵循脚手架既定风格（无强制 formatter 配置；保持与现有代码一致）。
- 文件头注释：`作者: luote (luote) - https://luote996.cn`；注释另起一行。
- Lombok 简化 POJO；函数 ≤ 50 行、文件 ≤ 800 行、嵌套 ≤ 4 层；避免魔法数字（计算口径进常量）。

## 2. 命名

- 类 `PascalCase`（`XxxController` / `XxxService` / `XxxServiceImpl` / `XxxMapper` / `XxxPO`…实际脚手架 PO 无后缀，如 `User`）；变量 / 方法 `camelCase`；常量 `UPPER_SNAKE_CASE`。
- 表名小写复数蛇形（`users` / `foods` / `diet_records`）；列小写蛇形；PO 驼峰由 MP 自动映射。

## 3. 结构

- 分层：`controller → service（接口）→ service/impl → mapper`；对象四象限 `po / dto / query / vo`。
- Controller 只做参数接收与结果返回；业务在 Service；SQL 在 Mapper。
- `@Transactional` 写在 Service 实现类 public 方法（同类自调用不走代理，需拆分或注入自身代理）；多表一致性写必须事务。

## 4. 错误处理

- 统一响应 `Result`（`Result.ok` / `Result.fail`）；业务异常抛 `BusinessException`，`GlobalExceptionHandler` 兜底。
- 错误文案集中 `ExceptionConstant`，禁止业务代码散落字面量。
- 日志 `log.error("描述", e)`，禁止 `e.printStackTrace()`、空 catch；禁止打印完整 Token / 密码 / 身份证。

## 5. 数据访问（MySQL / MyBatis-Plus）

- 无 BaseEntity：`id` / `deleted` / `create_time` / `update_time` 直接写 PO；逻辑删除 `@TableLogic`。
- 查询用 `LambdaQueryWrapper`；参数 `#{}` / Wrapper，**禁止 `${}` 拼用户输入**；列表必须分页（`PageQuery` + `toPage()`）或设上限。
- 软删表唯一约束用生成列（活跃唯一），勿直接加普通 UNIQUE；营养克数 `DOUBLE` 存储、真实金额 `DECIMAL`。

## 6. 安全校验

- DTO 用 `@Valid` + JSR-303；人体数据等业务区间在 Service **二次兜底**校验（年龄 12–80 / 身高 100–250 / 体重 25–200 / 目标体重 ≤ 当前体重）。
- 管理端接口 `@PreAuthorize("hasRole('ADMIN')")`；当前用户经 `UserContext`；密码 BCrypt。
- Jackson `fail-on-unknown-properties=true` 保持开启。

## 7. 计算口径实现约定

- 一切健康计算**后端实现并落库**（ADR-0004）；公式做成纯函数（可单测），除数 `2 / 2.2 / 2` 等口径**固定写死**，禁止 `nH+1` 动态重算。
- 新增业务 CRUD：`gen-crud.js` 脚本恢复前按本规范 + [开发规范](../../../doscFile/03-开发规范.md) §3 手写完整样板（含 `data.sql` 与 `ExceptionConstant` 同步）。

## 8. 注释要求

- 公式类代码必须注释口径来源（如「PRD §2.4 图片公式 · 9/3 定稿」）；复杂业务规则（调碳兜底、守恒）必须说明。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

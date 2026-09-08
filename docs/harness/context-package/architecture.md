# 架构约束 — 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 来源 | 技术栈说明 §2、开发规范 §1/§2/§6、Constitutions §3 |

***

## 1. 总体形态

**三端单体架构**（前后端分离）：

```
用户微信小程序 (uniapp)  ─┐
PC 管理后台 (front)      ─┼─ HTTPS → Nginx 反向代理 → Spring Boot 单体 (backend) → MySQL 8 / Redis
                          └─ WebSocket（子协议 bearer）
```

- 后端统一提供两端全部 API，统一上下文 `/api`（端口 8080）。
- 部署形态：单体 jar + Nginx；**不引入微服务 / 消息队列**（团队 1 开发，禁止过度设计）。

## 2. 允许的分层

### 2.1 后端（根包 `cn.zhenxinjian`）

```
controller → service（接口）→ service/impl → mapper
对象：po（表映射）/ dto（入参）/ query（查询入参）/ vo（出参）
横切：common（cache/constant/exception/query/result/utils）· config · security · websocket
```

### 2.2 管理后台（front）

`view/<功能>`（路由级模块）+ `api/<资源>.ts`（一资源一文件）+ `component/` + `store/`（Pinia）+ `layout/`。

### 2.3 用户端（uniapp）

`pages.json` 声明式路由 + `pages/<页面>` + `api/` + `components/` + `store/` + `utils/`。

## 3. 依赖方向

- 后端：仅允许 `controller → service → mapper` 自上而下调用；`common` / `config` 不依赖业务包；Service 之间可横向调用但**禁止循环依赖**。
- 前端：页面 → api → utils；组件不直接调 api（由页面传入 / 触发）。
- **禁止**：Controller 直接访问 Mapper；utils 反向依赖页面；跨端代码混用（front 与 uniapp 不共享源码，共享口径走后端接口与 PRD）。

## 4. 禁止模式

- Controller 写业务 / SQL；`${}` 拼接用户输入（SQL 注入）。
- Service 同类自调用 `@Transactional` / `@Cached`（AOP 代理失效）——拆 Service 或注入自身代理。
- 领域内单例可变状态；循环依赖。
- 前端页面内裸调 HTTP（必须走 `utils/request.ts` 封装）；WebSocket 绕过 `utils/ws.ts`。
- UniApp 直接操作浏览器 DOM（多端兼容）；小程序端 API 用相对路径（必须绝对 URL baseUrl）。
- 前端硬编码后端密钥 / 内网地址进构建产物。

## 5. 集成边界（外部系统）

| 外部系统 | 协议 | 用途 | 备注 |
| -------- | ---- | ---- | ---- |
| 微信开放平台 | HTTPS API | `wx.login` 换 openid、订阅消息推送 | 生产需 HTTPS + 域名白名单 |
| MinIO / 阿里云 OSS | HTTP(S) SDK | 头像 / 文件上传 | 默认关闭，按需开启（`zhenxinjian.storage.*`） |

> 新增外部依赖必须先经 Checkpoint 确认（宪法 §5 高危操作）。

## 6. 对象与契约约定

- 接口统一返回 `Result`（`Result.ok` / `Result.fail`）；错误文案集中 `ExceptionConstant`。
- 分页入参统一 `PageQuery` + `toPage()`；列表接口必须分页 / 设上限。
- 当前登录用户从 `UserContext` 获取，禁止自行解析 Token。
- 前后端联调契约以 **Swagger UI** 为准。
- 无 BaseEntity：`id` / `deleted` / `create_time` / `update_time` 直接写在各 PO 上，由 `MybatisPlusConfig` 自动填充。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

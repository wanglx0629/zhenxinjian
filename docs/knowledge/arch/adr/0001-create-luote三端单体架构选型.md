# ADR 0001: create-luote 三端单体架构选型

- 状态：Accepted
- 日期：2026-09-04

## 背景

臻心减为个人团队项目（1 PM + 1 开发），产品载体是微信小程序，同时需要 PC 运营管理后台（用户管理、食物库维护、数据查看）与后端服务。可选方案：

1. 微服务拆分（user / food / record 等服务）；
2. 单体 Spring Boot + 三端仓库；
3. Serverless（云函数 + 云开发）。

团队规模小、无硬性时间要求、以打磨体验为核心，且 create-luote 脚手架可一键生成三端样板。

## 决策

采用 **create-luote 脚手架生成的三端单体架构**：

- `apps/zhenxinjian-backend`：Spring Boot 3.4.5 单体（MyBatis-Plus + MySQL 8 + Redis/JetCache），统一 `/api` 上下文；
- `apps/zhenxinjian-front`：Vue 3 + Element Plus 管理后台；
- `apps/zhenxinjian-uniapp`：UniApp 用户小程序（主发微信小程序，可发 H5）；
- 部署：单体 jar + Nginx 反向代理，不引入微服务与消息队列。

## 后果

**正向**：单人可维护；脚手架自带认证（JWT）、缓存（JetCache）、文件存储（MinIO/OSS）、接口文档（Swagger）等成熟能力；三端约定统一（`Result` / `ExceptionConstant`）。

**负向 / 权衡**：单体扩展上限存在（用户量大后需拆分）；三端样板由脚手架锁定，深度定制需理解脚手架约定；UniApp 跨端 API 受小程序运行时限制（禁止直接 DOM 操作）。

**后续约束**：依赖方向与禁止模式见 [architecture.md](../../../harness/context-package/architecture.md)。

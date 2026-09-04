# 后端架构笔记 — zhenxinjian-backend

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 工程路径 | `apps/zhenxinjian-backend` |

***

## 1. 定位

Spring Boot 3.4.5 单体，统一提供小程序端与管理后台全部 API：认证（微信登录 + JWT）、核心计算（BMR/TDEE/宏量）、食物库、饮食记录、提醒、文件上传。上下文 `/api`，端口 8080，Swagger UI：`/api/swagger-ui.html`。

## 2. 包结构（根包 `cn.zhenxinjian`）

```
├── ZhenxinjianApplication.java   # 启动类
├── common
│   ├── ai/        # Spring AI 校验切面（AiValidate / AiValidationAspect）
│   ├── cache/     # CacheValueGuard、UserCacheService、缓存预热
│   ├── constant/  # Cache/Common/Exception/Storage Constant
│   ├── exception/ # BusinessException + GlobalExceptionHandler
│   ├── query/     # PageQuery 分页基类
│   ├── result/    # Result 统一响应
│   └── utils/     # JwtUtils、RedisUtils、UserContext
├── config/        # MybatisPlus / Security / Storage / Swagger / WebSocket / Properties
├── controller/    # Auth / User / File（业务扩展按此模式新增）
├── domain/        # po / dto / query / vo 四象限
├── mapper/        # MyBatis-Plus Mapper
├── security/      # JwtAuthenticationFilter、 EntryPoint、DeniedHandler、JSON 写出
├── service/       # 接口 + impl/；StorageService 抽象（本地/MinIO/OSS）
└── websocket/     # Handler / Interceptor / SessionRegistry
```

## 3. 关键机制

- **统一契约**：`Result` 响应体 + `BusinessException` + `GlobalExceptionHandler` 兜底；错误文案集中在 `ExceptionConstant`。
- **认证**：微信 `wx.login` code → openid 建档；JWT（120min / 30min 续期）存 Redis；`UserContext` 获取当前用户。
- **缓存**：JetCache 两级（Caffeine 本地 + Redis 远程）；`@Cached` 写在独立 Bean（防同类自调用 AOP 失效）；写后 `@CacheInvalidate`；穿透/击穿/雪崩防护齐备。
- **逻辑删除**：`deleted` + `@TableLogic`；无 BaseEntity，公共字段直接写 PO 由 `MybatisPlusConfig` 自动填充。
- **配置**：`application.yml`（主）+ `application-dev.yml` / `application-prod.yml`（不入库，走环境变量）。

## 4. 配置项速查（`zhenxinjian.*`）

| 配置 | 默认 | 说明 |
| ---- | ---- | ---- |
| `jwt.expire-minutes` | 120 | 过期 30 分钟前可续期 |
| `redis.login-fail-max` | 10 | 15 分钟窗口 |
| `cache.max-value-bytes` | 51200 | BigKey 写前守卫 |
| `websocket.max-sessions(-per-user)` | 200 / 3 | 会话上限 |
| `storage.max-size-mb` | 10 | 允许 jpg/png/webp/pdf 等 |

## 5. 扩展模式（新增业务模块）

1. 新增业务 CRUD（脚手架脚本恢复前按规范手写）：controller + service 接口/impl + mapper + po/dto/query/vo + `data.sql` + `ExceptionConstant`。
2. 计算类能力做成**纯函数工具或独立 Service**，便于 khufu-ut 覆盖定稿样例（如碳循环 241/23、153/49、72/77）。
3. 多表一致性写操作加 `@Transactional`；更新后清 JetCache 缓存。

## 6. 已知事项

- `gen-crud.js` 脚本缺失（见宪法 §4 缺口说明）。
- WebSocket 为 Demo 性质（`enabled` 可关）；臻心减一期无实时推送需求（提醒走微信订阅消息）。
- Spring AI Alibaba 已集成但一期不启用 AI 识物。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

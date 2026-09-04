# 资源约束 — 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 来源 | application.yml、PRD §9.1 性能需求、开发规范 §4/§5 |

***

## 1. 数据库（MySQL 8）

- 字符集 `utf8mb4` + `utf8mb4_unicode_ci`，引擎 InnoDB；JDBC 时区 `serverTimezone=Asia/Shanghai`，生产 `useSSL=true`。
- 连接配置走环境变量 `DB_HOST/DB_PORT/DB_USER/DB_PASSWORD`；禁止提交真实凭据。
- 逻辑删除 `deleted`（0/1）；必备字段 `id BIGINT AUTO_INCREMENT` / `deleted` / `create_time` / `update_time`。
- 一期核心表：`users`、`user_body`、`foods`、`diet_records`、`user_reminders`（详见 [PRD MVP](../../prd/mvp/mvp-v1.md)）。
- 营养克数 / 热量：后端按 `DOUBLE` 存储防累计漂移（展示层四舍五入到整数克）；涉及真实金额一律 `DECIMAL`。
- 本地初始化执行 `src/main/resources/data.sql`；生产**禁止**带 `DROP TABLE` 的全量脚本，用变更脚本 / Flyway。

## 2. 缓存（Redis 5+ / JetCache 2.7.7）

- 两级缓存：本地 Caffeine（limit 128，5 分钟）+ 远程 Redis Lettuce（1 小时，keyPrefix `zhenxinjian:cache:`，广播通道 `zhenxinjian-jetcache`）。
- JetCache 不读 `spring.data.redis.password`：有密码必须配完整 URI `JETCACHE_REDIS_URI=redis://:密码@host:6379/0`。
- 会话类 Key 必须 TTL：Token 与 JWT 过期一致、验证码 5 分钟、登录失败计数 15 分钟。
- BigKey 防护：单 String value ~10KB（脚手架 `cache.max-value-bytes=51200`，`CacheValueGuard` 写前校验）；集合 ~1000 元素；大 Key 删除用 `UNLINK`。
- 已有防穿透 / 击穿 / 雪崩：`cacheNullValue` + `@CachePenetrationProtect` + 过期抖动。

## 3. 计算与配额

- 文件上传：单文件 / 单请求 ≤ **10MB**；允许 jpg / jpeg / png / gif / webp / pdf。
- WebSocket（若启用）：单条消息 ≤ 4096 字节；全局会话 ≤ 200；单用户并发 ≤ 3。
- 登录试错：10 次失败锁定 15 分钟。
- 搜索关键词长度截断（防超长输入打爆模糊查询）。

## 4. 性能预算（PRD §9.1，验收口径）

| 指标 | 要求 |
| ---- | ---- |
| 小程序首屏加载 | ≤ 2 秒 |
| 页面切换响应 | ≤ 300ms |
| 计算结果展示 | ≤ 100ms（前端预览） |
| 数据提交响应 | ≤ 1 秒 |
| 食物搜索响应 | ≤ 200ms |
| 食物库首次加载 | ≤ 500ms（缓存至本地） |

## 5. 兼容性

- 微信 iOS 12+ / Android 8+ / 微信 8.0+；微信开发者工具正常调试。

## 6. 外部依赖与 SLA

| 依赖 | 影响 | 降级策略 |
| ---- | ---- | ---- |
| 微信开放平台（登录 / 订阅消息） | 登录与提醒不可用 | 授权失败重试 + 游客 3 天体验兜底 |
| DashScope（一期未用） | 无 | N/A |
| MinIO / OSS（默认关闭） | 头像上传不可用 | 本地存储 `upload/` 兜底 |
| Redis | 缓存 / Token / 验证码失效 | Redis 为强依赖，须先恢复再提供服务 |

## 7. 运行环境

JDK 17+、Maven 3.8+、Node 18+（建议 20 LTS）、MySQL 8、Redis 5+、微信开发者工具；可选 MinIO。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

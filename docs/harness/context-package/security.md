# 安全约束 — 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 来源 | PRD §9.3、脚手架 Security 配置、开发规范 §2.3 / §5.5 |

***

## 1. 认证（Authentication）

- **用户端**：微信一键授权登录，无需手机号，`openid` 唯一绑定用户账号；登录态持久化（JWT + Redis）。
- **管理后台**：账号密码登录，密码只存 **BCrypt 哈希**，禁止明文。
- JWT 由 `JwtAuthenticationFilter` 统一校验；默认 120 分钟过期、提前 30 分钟续期；Token 存 Redis（前缀 `zhenxinjian:token:`）。
- 登录失败计数（`zhenxinjian:login:fail:`，默认上限 10 次 / 15 分钟窗口）。
- 验证码短 TTL（5 分钟）、读后删除。
- 401 统一处理：前端静默刷新失败则跳转登录页。
- **游客体验**（3 天）的时效 / 权限 / 数据迁移判定**以后端校验为准**，前端计时仅用于展示——杜绝前端篡改。

## 2. 授权（Authorization）

- 管理端接口一律 `@PreAuthorize("hasRole('ADMIN')")`。
- `permit-urls` 仅放开登录 / 注册 / 验证码 / WS 握手等必要路径，**不得**误放开敏感接口；每次新增放开路径需在评审中确认。
- 数据归属校验：用户仅能操作**自己的**自定义食物、饮食记录、提醒设置等数据。

## 3. 传输与数据保护

- 全链路 **HTTPS**（小程序生产环境强制要求，且需在微信公众平台配置合法域名）。
- 用户隐私数据（`openid` 等）**仅存服务端，前端不暴露**；加密存储、禁止明文传输。
- Jackson 反序列化 `fail-on-unknown-properties=true`，拒绝未知字段注入。
- 用户输入做 **XSS 过滤**。
- 日志禁止打印完整身份证、银行卡、明文密码、完整 Token。
- 禁用 / 删除用户 / 改密 / 改角色：`removeToken` + 踢 WebSocket（`kickUser`），保持 HTTP 与 WS 会话一致。

## 4. 密钥管理

- JWT Secret、DB / Redis 密码、OSS AK/SK 一律走**环境变量 / 配置**：`JWT_SECRET`、`DB_HOST/DB_PORT/DB_USER/DB_PASSWORD`、`JETCACHE_REDIS_URI`、`OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET` 等。
- 生产环境必须替换 `application.yml` 中的默认值；禁止提交真实 `.env` 与 `application-*.yml`（已在 `.gitignore`）。
- CORS 白名单（`security.cors-allowed-origins`）生产必须替换为真实域名，**禁止 `*`**。

## 5. WebSocket

- Token 通过子协议传递：`new WebSocket(url, [protocol, jwt])`（H5 子协议、小程序 Authorization 双路径）；**禁止放 query**。
- `allowed-origins` 禁止 `*`（留空复用 CORS 白名单）；单条消息字节上限、单用户并发连接与全局会话上限由配置约束。

## 6. 输入校验边界

- 所有用户输入数值做人体合理区间校验，**后端二次兜底拒绝**：年龄 12–80、身高 100–250cm、体重 25–200kg、目标体重 25–200kg 且 ≤ 当前体重；自定义食物宏量 ≈ 热量（±10%）。
- 网络异常、参数缺失、计算异常统一兜底提示，不向前端泄露堆栈细节。

## 7. 合规

- 所有健康计算结果内置**免责声明**：仅作生活化减脂参考，非医疗建议。
- 游客临时数据：过期未登录保留 7 天后自动清空。
- 合规要求：暂无 GDPR/PCI 类外部合规（C 端工具类产品）；涉及微信小程序平台规范（用户协议 / 隐私政策勾选后方可登录）。

## 8. 信任边界

- 信任边界内的三端 + 后端；边界外：微信开放平台 API、MinIO/OSS、公网用户。
- **核心计算结果不可被前端参数篡改**：前端只做预览，后端计算为准（防「计算器类产品刷数据」风险）。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

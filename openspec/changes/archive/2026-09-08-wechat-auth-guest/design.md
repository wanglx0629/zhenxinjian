# Design — 微信授权登录 + 游客 3 天体验

## Context

见 [proposal.md](./proposal.md)。既有可复用资产：backend 的 JWT 体系（jjwt + `JwtUtils`/`JwtAuthenticationFilter` + Redis 黑名单）、`users` 表（已预留 `wechat_openid` 字段与活跃唯一索引 `uk_wechat_openid_active`）、uniapp 的 `request.ts`（Bearer 注入 + 401 处理 + X-Refresh-Token 续期）与 Pinia `store/user.ts`。业务表（user_body / diet_records 等）尚未创建——数据迁移设计须面向「表还不存在」的现实。

## Goals / Non-Goals

**Goals:**

- 微信 code2session 登录闭环（真机可联调，appid/secret 已就绪）
- 游客 3 天体验全链路：签发 → 服务端兜底校验 → 到期拦截 → 迁移 → 过期清空
- 为 Change 2/5 的数据迁移预留扩展点（迁移框架先行，具体表迁移各自补齐）

**Non-Goals:**

- 订阅消息（Change 6）；昵称头像授权获取（`getUserProfile` 可后续增强，先用占位）
- 管理后台侧的用户展示调整（一期后台仅管理员使用）
- 游客「换设备找回」——游客身份仅存本机 storage，丢失即重置 3 天（与原生骨架一致）

## Decisions

### D1. 微信 SDK：WxJava（`weixin-java-miniapp`）

后端引入 WxJava miniapp 模块做 code2session。备选：手写 HTTP 调 `api.weixin.qq.com/sns/jscode2session`——仅一个接口手写也可行，但后续订阅消息（Change 6）仍需微信 API 封装，一次引入复用两端。appid/secret 放 `application-dev.yml`（已在 .gitignore），配 `WxMaProperties` + `WxMaService` Bean。

### D2. 游客身份复用 `users` 表，加 3 列：`user_type` / `guest_expire_at` / `merged_into`

游客 = 一条 `user_type=GUEST` 的 user 记录（`wechat_openid` 为 NULL）；正式用户 `user_type=WECHAT`。

- **理由**：后续所有业务表都以 `user_id` 为归属外键，游客与正式用户同表则「迁移」退化为一次性 `UPDATE biz_table SET user_id = formal_id WHERE user_id = guest_id`，无须跨表搬运；`merged_into` 记录合并去向支持幂等判断。
- **备选**：独立 `guests` 表——迁移时要逐表改归属且查询需 UNION，弃。
- **约束遵守**：不动 `uk_wechat_openid_active`；`user_type` 无唯一性诉求，普通索引即可。

### D3. 游客身份标识 = 后端生成 UUID，客户端 storage 持久化

小程序无稳定设备 ID。「设备维度」落地为：`POST /api/auth/guest` 首次生成 `guest_key`（UUID）返回，客户端存 `uni.setStorage`（键名沿用骨架的 `zxj_` 前缀规范）；有效期内再次启动带原 guest_key 换新 token，不重置起算时刻。清缓存/换设备 = 新游客身份（3 天重新起算，PRD 允许）。

### D4. JWT 单结构双身份：claim 增加 `userType` 与 `gexp`

沿用既有 access/refresh 双 token 结构，payload 增加：

- `userType`: `GUEST` | `WECHAT`
- `gexp`（仅游客）: 游客到期时间戳

`JwtAuthenticationFilter` 中：游客 token 且 `gexp < now` → 返回业务错误码 **GUEST_EXPIRED**（HTTP 200 + Result code，区别于 401 未登录），客户端 `request.ts` 拦截该 code 统一跳 P15。DB 中 `guest_expire_at` 为最终真源（claim 可被提前作废，如已迁移）。

### D5. 迁移框架：`GuestMigrationService` + 登录时合并

`POST /api/auth/wechat/login` 请求体可选携带 `guestKey`。流程：code2session → 找/建正式用户 → 若 guestKey 命中有效游客（含 7 天宽限期内）→ 事务内执行迁移。

迁移框架定义接口：

```
interface GuestDataMigrator { void migrate(Long guestId, Long formalId); }
```

本 change 仅实现「用户记录层迁移」：游客 `merged_into = formalId`、软删游客记录；身体数据/饮食记录的 Migrator 实现分别挂在 Change 2 / Change 5（表创建时同步补 `SpringUtils.getBeansOfType(GuestDataMigrator.class)` 逐个调用）。幂等：游客已 `merged_into` 非空则跳过。

### D6. 过期清空：`@Scheduled` 每日一次

每日 03:00 扫描 `user_type=GUEST AND merged_into IS NULL AND guest_expire_at < now - 7d`：软删用户记录 + 调用全部 `GuestDataMigrator#purge(guestId)` 预留的清理钩子（本阶段无业务数据，仅框架）。

### D7. 错误码段：微信登录 401xx / 游客 402xx

`ExceptionConstant` 预留：40101 code 无效、40102 微信服务不可用、40103 openid 绑定冲突；40201 游客已到期、40202 guestKey 无效。沿用脚手架 `Result` + `BusinessException` 体系，符合宪法「错误文案进 ExceptionConstant」。

### D8. 小程序页面与路由

- 新增 `pages/auth/guide`（P01）与 `pages/auth/expire`（P15，`navigationStyle: custom` 全屏拦截，onBackPress 拦截物理返回）
- 移除脚手架 `pages/login`、`pages/register`、`pages/websocket`；tabBar 调整为 首页/记录/食物库/我的（记录、食物库为占位页，Change 4/5 填充）
- `api/auth.ts` 新增 `wechatLogin(code, guestKey?)` / `createGuest()`；`store/user.ts` 增加 `userType` 状态与到期跳转逻辑
- `request.ts` 响应拦截增加 GUEST_EXPIRED 分支

## Risks / Trade-offs

- [code2session 依赖微信可用性] → 失败快速返回 40102，客户端提示重试；不降级、不本地伪造 openid
- [游客 UUID 可被伪造遍历他人游客身份] → guest_key 仅关联匿名 3 天体验数据、无支付/敏感信息，风险可接受；接口按微信场景值与频控（Redis 计数）缓解
- [迁移时并发登录（双端同时登录）] → 事务 + `merged_into` 判空乐观幂等，后到请求读到已合并状态直接跳过
- [定时任务单实例假设] → 一期单机部署，`@Scheduled` 足够；上多实例时换 ShedLock（记入后续运维 change）
- [tabBar 占位页提前定型] → 占位页仅静态文案，不改导航结构，避免 Change 5 返工

## Migration Plan

1. DB：`users` 表 `ALTER TABLE` 加 `user_type`(默认 WECHAT)/`guest_expire_at`/`merged_into`（提供 `sql/change1_users_guest.sql`）
2. 后端先行动（游客/登录接口可用，Mock 下小程序未动也不影响管理后台）
3. 小程序端页面替换
4. 回滚：接口为新增，DB 新列带默认值，回滚 = 还原代码；已产生的游客记录留存无害

## Open Questions

（无——设备维度兜底方案、宽限期口径均已与 PRD/速查对齐；昵称头像占位策略不影响本 change 任务拆分）

# Tasks — 微信授权登录 + 游客 3 天体验

> 前置：env-baseline 已完成（端口 8080 统一）。设计依据 [design.md](./design.md)，行为契约见 [specs/](./specs/)。

## 1. 数据库与依赖

- [ ] 1.1 编写 `sql/change1_users_guest.sql`：`users` 表加 `user_type`(默认 WECHAT)/`guest_expire_at`/`merged_into` 三列 + `user_type` 普通索引，并执行到本地库
- [ ] 1.2 `pom.xml` 引入 `weixin-java-miniapp`；`application-dev.yml` 增加 appid/secret 配置节（不入库，仅本地）
- [ ] 1.3 `ExceptionConstant` 新增 401xx（微信登录）/ 402xx（游客）错误码段

## 2. 后端 — 微信登录

- [ ] 2.1 `WxMaProperties` + `WxMaService` Bean 配置
- [ ] 2.2 `POST /api/auth/wechat/login`：code2session → openid 找/建用户（`user_type=WECHAT`）→ 签发双 token；请求体可选 `guestKey` 触发迁移
- [ ] 2.3 openid 唯一绑定并发兜底：捕获唯一索引冲突后改为查询复用（spec：并发首次登录）
- [ ] 2.4 微信侧失败处理：code 无效/过期返回 40101，微信不可达返回 40102（不产生半成品用户）

## 3. 后端 — 游客机制

- [ ] 3.1 `POST /api/auth/guest`：生成 `guest_key`(UUID) + `user_type=GUEST` 用户记录（`guest_expire_at = now + 3d`）+ 签发带 `userType`/`gexp` claim 的游客 token；带既有有效 guest_key 则复用记录不重置起算
- [ ] 3.2 `JwtUtils` 支持 `userType`/`gexp` claim 读写；`JwtAuthenticationFilter` 增加游客到期判断：`gexp < now` → 返回 40201（DB `guest_expire_at` 为最终真源）
- [ ] 3.3 `GuestDataMigrator` 接口定义（`migrate(guestId, formalId)` + `purge(guestId)`）；实现用户记录层迁移：游客 `merged_into = formalId` + 软删，幂等判空
- [ ] 3.4 登录接口事务内调用全部 Migrator（`SpringUtils.getBeansOfType`）
- [ ] 3.5 `@Scheduled` 每日 03:00 清理任务：到期超 7 天未合并的游客 → 软删 + 调用各 Migrator 的 purge

## 4. 后端 — 验证

- [ ] 4.1 单测：code2session Mock（成功/无效/不可达三路）、游客签发与复用、gexp 过期过滤器、迁移幂等（重复登录不重复迁移）
- [ ] 4.2 Swagger 手工走查：游客签发 → 业务接口（未过期 200/过期 40201）→ 微信登录携 guestKey → 二次登录不重复迁移

## 5. 小程序 — 页面与路由

- [ ] 5.1 移除 `pages/login`、`pages/register`、`pages/websocket`；新增 `pages/auth/guide`（P01：先体验 3 天 / 微信授权登录）
- [ ] 5.2 新增 `pages/auth/expire`（P15：全屏不可关闭，`navigationStyle: custom` + onBackPress 拦截；立即授权 / 放弃数据退出）
- [ ] 5.3 `pages.json`：tabBar 调整为 首页/记录/食物库/我的（记录、食物库为占位页）；清理失效路由

## 6. 小程序 — 登录与游客逻辑

- [ ] 6.1 `api/auth.ts` 新增 `wechatLogin(code, guestKey?)` / `createGuest()` / `guestRenew(guestKey)`
- [ ] 6.2 `store/user.ts`：`userType` 状态、guest_key storage 持久化（`zxj_` 前缀）、启动时校验身份并分流（有效游客→首页 / 过期→P15 / 无身份→P01）
- [ ] 6.3 `request.ts`：响应拦截增加 40201 分支 → 清登录态并跳转 P15
- [ ] 6.4 P01 微信登录按钮：`wx.login()` 取 code → 调后端 → 失败 toast + 可重试（不缓存 code）
- [ ] 6.5 P15「立即授权」同 6.4 且携带 guestKey 完成迁移；「放弃数据退出」清 storage 回 P01

## 7. 联调验收

- [ ] 7.1 真机（微信开发者工具 + 手机预览）：P01 游客进入 → 功能可用 → 3 天到期模拟（DB 改 `guest_expire_at`）→ P15 拦截不可关闭 → 授权登录数据迁移
- [ ] 7.2 回归：管理后台用户名密码登录不受影响（后台仍走脚手架 AuthController）
- [ ] 7.3 验收对照 specs/ 两份 spec 的全部 Scenario 逐条勾验

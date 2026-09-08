# Change 1 — 微信授权登录 + 游客 3 天体验

## Why

MVP v1 五大能力的地基：所有用户数据（身体数据/饮食记录/提醒）都挂在「身份」上。当前三端仅有脚手架的用户名+密码+图形验证码登录，而 PRD 要求微信一键授权登录 + 游客 3 天完整功能体验（到期强制授权、数据自动迁移）——这是 Phase 1 的第一步，也是后续所有 change 的前置依赖。

## What Changes

**后端（zhenxinjian-backend）**

- 引入 WxJava（weixin-java-miniapp）SDK，新增微信配置节（appid/secret 走 `application-dev.yml`，不入库）
- 新增 `POST /api/auth/wechat/login`：接收 `wx.login()` 的 code → `code2session` 换取 openid → 按 openid 唯一绑定查找/创建用户 → 复用既有 JWT 体系签发 token
- 新增游客身份机制：`POST /api/auth/guest` 签发游客 token（设备维度的匿名身份 + 3 天有效期，后端兜底校验）
- 游客→登录数据迁移：微信登录时若请求头携带有效游客 token，将该游客名下业务数据合并到正式用户（当前阶段先落框架与用户记录合并，身体数据/记录表的迁移随 Change 2/5 各自补齐）
- 游客过期清理：游客身份过期 7 天后数据清空（定时任务）
- `ExceptionConstant` 新增微信侧错误码（code 无效/过期、code2session 调用失败、游客身份过期等）
- **BREAKING**（开发期内部）：脚手架的 `AuthController` 用户名密码登录降级为管理后台专用，小程序端不再调用

**小程序（zhenxinjian-uniapp）**

- P01 游客引导页：首次进入选择「先体验 3 天」或「微信授权登录」
- 微信授权登录流程：`wx.login()` 取 code → 调后端登录接口 → 授权失败可重试（F02）
- 游客模式：本地不落地敏感数据，token 由后端签发；启动时校验游客时效，到期跳转 P15
- P15 到期强制授权页：不可关闭的弹窗，仅提供「立即授权」/「放弃数据退出」
- 替换脚手架登录页/注册页：`pages/login`、`pages/register` 移除，`pages.json` 路由与 tabBar 调整为产品形态（首页/记录/食物库/我的四 tab 占位）
- `api/auth.ts` 新增微信登录/游客接口封装，Pinia user store 适配双身份

## Capabilities

### New Capabilities

- `auth/wechat-login`: 微信一键授权登录——code2session 换取 openid、openid 唯一绑定用户、JWT 登录态签发与续期、授权失败重试
- `auth/guest-mode`: 游客 3 天体验——游客身份签发与后端时效兜底校验、到期强制授权拦截（不可关闭）、游客数据登录后自动迁移、过期 7 天清空

### Modified Capabilities

（无——`openspec/specs/` 目前为空，脚手架登录行为无既有规格）

## Impact

- **依赖**：backend pom 新增 `weixin-java-miniapp`；uniapp 无新增 npm 依赖
- **代码**：backend `auth/` 接口层 + `security/` JWT 过滤器适配游客 token 类型；uniapp `pages/`（删 2 增 2）、`api/auth.ts`、`store/user.ts`
- **数据**：`users` 表复用既有 `wechat_openid` 字段（活跃唯一索引 `uk_wechat_openid_active` 已预留）；可能需加 `user_type`（游客/正式）与 `guest_expire_at` 字段——以 design 阶段定稿为准
- **外部依赖**：微信 appid/secret 已就绪；订阅消息模板未申请（不影响本 change，属 Change 6）
- **前置**：依赖 Change 0（env-baseline）端口统一完成

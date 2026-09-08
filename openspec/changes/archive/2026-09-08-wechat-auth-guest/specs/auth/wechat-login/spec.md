# Spec Delta — auth/wechat-login

## Purpose

让微信小程序用户通过一键授权完成登录：以微信 code 换取 openid 并唯一绑定用户身份，签发与刷新登录态，授权失败时可持续重试直至成功。

## ADDED Requirements

### Requirement: 微信 code 换取登录态

系统 SHALL 提供微信登录接口：接收小程序端 `wx.login()` 获取的临时 code，向微信服务器完成 code2session 交换得到 openid，随后签发与既有 JWT 体系兼容的登录态（access token + refresh token）返回给客户端。

#### Scenario: 首次微信登录成功

- **WHEN** 客户端携带有效 code 调用微信登录接口
- **THEN** 系统完成 code2session 得到 openid，创建新用户记录（昵称/头像默认占位，openid 写入唯一绑定字段），返回登录态与用户信息

#### Scenario: code 无效或已使用

- **WHEN** 客户端携带无效、过期或已消费的 code 调用登录接口
- **THEN** 系统返回明确的业务错误（微信侧错误码透传归类），客户端可重新执行 `wx.login()` 获取新 code 后重试

#### Scenario: 微信服务器不可达

- **WHEN** code2session 调用超时或微信服务端异常
- **THEN** 系统返回登录服务暂不可用的业务错误，不产生半成品用户记录

### Requirement: openid 唯一绑定用户

系统 SHALL 保证一个 openid 在任意时刻至多绑定一个活跃用户：同一 openid 再次登录时 MUST 复用既有用户记录（含其历史数据），MUST NOT 创建重复用户。

#### Scenario: 同一微信用户重复登录

- **WHEN** 同一 openid 第二次调用微信登录接口
- **THEN** 系统返回同一用户的登录态，用户历史数据完整保留

#### Scenario: 并发首次登录

- **WHEN** 同一 openid 的两个登录请求并发到达
- **THEN** 系统仅创建一个用户记录，另一请求复用该记录（依赖活跃唯一约束兜底，不抛 500）

### Requirement: 授权失败可重试

小程序端 SHALL 在授权被拒绝或登录接口失败时提供重试路径，且重试 MUST 引导用户重新完成授权动作（不缓存失效 code）。

#### Scenario: 用户拒绝授权后重试

- **WHEN** 用户在授权弹窗点击拒绝，随后再次点击登录按钮
- **THEN** 客户端重新唤起授权流程（重新 `wx.login()` 取新 code），直至成功或用户离开页面

### Requirement: 登录态持久化与续期

系统 SHALL 签发有过期时间的登录态并支持续期：access token 过期后客户端可凭 refresh token 换新，无须用户重新授权；refresh token 失效时 MUST 引导重新登录。

#### Scenario: access token 过期后静默续期

- **WHEN** 请求携带过期 access token 与有效 refresh token
- **THEN** 系统签发新登录态，业务请求不中断，用户无感知

#### Scenario: refresh token 失效

- **WHEN** refresh token 过期或无效
- **THEN** 客户端清除本地登录态并引导用户重新走微信授权登录

# ADR 0005: 微信 Token 内存态与单实例部署约束

- 状态：Accepted
- 日期：2026-09-13

## 背景

后端微信能力（登录 code2session、订阅消息推送）依赖 access_token，该凭证微信侧全局唯一、有效期 2 小时、刷新限额。当前 `WxMaConfiguration` 采用 weixin-java-miniapp SDK 的 `WxMaDefaultConfigImpl`，token 存 JVM 内存由 SDK 自动续期。一期为单实例部署，此方案零额外协调成本；但一旦多实例部署，各实例各自刷新会互相顶替——后刷新者使先刷新者的 token 失效，触发 40001/42001 抖动，登录与推送链路随机失败。同类单实例假设还存在于定时任务层（`ReminderPushTask` 等注释标注「上多实例时换 ShedLock」），属同一约束族。该约束此前仅口头存在，未显式登记，扩容即踩坑。

## 决策

1. **一期裁定单实例部署**，`WxMaDefaultConfigImpl` 内存 token 为当前正式方案，不为此引入额外协调组件。
2. **扩容前置门禁**：任何多实例部署（水平扩容 / 蓝绿 / 滚动多副本）之前，必须将 `WxMaService` 的 config 切换为 Redis 集中存储实现——SDK 4.7.0 已内置 `WxMaRedisConfigImpl` / `WxMaRedissonConfigImpl`（`cn.binarywang.wx.miniapp.config.impl` 包），token 集中存储、集群共享单一刷新结果；切换为配置类替换，业务调用代码零改动。
3. 本约束与定时任务 ShedLock 化同属**多实例扩展门禁族**，扩容评审时须一并核对（token 存储 + 调度去重两项均完成后方可上多实例）。

## 后果

**正向**：一期零协调成本，token 链路不依赖 Redis 可用性；约束经 ADR 显式化并与代码注释互链，扩容评审有明确核对清单；SDK 4.7.0 内置 Redis 实现，迁移路径现成、成本低。

**负向 / 权衡**：单实例成为部署硬约束，水平扩容前必须先完成决策 2 的切换与回归；实例重启后内存 token 丢失需重新获取（SDK 懒加载自动处理，代价为首次调用略增延迟，可接受）。

**关联**：[WxMaConfiguration.java](../../../../apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/config/WxMaConfiguration.java)（类注释互链）、[security.md](../../../harness/context-package/security.md)、定时任务单实例约束（`ReminderPushTask` 类注释 ShedLock 换装点）。

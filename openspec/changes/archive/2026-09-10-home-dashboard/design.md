# Design — 首页 Dashboard（P02）与「我的」页（P14）

## Context

现状约束（详见 proposal.md - Why）：

- `pages/home/index.vue` 为 Change 1 占位欢迎页；`pages/mine/index.vue` 仅有账号信息 + 退出登录
- 数据通路全部就绪：`store/diet.ts`（`fetchDay` 并行拉 `diet/records` + `diet/summary`，含三色进度所需的 actual/target/rate 与 `mode`、`recorded` 空态标记）、`store/body.ts`（档案 + `mode`）、`store/user.ts`（游客 / 正式 / 剩余时长）
- 三色进度与超标建议卡已在 `pages/record/index.vue` 内联实现（diet-record 交付），文案口径以其为准
- **并行约束**：carb-cycle 变更正在修改 `pages/record/index.vue`、`config/constants.ts`、`pages.json`，并新建 `pages/mode/select.vue`、`pages/cycle/plan.vue`、`pages/cycle/setting.vue`（本变更跳转目标）

## Goals / Non-Goals

**Goals:**

- 首页一屏聚合当日减脂全景（热量 + 三宏 + 超标 + 餐次），数据与记录页同源同口径
- 「我的」页成为身份与设置枢纽（档案摘要、当前模式、留存口径）
- 与 carb-cycle 的代码交集最小化，支持两变更独立推进、先后合并

**Non-Goals:**

- 不新增 / 修改任何后端接口、表结构、maven/npm 依赖
- 不改动 `pages/record/index.vue`（carb-cycle 正在改），不在 `pages.json` 注册新页面（P02/P14 已注册）
- 不实现 P05/P06/P07 目标页本身（carb-cycle 范围）；不做提醒、记体重、历史版本（后续变更）

## Decisions

### D1 首页数据组装：复用既有 store，onShow 强制回到当日

首页直接复用 `useDietStore.fetchDay(ymd(new Date()))` + `useBodyStore` 拉取，不新建 home store。

- **为什么**：diet store 的 `fetchDay` 已封装 records+summary 并行拉取与空态计算，重复造轮子只会引入口径漂移风险（spec 要求首页与记录页数值完全一致）
- **取舍**：diet store 的 `currentDate` 是与记录页共享的单值。首页每次 onShow 强制设为当日——产品语义正确（P02 是「今日总览」），记录页 onShow 按其自身逻辑恢复查看日期，互不污染；该行为列入联调走查项
- **备选**：首页独立局部状态直调 api——放弃，会导致两套数据组装逻辑

### D2 进度条 / 超标建议卡：新建独立组件，不回改记录页

新建 `components/MacroProgress.vue`（三宏三色进度）与 `components/OverLimitCard.vue`（超标 chips + 微调建议），样式与文案口径参照 `pages/record/index.vue` 既有实现，仅首页使用。

- **为什么**：抽取共享组件并回改记录页是最优长期解，但 `pages/record/index.vue` 正被 carb-cycle 修改，回改必然产生合并冲突；先让首页用上正确口径，统一重构留给两变更汇合之后
- **代价**：短期内进度 / 建议存在两份实现，口径由 spec（`diet/progress` 三色与文案表）锁定，走查时逐项对照

### D3 字典与常量：优先复用 carb-cycle 新增项，本变更增量最小

模式标签（532 / 碳循环）与日型文案复用 carb-cycle 在 `config/constants.ts` 新增的模式 / 日型字典；本变更仅新增其未覆盖的展示常量（如餐次展示名、问候语时段），追加在文件末尾独立区块，降低同文件冲突面。

### D4 跳转分发口径

| 入口 | 目标 | 说明 |
| ---- | ---- | ---- |
| 换模式 / 减脂模式 | `pages/mode/select` | carb-cycle 交付 |
| 看计划（mode=2） | `pages/cycle/plan` | carb-cycle 交付 |
| 看计划（mode=1） | `pages/body/result` | 既有 P04，532 目标即档案快照 |
| 碳循环无周期引导 | `pages/cycle/setting` | carb-cycle 交付（P06） |
| 身体数据 / 修改 | `pages/body/profile` | 既有 P03 |
| 记饮食 / 餐次卡 | `pages/record/index` | tab 页用 `uni.switchTab` |
| 授权登录（P14） | `pages/auth/guide` | 复用既有完整授权 + 重试流程，不重复实现 wx.login |

- tab 页跳转 MUST 用 `switchTab`（首页 / 记录 / 食物库 / 我的在 tabBar），非 tab 页用 `navigateTo`

### D5 问候与剩余时长口径

- 问候语按本地时分发：05–11 早上好 / 11–14 中午好 / 14–18 下午好 / 18–23 晚上好 / 23–05 夜深了；日期展示「M月D日 星期X」
- 游客剩余时长复用 `userInfo.guestExpireAt` 计算，首页与我的页同一算法（不足 1 天按小时展示「X 时」，≥1 天展示「X 天」或「X 天 X 时」），两页口径一致由同一工具函数保证

### D6 页面三态与性能

- 加载态：骨架屏（头卡 + 进度卡占位块）；错误态：提示 + 重试按钮重新拉取全部；空态按 spec「空态引导」分未建档 / 无记录 / 无周期三种
- 首屏性能（PRD ≤2s）：三接口并行（diet store 内部已并行 records+summary，body profile 与之再并行），骨架屏先行渲染

## Risks / Trade-offs

- [carb-cycle 未合入时，跳 `pages/mode/select` 等路由在已注册前不可用] → 该路由注册已由 carb-cycle 提交进 `pages.json`；本变更联调验收项明确要求在 carb-cycle 合并后走查全链路，开发期先验证 532 路径与空态
- [`config/constants.ts` 与 carb-cycle 同文件并行改动] → 本变更仅文件末尾追加独立区块，冲突时为纯文本合并，手工保留双方区块
- [进度 / 建议两份实现短期并存（D2）] → 文案与三色口径以 spec 为准双向对拍；汇合后由后续变更统一抽取
- [首页 onShow 重置 diet store 日期影响记录页查看历史日期的体验] → 产品口径：首页永远展示当日；记录页保留自身日期恢复逻辑，走查验证

## Migration Plan

纯前端变更，无数据迁移、无灰度开关：随小程序版本正常发布；回滚 = 回退版本即可（首页 / 我的页均为新渲染逻辑替换占位页，无数据兼容性问题）。

## Open Questions

- 身体数据历史版本追溯（F08）是否在 MVP 一期范围内——PRD 未明确，待 PM 确认后单独立项，不影响本变更

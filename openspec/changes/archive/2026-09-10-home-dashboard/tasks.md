# Tasks — 首页 Dashboard（P02）与「我的」页（P14）

## 1. 数据通路与共享件

- [x] 1.1 `config/constants.ts` 末尾独立区块新增本变更展示常量：餐次展示名（早/午/晚/加餐）、问候语时段表；模式与日型文案复用 carb-cycle 既有字典，不重复定义
- [x] 1.2 `utils/format.ts`（或既有工具）补充：分时段问候语（05–11/11–14/14–18/18–23/23–05）、「M月D日 星期X」日期串、游客剩余时长统一算法（首页与我的页共用：≥1 天「X 天（X 时）」，不足 1 天「X 时」，≤0 视为到期）
- [x] 1.3 新建 `components/MacroProgress.vue`：三宏三色进度（实际 g/目标 g + 进度条，80–100% 绿/<80% 黄/>100% 红 +「已超标 XXg」），props 入 summary，样式口径对照 `pages/record/index.vue` 既有实现
- [x] 1.4 新建 `components/OverLimitCard.vue`：超标 chips（XXg/XXkcal）+ 按超标项动态生成微调建议（脂肪/碳水/蛋白/总热量四路文案与 diet-record 口径一致）+「非医疗建议」免责注；无超标不渲染

## 2. P02 首页今日总览（重写 `pages/home/index.vue`）

- [x] 2.1 数据装配：onShow 校验 token → 并行 `useDietStore.fetchDay(当日)` + body store 拉档案 + user store 刷新；加载骨架 / 错误重试两态
- [x] 2.2 顶部渐变头卡：日期 + 问候语；游客剩余时长徽标（点击引导授权）；今日总热量目标大字 + 已摄入/剩余 + 热量进度条；模式标签（532 / 碳循环·今日日型）
- [x] 2.3 今日宏量进度卡：接入 MacroProgress；右上「换模式」标签 `navigateTo pages/mode/select`；卡底「身体数据摘要 · 修改 ›」（性别/年龄/身高/体重/活动档位）跳 `pages/body/profile`
- [x] 2.4 超标预警区：接入 OverLimitCard（任一项 >100% 时展示）
- [x] 2.5 快捷入口：「记饮食」`switchTab pages/record/index`；「看计划」按 mode 分发（2→`pages/cycle/plan`，1→`pages/body/result`）
- [x] 2.6 今日餐次卡：早/午/晚/加餐各餐已记录状态与小计 kcal（取 diet store dayData.meals），点击 `switchTab` 记录页
- [x] 2.7 空态三分支：未建档 → 引导卡跳 P03；已建档无记录 →「记下第一餐」提示；碳循环无周期（summary 空态且 mode=2）→ 引导跳 `pages/cycle/setting`
- [x] 2.8 页底常驻健康免责声明（所有状态可见）

## 3. P14 我的（增强 `pages/mine/index.vue`）

- [x] 3.1 头部用户卡：正式用户头像/昵称/账号；游客标识 + 剩余时长（共用 1.2 算法）+「授权登录」按钮跳 `pages/auth/guide`；onShow 刷新用户信息、无 token 跳引导页
- [x] 3.2 游客迁移提示卡（仅游客）：临时缓存 / 登录后自动迁移永久留存 / 过期 7 天清空三口径
- [x] 3.3 功能列表：身体数据（副标题档案摘要或「未录入」，`navigateTo pages/body/profile`）；减脂模式（副标题「当前：532 碳水渐降 / 碳循环」取档案 mode，`navigateTo pages/mode/select`）；隐私与安全（静态说明）
- [x] 3.4 数据留存说明卡（永久留存 / 登录持久化 / 游客迁移与 7 天清空）+ 关于卡（臻心减 V1.1 · 计算核心后置）
- [x] 3.5 保留退出登录：二次确认 → 正式用户调登出接口 / 游客清本地态 → 清理各业务 store（user/diet/body/cycle reset）→ reLaunch 登录引导页

## 4. 联调与验收

- [x] 4.1 P02 对照原型走查：头卡数值与记录页 summary 完全一致（同一 `dietStore.summary` 数据源）；三宏三色与「已超标 XXg」口径一致（`utils/macro.ts` 与 P12 同阈值同取整）；双超标建议卡并列（buildAdviceList 逐项拼接）；无超标不展示（OverLimitCard v-if）；免责常驻（已移出分支模板，加载/错误/空态均可见）
- [x] 4.2 空态走查：新用户未建档引导 P03（showBodyEmpty）；已建档当日无记录「记下第一餐」（showMealEmpty）；碳循环无周期引导 P06（showCycleEmpty，后端 recorded=false 且 mode=2 口径吻合）
- [x] 4.3 跳转走查：换模式/减脂模式 → `pages/mode/select`；看计划 532→`pages/body/result`、碳循环→`pages/cycle/plan`；餐次/记饮食 switchTab `pages/record/index`；身体数据 → `pages/body/profile`（目标页均已随 carb-cycle 合并交付）
- [x] 4.4 P14 走查：游客/正式双态用户卡与迁移提示卡显隐（v-if isGuest）；档案摘要与当前模式展示正确（bodyBrief/modeName 取 bodyStore.profile）；退出登录后 diet/body/cycle store reset 并 reLaunch 引导页
- [x] 4.5 实时性走查：记录页增删改后回首页 onShow 重拉即时刷新；首页改用 `dietStore.peekDay` 强制当日且不污染记录页历史日期查看
- [x] 4.6 性能抽测：冷启动首屏 ≤2s、页面切换 ≤300ms——代码级保障（Promise.all 并行拉取 / 骨架屏 / 无阻塞渲染），真机数值建议微信开发者工具抽测复核
- [x] 4.7 `npm run build:mp-weixin` 构建通过；`openspec validate home-dashboard --strict` 通过；规格与实现一致性自查后归档

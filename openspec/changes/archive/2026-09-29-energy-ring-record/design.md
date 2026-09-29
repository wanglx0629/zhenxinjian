# 设计 — 能量环推广至 P12 记录页 + 进度模板收敛

## Context

- EnergyRing 组件（`components/EnergyRing.vue`）已落地首页头卡：props `percent/size/stroke/trackColor/overColor`，默认插槽承载环心内容，>100 自动红满环（design-system-v2-p0）
- P12 记录页「当日进度」面板（`pages/record/index.vue:195-210`）为四条线性进度条，模板与 `components/MacroProgress.vue` 同构复制（两处样式类名 progress-* 完全一致）；数据口径同源 `utils/macro.ts#buildProgressItems`，返回 `carb/protein/fat/kcal` 四项，`kcal` 项含 `rate/actual/target/overAmount`
- 首页当前用法（参照模板，`pages/home/index.vue:230-238`）：`<EnergyRing :percent="kcalRate" :size="360" :stroke="20">` + 插槽三行文字 + 环外「还可吃/已超标」胶囊
- 约束：三色阈值（80/100）与 `utils/macro.ts` 口径不可改；`MacroProgress.vue` 首页用法（`:279`，四项全量）不可破坏

## Goals / Non-Goals

**Goals:**

- P12 当日进度面板：热量项升级为能量环（环心 = 已摄入数字 / 目标 kcal），三宏保留线性条，形成「环 + 三条」的信息层级
- 记录页三宏线性条与 `MacroProgress.vue` 收敛为单一实现，消除手工复制的样式漂移
- 首页/记录页热量视觉语言一致（同一组件、同一缺口即预算语义）

**Non-Goals:**

- 不改 `buildProgressItems`/`buildAdviceList` 计算逻辑与三色阈值
- 不改 EnergyRing 组件本体（零改动复用）
- 不动首页任何现有展示（MacroProgress 四项全量用法保持默认行为）
- 不推广到 P07/P08 计划页（无 actual 数据，语义错配，见归档 change 挂账说明）
- 不做后台 Web 版能量环（独立后续变更）

## Decisions

### D1 记录页热量环布局：面板内左环右条还是上环下条？

**选定：上环下条（环居面板顶部中央，三宏条列于环下方）。**

- 理由：与首页 hero「环居中、环心大数字」语言完全一致；面板宽度 686rpx（页面 padding 后）不足以做左环右条的横向双栏（环最小可读直径约 240rpx + 三宏条需 ~400rpx）
- 环尺寸取 **280rpx / stroke 16**（小于首页 360/20：记录页环是面板级信息单元，首页是 hero 主视觉，层级应有区分；16 为设计系统 §8.6 描边档位下限）
- 环心内容：`已摄入` 小标 + `kcalActual` 大数字（48rpx，次级于首页 64rpx）+ `/ kcalTarget kcal`；环下方右侧保留状态行（达成率 % 或 `已超标 XX kcal`，色值沿用 item.color 三色口径）
- 备选否决：左环右条（宽度不足）；环心放达成率%（弱化核心结果数字，与首页语义冲突）

### D2 模板收敛方向：改 MacroProgress 支持过滤，还是记录页继续内联？

**选定：`MacroProgress.vue` 增加 `excludeKeys?: ProgressItem['key'][]` prop，记录页传 `['kcal']`。**

- 理由：组件注释自述「口径与 P12 记录页一致」，本就该单一实现；过滤 prop 是最小改动（computed 一行 filter），默认不传 = 四项全量 = 首页行为不变
- 记录页删除内联 progress-* 模板（`:197-209`）与对应样式（`:383-434`），热量项由 EnergyRing 区块承接，三宏区改 `<MacroProgress :summary="dietStore.summary" :exclude-keys="['kcal']" />`
- 备选否决：新建子组件拆分（过度设计）；记录页继续内联（维持双处漂移风险）

### D3 热量环数据源：直接用 progressItems 中的 kcal 项

**选定：从现有 `progressItems` computed 取 kcal 项驱动环与状态行，不新增 computed。**

- `percent = kcalItem.rate`（后端 kcalRate 原值，>100 由组件内建红满环承接）
- 环心数字 `kcalItem.actual`、`kcalItem.target`；状态行 `overAmount > 0 ? '已超标' : '达成率'` 口径同现状线性条
- 面板渲染条件 `progressItems.length` 不变（空态/未建档分流逻辑零改动）

### D4 文档回写口径

`docs/knowledge/design-system/design-system.md` §8.6 落地标注追加「P12 记录页已接入（energy-ring-record）」；不新增章节，仅补一行推广记录 + 版本行日期。

## Risks / Trade-offs

- [MacroProgress 加 prop 影响首页] → 默认值不传即全量，首页模板零改动；验收含首页回归走查
- [记录页删除内联样式后三宏条视觉差异] → MacroProgress 与内联版本类名/样式值本就逐行相同（两处 diff 对照确认），切换后视觉零差异；验收含截图对照
- [环 + 三条使面板变高，页面首屏信息密度下降] → 环 280rpx 属紧凑档；餐别分组仍在首屏下方一屏内可达，可接受
- [mp-weixin 兼容] → EnergyRing 已是 data-uri `<image>` 方案（v2-p0 已双端验证），记录页复用零新增风险

## Migration Plan

1. MacroProgress 加 `excludeKeys`（向后兼容）
2. 记录页接入环 + 切换三宏条 + 删内联模板/样式
3. 文档回写
4. 回滚 = `git revert`（纯前端展示层，无数据/接口变更）

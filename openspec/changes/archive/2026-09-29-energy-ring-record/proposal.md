# 提案 — 能量环推广至 P12 记录页 + 首页/记录页进度模板收敛

## Why

EnergyRing 组件已随 design-system-v2-p0 落地首页头卡（2026-09-28），但 P12 当日记录页的「当日进度」面板仍是四条线性进度条（碳水/蛋白/脂肪/热量逐行同构），热量作为核心结果指标与三宏平铺同级，信息层级未体现「热量为主、三宏为辅」的产品语义；且记录页进度条模板（`pages/record/index.vue:197-209`）与 `components/MacroProgress.vue:15-31` 为手工复制的重复实现，两处口径虽同源（`utils/macro.ts` buildProgressItems）但样式漂移风险长期存在。归档 change「不做」清单已挂账「能量环推广至记录页」为后续变更。

## What Changes

- **P12 记录页「当日进度」面板热量项升级**：热量从线性进度条升级为 EnergyRing 能量环（环心承载已摄入大数字 / 目标 kcal / 达成率，与首页 hero 语言一致），三宏（碳水/蛋白/脂肪）保留线性进度条；三色判定阈值（80%/100%）与 `utils/macro.ts` 数据口径完全不变
- **模板收敛**：记录页三宏线性条改用 `components/MacroProgress.vue`（其注释自述「口径与 P12 记录页一致」，本为同构复制），消除双处维护；`MacroProgress` 增加按 key 过滤能力（记录页排除 kcal 项，首页保持四项全量）
- **超标语义保持**：热量超标时环切红色满环（EnergyRing 内建 >100 行为），「已超标 XX kcal」文案与红色胶囊由环下方状态行/胶囊承接，口径同现状

不改：三色判定阈值、`buildProgressItems`/`buildAdviceList` 计算口径、后端接口、日期导航与餐别分组结构、首页任何现有展示。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无——本变更为纯视觉层迁移：P12 页面信息呈现形式升级 + 前端模板去重，无 spec 级行为变化。沿用 design-system-v2 / design-system-v2-p0 先例，`.openspec.yaml` 设 `skip_specs: true`。）

## Impact

- **小程序**（`apps/zhenxinjian-uniapp`）：
  - `src/pages/record/index.vue`：当日进度面板热量项替换为 EnergyRing + 三宏条改用 MacroProgress 组件
  - `src/components/MacroProgress.vue`：增加 `excludeKeys`（或等价）prop 支持按 key 过滤进度项
- **不受影响**：后端、API、`utils/macro.ts`、首页既有四项 MacroProgress 用法（默认行为不变）、EnergyRing 组件本身（零改动复用）

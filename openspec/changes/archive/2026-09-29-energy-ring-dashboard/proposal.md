# 提案 — 后台 Web 版 EnergyRing 组件 + Dashboard DAU/MAU 粘性比环

## Why

设计系统 V2「能量引擎」视觉语言已覆盖小程序首页（360/20 hero 环）与 P12 记录页（280/16 面板环），但**后台是该语言唯一尚未覆盖的端**：dashboard 四个总览卡均为绝对计数（今日/昨日 DAU、MAU、累计用户·记录），缺少能快速判断产品健康度的比率指标；且小程序版 `EnergyRing.vue` 是 rpx 单位 + `<image>` SVG data-uri 方案（为绕开 mp-weixin 限制），无法直接用于 Web。此项已被 design-system-v2-p0 与 energy-ring-record 两个归档 change 的「后续变更」同时点名，是唯一未被决策性否决的实质开发项。

## What Changes

- **新增后台 Web 版 EnergyRing 组件**（`apps/zhenxinjian-front/src/component/EnergyRing.vue`）：原生内联 `<svg>`（非 data-uri）、px 单位、`stroke-dashoffset` 走 CSS transition 补间；视觉语义与小程序版一致——琥珀半透明 track 缺口 + 绿→琥珀对角 linearGradient 进度弧，round linecap；色值引用 front 工程 `--zhenxinjian-*` CSS 变量（JS 侧取渐变端点用常量，与 `echarts-theme.ts` 先例一致）
- **新增 `prefers-reduced-motion` 降级**（§8.7 规范欠账，本次一并清掉）：减少动态偏好下禁用补间、直接定位
- **Dashboard 新增 DAU/MAU 粘性比环卡片**：percent = 今日 DAU / 近30天 MAU × 100（`StatsOverview` 现有 `todayDau`/`mau` 两字段前端计算，**零后端改动**）；环心放百分比大数字 + `DAU/MAU` 标注；卡片落位于 KPI 卡行（与现有四张同级，响应式栅格）
- 不改变：现有四张 KPI 卡内容、DAU 趋势图、后端接口与统计口径、任何小程序页面

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无——纯前端视觉资产新增：跨端组件移植 + dashboard 一张比率卡，无 spec 级行为变化，沿用 design-system-v2-p0 / energy-ring-record 先例，`.openspec.yaml` 设 `skip_specs: true`。）

## Impact

- **管理后台**（`apps/zhenxinjian-front`）：
  - 新增 `src/component/EnergyRing.vue`（Web 版，内联 SVG）
  - `src/view/dashboard/index.vue`：新增粘性比环卡片（el-col），新增粘性比 computed
- **不受影响**：后端、API/`StatsOverview` 字段（仅前端组合两字段）、小程序双端、现有 dashboard 其余模块、ECharts 主题

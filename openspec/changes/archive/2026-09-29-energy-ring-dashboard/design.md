# 设计 — 后台 Web 版 EnergyRing 组件 + Dashboard DAU/MAU 粘性比环

## Context

- 小程序版 `EnergyRing.vue`（uniapp）：rpx 单位 + `<image>` SVG data-uri + setInterval 10 步补间，为绕开 mp-weixin 不支持内联 SVG；视觉 = 琥珀半透明 track + 绿→琥珀 userSpaceOnUse linearGradient 弧，round linecap
- Front 工程：组件目录为 `src/component/`（单数，现有 CaptchaInput/PagePager/TeLogo）；色值令牌完备于 `global.css :root`（`--zhenxinjian-primary #00AC7C`、`--zhenxinjian-cta #FFB020`、`--zhenxinjian-danger`、`--zhenxinjian-track`、`--zhenxinjian-font-num` 等）；ECharts 取色先例 `utils/echarts-theme.ts`（JS 常量写 hex）
- Dashboard：`view/dashboard/index.vue` 四 KPI 卡（el-row，`:xs="12" :sm="6"`）+ DAU 趋势 block；`overview` 类型 `StatsOverview`（todayDau/yesterdayDau/mau/totalUsers/totalDietRecords）
- 约束：零后端改动；§8.7 后台须尊重 `prefers-reduced-motion`

## Goals / Non-Goals

**Goals:**

- Web 版 EnergyRing：内联 SVG、px 单位、CSS transition 补间、reduced-motion 降级，视觉语义与小程序版一致
- Dashboard 新增 DAU/MAU 粘性比环卡，仅前端组合现有两字段

**Non-Goals:**

- 不新增/修改后端接口与统计口径
- 不做多环嵌套、不开放渐变端点 props（沿用归档决策）
- 不替换现有四张 KPI 卡与趋势图
- 不碰 mp-weixin 字体（已决策维持降级）
- 不把能量环推广到后台其他页面（仅 dashboard 一处）

## Decisions

### D1 Web 版渲染：原生内联 SVG

**选定：`<svg viewBox="0 0 120 120">` 直接内联，circle 属性响应式绑定。**

- 理由：Web 无 mp-weixin 限制；内联 SVG 可直接绑定 `stroke-dasharray`、去掉 data-uri 字符串拼接/encodeURIComponent/Map 缓存，代码量减半且可被 CSS 控制
- 结构与小程序版同构：track 圆（`var(--zhenxinjian-cta)` + 45% 透明度，用单独常量 `rgba(255,176,32,.45)`）+ 进度弧圆（`<linearGradient id>` userSpaceOnUse 对角 x1=8,y1=8→x2=112,y2=112，端点 `var(--zhenxinjian-primary)`/`var(--zhenxinjian-cta)`），`transform="rotate(-90 60 60)"`、round linecap
- 备选否决：data-uri（Web 无必要）；ECharts 环形饼图（动效/渐变语义损失、包体增加）

### D2 补间：CSS transition on stroke-dasharray

**选定：进度弧 `stroke-dasharray` 加 `transition: stroke-dasharray .3s ease-out`，浏览器合成层自动补间。**

- 与小程序版 300ms ease-out 时长/体感对齐；首次挂载不触发动画（mount 后下一帧再启用 transition class，避免 0→值的加载动画，与小程序「首次直接跳变」口径一致）
- 实现：`:style` 绑定 dasharray；组件根节点 class `ready` 在 `onMounted` requestAnimationFrame 后置 true，CSS 中 `.ready .arc { transition: ... }`
- 备选否决：JS rAF/setInterval（CSS 已够，少一段命令式代码）

### D3 reduced-motion 降级

`@media (prefers-reduced-motion: reduce)` 下 `.arc { transition: none }`；纯 CSS 方案，无需 JS 读媒体查询，清掉 §8.7 规范欠账。

### D4 粘性比口径与异常边界

- `stickiness = mau > 0 ? todayDau / mau * 100 : 0`；理论上 DAU ⊂ MAU 窗口，比率 ∈ [0,100]
- 喂环 percent 做 `Math.min(stickiness, 100)` 兜底（防统计口径边界瞬时错位出现 >100；此处 >100 不是「超标」语义，不走红环）；Web 组件保留与小程序版一致的 `overColor` >100 能力（通用组件），但本页不会触发
- mau=0（无数据）显示 0 环 + 环心「—」，不显示误导性数字

### D5 卡片落位

**选定：KPI 行下方新增独立 `el-row`，环卡 `<el-col :xs="24" :sm="12" :md="8">`，左对齐不占满宽。**

- 理由：五卡塞进原 sm:6 行会 4+1 错位；独立行 + md=8 为后续比率卡留位（行内可再容纳两张），当前单卡不浪费纵向空间
- 卡片沿用 `stat-card tech-topline` 视觉：环居中（120px），环心 = 粘性比 `XX%` 大数字（`.num`，命中 Space Grotesk）+ 环下/环内 `DAU/MAU` 与 `今日DAU/30日MAU` 小字
- 备选否决：环卡与趋势图左右拼（趋势图高度联动、改动大）；原行挤五卡（栅格错位）

## Risks / Trade-offs

- [内联 SVG 在小屏/缩放模糊] → viewBox + 矢量，天然无损；环 120px 固定，卡片响应式
- [首次挂载动画误触发] → ready class rAF 后置（D2）；验收含首屏直接到位检查
- [粘性比口径被误读为精确值] → 卡片标注「今日 DAU / 近 30 天 MAU」分母口径，小字说明
- [组件与小程序版漂移] → 两版结构/色值同源（同 §8.6 与 CSS 变量），仅渲染层不同；文档回写标注两版位置

## Migration Plan

1. 新增 Web EnergyRing 组件
2. Dashboard 加粘性比 computed + 环卡
3. 文档回写（§8.6 标注 Web 版 + §8.7 reduced-motion 已落地）
4. 回滚 = `git revert`（纯前端，无数据变更）

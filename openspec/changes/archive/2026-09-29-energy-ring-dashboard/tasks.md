# 后台 Web 版 EnergyRing + Dashboard 粘性比环 — Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消（注明原因）
> 前置阅读：本 change `design.md`（D1–D5 决策）与 `docs/knowledge/design-system/design-system.md` §8.6/§8.7

## Web 版组件

- [x] **T-01** 新增 `apps/zhenxinjian-front/src/component/EnergyRing.vue`：原生内联 `<svg viewBox="0 0 120 120">`（design D1）——track 圆（琥珀 45% 透明）+ 进度弧圆（userSpaceOnUse 对角 linearGradient，端点 `var(--zhenxinjian-primary)`/`var(--zhenxinjian-cta)`、round linecap、`rotate(-90 60 60)`），circle 属性响应式绑定；props：`percent`（必填）、`size?`（px，默认 120）、`stroke?`（px，默认 7）、`trackColor?`/`overColor?`；默认插槽承载环心；保留 >100 红色满环能力（通用组件口径）；作者注释
- [x] **T-02** 补间与降级（design D2/D3）：进度弧 `stroke-dasharray` CSS transition `.3s ease-out`；`onMounted` rAF 后置 `ready` class 防首屏加载动画；`@media (prefers-reduced-motion: reduce)` 下 `transition: none`；组件卸载无残留定时器（纯 CSS transition，无定时器）；渐变 id 经 `useId()` 逐实例唯一

## Dashboard 接入

- [x] **T-03** `apps/zhenxinjian-front/src/view/dashboard/index.vue`：新增粘性比 computed——`mau > 0 ? todayDau / mau * 100 : 0`，喂环前 `Math.min(.,100)` 兜底（design D4）；import EnergyRing
- [x] **T-04** 模板新增独立环卡行（design D5）：`<el-row>` + `<el-col :xs="24" :sm="12" :md="8">`，`stat-card tech-topline` 视觉；环心放粘性比 `XX%` 大数字（`.num` 命中 Space Grotesk），环内/环下标注 `DAU/MAU` 与「今日 DAU / 近 30 天 MAU」分母口径；mau=0 显示「—」；沿用现有 v-loading（overviewLoading）不新增加载态

## 文档回写

- [x] **D-01** `docs/knowledge/design-system/design-system.md`：§8.6 落地标注追加「后台 dashboard Web 版已接入（energy-ring-dashboard，2026-09-29），Web 组件内联 SVG」；§8.7 标注 `prefers-reduced-motion` 已落地；更新文首「最近更新」与文末版本行（V2.3）

## 验收

- [x] **V-A1** 编译通过：front `npm run build`（vue-tsc + vite build）成功无报错（2026-09-29 实测通过）
- [x] **V-A2** 后台页面截图走查 dashboard（2026-09-29 Playwright 实测）：粘性比环弧长随比率（80/400→弧 dasharray 65.34/261.38、环心 20.0% 居中不遮挡）；环卡响应式（375px 窄屏整行 / 宽屏 md=8 左对齐）；现有四张 KPI 卡与趋势图无变化
- [x] **V-A3** 首屏无加载动画（ready class rAF 后置，截图首次打开环直接到位）；`reduced_motion='reduce'` 模拟下进度弧 computed transitionDuration 由 0.3s 变为 ~0（补间禁用），正常环境为 0.3s
- [x] **V-A4** 边界值（代码走查）：mau=0 时 computed 返回 0、`hasStickiness=false` → 环 0 + 环心「—」；todayDau>mau 时 `Math.min(.,100)` 封顶，喂给组件 percent ≤ 100 不触发 over 红环
- [x] **V-A5** `openspec validate energy-ring-dashboard` 通过（2026-09-29 实测：Change is valid）

## 不做（超范围，列入后续变更）

- 多环嵌套 / 渐变端点开放 props → 无需求（沿用归档决策）
- 新增业务达成率统计口径（热量/记录达成率等） → 需后端，独立变更
- 能量环推广后台其他页面（用户/内容管理等） → 无明确比率场景
- mp-weixin 字体文件 → 已决策维持降级

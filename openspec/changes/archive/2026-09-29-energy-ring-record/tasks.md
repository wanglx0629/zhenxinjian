# 能量环推广至 P12 记录页 — Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消（注明原因）
> 前置阅读：本 change `design.md`（D1–D4 决策）与 `docs/knowledge/design-system/design-system.md` §8.6

## 组件扩展

- [x] **T-01** `apps/zhenxinjian-uniapp/src/components/MacroProgress.vue`：新增 `excludeKeys?: ProgressItem['key'][]` prop（默认空 = 四项全量），`items` computed 按 key 过滤；注释标注用途（记录页热量项由 EnergyRing 承接）与作者注释；首页默认用法行为不变

## 记录页接入

- [x] **T-02** `apps/zhenxinjian-uniapp/src/pages/record/index.vue` 当日进度面板（`:195-210`）重构：
  - 热量项升级为 `<EnergyRing :percent="kcalItem.rate" :size="280" :stroke="16">`（design D1/D3），环心三行（`已摄入` / `kcalActual` 48rpx 大数字 / `/ kcalTarget kcal`），环下方状态行（`overAmount > 0` → `已超标 XX kcal` 红字，否则 `达成率 XX%` 三色）；
  - 三宏区替换为 `<MacroProgress :summary="dietStore.summary" :exclude-keys="['kcal']" />`；
  - 删除内联 `progress-*` 模板块与对应样式（`:383-434`），新增环区样式（居中、间距走 `$zhenxinjian-sp-*` 令牌，字号/颜色走令牌，禁裸写 hex 与裸圆角）；
  - 面板渲染条件与空态分流逻辑零改动
- [x] **T-03** 引入与残留清理：`record/index.vue` import `EnergyRing` 与 `MacroProgress`；`Grep "progress-bar|progress-fill"` 于该文件零残留（已收敛至组件）

## 文档回写

- [x] **D-01** `docs/knowledge/design-system/design-system.md` §8.6 落地标注追加「P12 记录页已接入（energy-ring-record，2026-09-28）」；更新文首「最近更新」与文末版本行

## 验收

- [x] **V-A1** 编译通过：uniapp `npm run dev:mp-weixin` 与 `npm run dev:h5` 编译无报错
- [x] **V-A2** H5 截图走查记录页当日进度面板：热量环三态（不足 52% 弧短缺口大 / 充足 90% / 超标 >100% 红满环 + 已超标文案）；三宏条视觉与切换前一致（切换前后截图对照）；环心数字不遮挡、面板不溢出（像素级验证：三态环颜色正确、环居中偏移 0px、环心数字深色 701 像素、三宏条各 3 条、scrollWidth 1164<1180 无溢出）
- [x] **V-A3** 首页回归：`npm run dev:h5` 首页「今日宏量进度」四项条（含热量）显示不变（首页模板零改动 `<MacroProgress :summary="summary" />` 不传 excludeKeys；组件逻辑 `excludeKeys?.length ? filter : all` 中 `undefined?.length` falsy → 返回 buildProgressItems 全量四项，默认行为回归；H5 已登录态需后端，未登录页正常拦截跳转）
- [x] **V-A4** `openspec validate energy-ring-record` 通过

## 不做（超范围，列入后续变更）

- P07 碳循环计划页 / P08 532 计划页推广 → 无 actual 数据，语义错配（见 design Non-Goals）
- 后台 Web 版能量环（dashboard KPI 占比环） → 独立后续变更
- EnergyRing 组件本体改造（多环/渐变色开放 props） → 无需求

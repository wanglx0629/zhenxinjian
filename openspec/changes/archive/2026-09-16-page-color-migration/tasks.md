# 双端页面级旧色迁移 — Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消（注明原因）

## 小程序端（zhenxinjian-uniapp）

- [x] **M-01** `components/EmptyState.vue`：4 幅 SVG 插画内联色迁移（#F0FDFA→#F4F8F6、#CCFBF1→#E3EFE9、#0D9488→#00AC7C、#14B8A6→#33BD96、#F97316→#FFB020）；CTA 阴影 rgba(249,115,22,.24) → rgba(255,176,32,.24)
- [x] **M-02** `pages/home/index.vue`：kcalBarColor 三色（#EF4444/#22C55E/#F59E0B → #FF4747/#00AC7C/#FFB020）；hero 渐变改 `$zhenxinjian-gradient-brand`；quick-item 两处阴影换琥珀/叶绿；.meal-chip.mc2 #ccfbf1 → #E3EFE9
- [x] **M-03** `pages/reminder/index.vue`：4 处 switch color="#0d9488" → #00AC7C；2 处 SVG stroke（#F59E0B→#FFB020、#EF4444→#FF4747）
- [x] **M-04** `pages/menstrual/index.vue`：switch color → #00AC7C；.phase-card 底色 → `$zhenxinjian-primary-bg`、描边 #99f6e4 → #B3E7D8
- [x] **M-05** `pages/record/index.vue`：.meal-chip #ccfbf1 → #E3EFE9
- [x] **M-06** `pages/mine/index.vue`：hero 渐变改 `$zhenxinjian-gradient-brand`
- [x] **M-07** `pages/taper/plan.vue`：today-card 渐变改 `$zhenxinjian-gradient-brand`
- [x] **M-08** `config/constants.ts`：PROGRESS_COLORS 三色（#22C55E/#F59E0B/#EF4444 → #00AC7C/#FFB020/#FF4747），注释同步 V2.0
- [x] **M-08a** 补充扫描发现（原清单外）：`App.vue` .btn-primary 两处琥珀阴影；`pages/body/profile.vue` 输入框 focus 叶绿光晕；`pages/record/recognize.vue` repick-btn 半透明叶绿底

## 管理后台（zhenxinjian-front）

- [x] **M-09** `view/dashboard/index.vue`：统计卡渐变与阴影迁移；ECharts 配置色板迁移（#0D9488→#00AC7C、#F97316→#FFB020、#CCFBF1→#E3EFE9、#F0FDFA→#F4F8F6、#134E4A→#10312B、rgba 旧值同比例换）；成功卡渐变 #22C55E→#00AC7C
- [x] **M-10** `view/login/index.vue`：页面背景改单色微光（对齐 global.css body）；auth-card 阴影换叶绿
- [x] **M-11** `component/TeLogo.vue`：logo 渐变改 `var(--zhenxinjian-gradient-brand)`
- [x] **M-11a** 补充扫描发现（原清单外）：`view/error/404.vue` 背景微光换叶绿；`public/favicon.svg` 底色 #0D9488 → #00AC7C

## 文档与验收

- [x] **M-12** `design-system.md` §8.10「硬编码残留列入后续变更」备注更新为已清除
- [x] **M-13** grep `apps/` 全部 V1.1 旧色值（十六进制大小写 + rgba）零残留
- [x] **M-14** 构建验证：管理后台 `vue-tsc && vite build` ✓（2252 模块，15.9s）；小程序 `uni build -p mp-weixin` ✓（DONE Build complete）
- [x] **M-14a** 终验补扫修正（原清单外）：`pages/home/index.vue` 与 `pages/record/index.vue` .meal-chip.mc1 旧橙浅底 #ffedd5 → #FFF1D9；`pages/home/index.vue` .hero-remain.over rgba(239,68,68,.4) → rgba(255,71,71,.4)；修正后小程序端重新构建通过

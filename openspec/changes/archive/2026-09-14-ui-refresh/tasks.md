# 双端 UI 升级 — Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消（注明原因）
> 按 P0 → P1 → P2 顺序执行；同优先级内按编号顺序。

## 小程序端（zhenxinjian-uniapp）

### P0 主色统一（基础）

- [x] **U-01** 改造 `src/uni.scss`：替换 `$zhenxinjian-primary: #0D9488`、`$zhenxinjian-primary-light: #14B8A6`、`$zhenxinjian-bg: #F0FDFA`、`$zhenxinjian-text: #134E4A`、`$zhenxinjian-border: #CCFBF1`，新增 `$zhenxinjian-cta: #F97316` 与阴影变量
- [x] **U-02** 改造 `src/pages.json`：`backgroundColor` 改 `#F0FDFA`，tabBar `selectedColor` 改 `#0D9488`
- [x] **U-03** 改造 `src/App.vue` 全局样式：`.panel` border 改 `#CCFBF1` + 新增 `box-shadow`；`.btn-primary` 主 CTA 改 `#F97316` + 按压态
- [x] **U-04** 我的页 `src/pages/mine/index.vue` 6 个 emoji（👤🔄🔔⚖️🩸🔒）替换为 inline SVG 图标
- [x] **U-05** 全局检查 `#409eff` 残留并替换为 teal（`constants.ts` CYCLE_DAY_TYPES 中碳日、`components/TeLogo.vue` 渐变已清理）

### P1 视觉层级

- [x] **U-06** 代谢结果 `src/pages/body/result.vue` 主结果数字 40rpx 黑 → 72rpx / 700 / 品牌渐变文字
- [x] **U-07** 首页 `src/pages/home/index.vue` hero 进度条由白色改为三色（按 rate 充足绿/不足黄/超标红）
- [x] **U-08** 记录页 `src/pages/record/index.vue` FAB 改橙色渐变 + `$zhenxinjian-shadow-fab`
- [x] **U-09** 身体数据 `src/pages/body/profile.vue` 输入框 focus 态加 teal 描边

### P2 空态插画化

- [x] **U-10** 首页未建档空态 emoji → `EmptyState` SVG 插画 + 橙色渐变 CTA（含无周期 / 当日无记录三分支）
- [x] **U-11** 食物库 `src/pages/food/index.vue` 搜索无结果空态插画化（search 型插画）
- [x] **U-12** 记录页未建档 / 无周期空态插画化（body / calendar 型插画）
- [x] **U-13** 代谢结果未录入空态插画化（body 型插画）

### 验收（小程序）

- [-] **U-A1** `npm run dev:h5` 跑通，4 个 tab 页 + 我的 + 代谢结果 + 记录页主流程截图对比（⏸️ 2026-09-15 暂缓：需人工跑 dev server 截图，代码侧静态验收 U-A2/U-A3 已过）
- [x] **U-A2** `Grep "#409eff"` 在 `apps/zhenxinjian-uniapp/src` 下无业务代码残留
- [x] **U-A3** `Grep "👤|🔄|🔔|⚖️|🩸|🔒"` 在 `apps/zhenxinjian-uniapp/src` 下无残留

---

## 管理后台（zhenxinjian-front）

### P0 品牌化

- [x] **A-01** 改造 `src/styles/global.css`：注入 `--el-color-primary` 一族 teal 变量；`--zhenxinjian-primary` 同步改 `#0D9488`
- [x] **A-02** 登录页 `src/view/login/index.vue`：副标题「wanglx」改「生活化减脂 · 管理后台」；背景 teal 渐变；卡片品牌化
- [x] **A-03** 全局检查 `#409eff` 残留（`component/TeLogo.vue`、`public/favicon.svg` 已清理）

### P1 视觉层级

- [x] **A-04** 仪表盘 `src/view/dashboard/index.vue` 4 张统计卡：加图标 + 浅色渐变底 + hover 阴影 + 主色数字
- [x] **A-05** 仪表盘 ECharts 折线改 teal + orange 双主题色
- [x] **A-06** 用户管理 `src/view/users/index.vue` 操作列加 `View / Edit / Delete` 图标 + `el-empty` 空态
- [x] **A-07** 食物库 `src/view/foods/index.vue` 操作列图标化 + `el-empty` 空态
- [x] **A-08** 饮食记录 `src/view/diet-records/index.vue` 操作列 `View` 图标 + `el-empty` 空态

### P2 导航与健壮性

- [x] **A-09** `src/router/index.ts`：路由 meta 配 title；新增 catch-all 404 路由
- [x] **A-10** `src/layout/MainLayout.vue`：顶栏下方加面包屑（路由 meta.title 驱动）
- [x] **A-11** 新增 `src/view/error/404.vue`：渐变大字 + 返回首页按钮
- [x] **A-12** `src/component/PagePager.vue`：分页 layout 加 `sizes, jumper`

### 验收（后台）

- [-] **A-A1** `npm run dev` 跑通，登录 / 仪表盘 / users / foods / diet-records / 404 截图对比（⏸️ 2026-09-15 暂缓：需人工跑 dev server 截图，代码侧静态验收 A-A2/A-A3 已过）
- [x] **A-A2** `Grep "#409eff"` 在 `apps/zhenxinjian-front/src` 下无业务代码残留
- [x] **A-A3** 登录页副标题无「wanglx」字样

---

## 文档持久化

- [x] **D-01** 新色板 / 阴影 / 字号 / 三色语义已增量写入 `docs/knowledge/design-system/design-system.md` V1.1（V1.0 内容保留）

---

## 完成度速查

| 端 | P0 | P1 | P2 | 验收 |
| ---- | ---- | ---- | ---- | ---- |
| 小程序 | 5/5 | 4/4 | 4/4 | 2/3 |
| 后台 | 3/3 | 5/5 | 4/4 | 2/3 |
| 文档 | — | — | — | 1/1 |

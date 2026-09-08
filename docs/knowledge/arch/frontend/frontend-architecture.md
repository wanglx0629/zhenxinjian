# 前端架构笔记 — zhenxinjian-front（管理后台）与 zhenxinjian-uniapp（用户小程序）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.1 |
| 编写日期 | 2026-09-04　V1.1 修订：2026-09-08（补充小程序骨架移植层） |

***

## A. PC 运营管理后台（`apps/zhenxinjian-front`）

### A1. 定位

面向运营人员：用户管理、食物库维护（内置食物的增改）、数据查看。Vue 3 + TypeScript + Vite + Element Plus + Pinia。

### A2. 结构

```
src/
├── api/        # 一资源一文件（auth.ts / user.ts ...），类型集中 types.ts
├── component/  # 通用组件（CaptchaInput / TeLogo / WsStatusCard / DemoChart ...）
├── layout/     # MainLayout 主布局
├── router/     # Vue Router
├── store/      # Pinia（user.ts）
├── styles/     # global.css
├── utils/      # request.ts（Axios 封装）、ws.ts
└── view/       # 页面（home / login / register / users / websocket）
```

### A3. 约定

- 路径别名 `@` → `src`；Composition API + `<script setup>`。
- 请求统一走 `utils/request.ts`（Axios 实例）；401 静默刷新失败跳登录。
- 开发端口 5173，`/api` 代理到 `http://localhost:8080`（`VITE_API_PROXY_TARGET` 可覆盖）。
- 构建含类型检查：`npm run build` = `vue-tsc && vite build`。
- 后台接口按模块挂到管理菜单，数据侧后端要求 ADMIN 角色。

## B. 用户小程序（`apps/zhenxinjian-uniapp`）

### B1. 定位

面向 C 端用户的产品主体：游客引导 → 身体数据 → 模式选择（532/碳循环）→ 计划 → 首页 Dashboard → 饮食记录（食物库）。一套代码主发微信小程序（可发 H5）。

### B2. 结构

```
src/
├── api/        # request.ts（http 封装，绝对 URL baseUrl）/ auth.ts / user.ts / types.ts
├── components/ # CaptchaInput / TeLogo / WsBoard / WsStatusCard
├── config/     # constants.ts ★ 全局业务常量（活动系数/缺口档位/532配比/碳循环参数/经期四阶段/录入区间）
├── data/       # foods.ts ★ 内置食物库 200 条（每 100g 口径，离线兜底）
├── pages/      # 脚手架初始页（home/login/mine/register/websocket）
├── static/     # tabBar 图标 / logo / 提示音
├── store/      # Pinia（user.ts）
├── utils/      # request / ws / storage / notify / throttle
│              # + calculator.ts ★ 核心算法 / validate.ts ★ 录入校验 / format.ts ★ 格式化
├── pages.json  # 路由 + tabBar + 全局样式
└── manifest.json # 应用配置（小程序 AppID）
```

> ★ 标记五个文件移植自 MRD-PRD 小程序骨架（原生微信小程序参考实现），TypeScript 化且通过 48 项对拍断言（BMR/TDEE/532/碳循环/经期四阶段/食物换算全部与 PRD 图片公式一致）。移植分析见 [doscFile/projectFile/03-小程序工程骨架分析.md](../../../doscFile/projectFile/03-小程序工程骨架分析.md)。

> 当前为脚手架初始页面集，需按 PRD 信息架构新增：游客引导（P01）、身体数据（P03）、代谢结果（P04）、模式选择（P05）、碳循环周期设置 / 计划（P06/P07）、532 月度计划（P08）、体重记录调碳（P09）、食物搜索（P10）、食物详情（P11）、当日记录（P12）、提醒设置（P13）、我的（P14）、到期强制授权（P15）、切换模式确认（P16）。页面对照表见 [mvp-v1.md](../../prd/mvp/mvp-v1.md) §6。

### B3. 约定

- 页面注册于 `pages.json`（路由 + tabBar），tabBar 图标放 `static/`。
- 小程序端 API **绝对 URL** baseUrl；开发期微信开发者工具可勾选「不校验合法域名」，生产 HTTPS + 平台域名白名单。
- 跨端优先 `uni.*` API，禁止直接操作 DOM；样式 Sass + `uni.scss` 变量。
- 请求统一 `utils/request.ts`；提醒走微信订阅消息（`wx.requestSubscribeMessage()`）。
- 类型检查：`npm run type-check`（vue-tsc --noEmit）。
- **算法层口径**：`config/constants.ts` 为前端唯一常量真源；`utils/calculator.ts` 仅做即时展示与离线兜底，改动须重跑对拍断言（基准见 [doscFile/projectFile/02-业务规则与公式速查.md](../../../doscFile/projectFile/02-业务规则与公式速查.md) §9 验算基准）。

## C. 两端共享口径

- 契约以后端 Swagger UI 为准；响应统一 `Result`。
- WebSocket 只经 `utils/ws.ts`（子协议 / Authorization 传 Token，禁 query）。
- 计算结果前端仅预览，展示以后端返回为准（ADR-0004）。

***

> **文档版本**：V1.1　**最后更新**：2026-09-08　**维护**：臻心减项目组

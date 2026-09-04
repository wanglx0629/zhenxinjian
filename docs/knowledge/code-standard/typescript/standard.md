# 代码规范 — TypeScript / Vue 3（front 管理后台 + uniapp 小程序）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 适用工程 | `apps/zhenxinjian-front`（Vue 3.4 + TS 5.4 + Element Plus）· `apps/zhenxinjian-uniapp`（UniApp 3 + Vue 3 + TS 4.9） |

***

## 1. 风格与工具

- Vue 3 **Composition API + `<script setup>` + TypeScript**；样式 Sass（uniapp）/ 全局 css（front）。
- 组件 `PascalCase` 命名；HTML/CSS 文件 kebab-case；变量 / 函数 `camelCase`；常量 `UPPER_SNAKE_CASE`。
- 类型检查：front `npm run build`（含 vue-tsc）；uniapp `npm run type-check` ——提交前必须通过。

## 2. 结构

- **一资源一 api 文件**（`api/user.ts`、`api/food.ts`…），请求 / 响应类型集中 `api/types.ts`。
- HTTP 一律走封装 `utils/request.ts`（front：Axios 实例；uniapp：`http` default export），**页面禁止裸调**。
- WebSocket 只经 `utils/ws.ts`。
- 状态 Pinia（`store/`）；路由 front 用 Vue Router、uniapp 用 `pages.json`（新页面必须同步注册；tabBar 图标放 `static/`）。
- 通用组件进 `component/`（front）/ `components/`（uniapp）；页面组件按功能目录组织。

## 3. 错误处理

- 401 统一处理：静默刷新失败 → 跳转登录页。
- 网络异常 / 参数缺失 / 计算异常统一 Toast 兜底文案（与后端 `ExceptionConstant` 口径一致）。
- 不写空 catch；异步错误需有用户可见反馈。

## 4. 跨端（uniapp 专属）

- 优先 `uni.*` API，**禁止直接操作浏览器 DOM**（多端兼容）。
- 小程序端 API 用**绝对 URL** baseUrl（`utils/request.ts` 配置）；生产 HTTPS + 微信公众平台域名白名单。
- 样式用 `uni.scss` 变量；rpx 优先；兼容性目标：微信 iOS 12+ / Android 8+ / 微信 8.0+。

## 5. 安全

- 前端**不暴露** openid 等敏感数据；不硬编码后端密钥 / 内网地址。
- 用户输入渲染做 XSS 过滤；订阅消息授权状态如实记录。
- 计算结果仅做预览展示，**以后端返回为准**（ADR-0004）；前端不得自行落库计算值。

## 6. 注释要求

- 页面 / 组件顶部说明用途与对应原型页（如 `P03 身体数据录入`）；复杂交互逻辑（智能推荐餐别、默认轮播日型）注释来源 PRD 章节。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

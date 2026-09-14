# 双端 UI 升级（去朴素化）— Proposal

| 项目 | 内容 |
| ---- | ---- |
| Change ID | 2026-09-14-ui-refresh |
| 类型 | feature（视觉/交互升级，不改业务逻辑） |
| 涉及端 | `apps/zhenxinjian-uniapp`（用户小程序）+ `apps/zhenxinjian-front`（PC 管理后台） |
| 涉及后端 | 无 |
| 关联文档 | `docs/knowledge/design-system/design-system.md`（本 Change 完成后回写） |

## 1. 背景

调研结论：双端共用一套「Element Plus 出厂默认蓝 #409eff + 白卡 + 1rpx 灰描边」的工程风，存在以下问题：

- 小程序端 hero 已自发演化出青绿渐变（#0d9488→#14b8a6），但被全局 EP 蓝稀释，**两套主色互不认账**
- 管理后台 100% 使用 EP 出厂主题，登录副标题写作者名「wanglx」，零品牌感
- 小程序「我的」页 6 个列表项使用 emoji（👤🔄🔔⚖️🩸🔒）充当图标
- 全 App 仅 1 处 box-shadow（FAB），卡片零层级
- 仪表盘 4 张统计卡刻意 `shadow="never"`，无图标、无色彩区分
- 表格操作列纯文字 link，无图标
- 三色语义（充足绿/不足黄/超标红）仅渗透进度条，未扩散到 hero / 徽章 / 提示卡

## 2. 目标

- 统一双端主色为 **teal（#0D9488）**，辅色 CTA **orange（#F97316）**，与 PRD 健康减脂语义对齐
- 建立 Z 轴阴影层级 token（card / pop / fab）
- 消除 emoji 图标，替换为 SVG / EP 图标
- 仪表盘 / 登录页 / 列表页品牌化

## 3. 非目标

- 不引入任何 UI 库（uView / uni-ui / Tailwind / UnoCSS），保持手写 SCSS + EP 现状
- 不改业务逻辑、接口、数据模型
- 不做深色侧边栏布局重构（仅列入 P3 备选）
- 不做暗色模式

## 4. 范围

- 小程序：uni.scss / App.vue 全局样式 / pages.json / 我的 / 首页 / 代谢结果 / 记录页 / 食物库 / 身体数据
- 后台：global.css / MainLayout / 登录页 / 仪表盘 / users / foods / diet-records / 404 页 / 分页组件

## 5. 验收

- `tasks.md` 全部勾完
- 双端主色统一为 teal，无 `#409eff` 残留（除 EP 内部 hover 派生）
- 我的页、列表操作列无 emoji 残留
- 代谢结果页主数字 ≥ 64rpx
- 仪表盘统计卡有图标、有渐变底、有 hover 阴影
- 登录页副标题不再出现作者名

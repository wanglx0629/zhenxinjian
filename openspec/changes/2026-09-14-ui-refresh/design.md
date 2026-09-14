# 双端 UI 升级 — Design

## 1. 设计 Token（双端统一真源）

### 1.1 色板

| Token | Hex | 用途 |
| ---- | ---- | ---- |
| `--brand-primary` | `#0D9488` (teal-600) | 主按钮、tabBar 选中、链接、激活态 |
| `--brand-primary-light` | `#14B8A6` (teal-500) | hover、渐变副色 |
| `--brand-gradient` | `linear-gradient(160deg, #0D9488, #14B8A6)` | hero 卡、登录页品牌区 |
| `--cta-action` | `#F97316` (orange-500) | 「立即记录」「保存」等关键 CTA |
| `--cta-action-hover` | `#FB923C` (orange-400) | CTA hover |
| `--success` | `#22C55E` (green-500) | 进度充足 / 达标 |
| `--warning` | `#F59E0B` (amber-500) | 进度不足 / 提醒 |
| `--danger` | `#EF4444` (red-500) | 超标 / 错误 |
| `--bg-page` | `#F0FDFA` (teal-50) | 页面底色（替换 `#f5f7fa`） |
| `--bg-card` | `#FFFFFF` | 卡片 |
| `--text-primary` | `#134E4A` (teal-900) | 标题、大数字 |
| `--text-regular` | `#1F2937` (slate-800) | 正文 |
| `--text-secondary` | `#475569` (slate-600) | 辅助说明 |
| `--text-placeholder` | `#94A3B8` (slate-400) | 占位 |
| `--border-card` | `#CCFBF1` (teal-100) | 卡片描边（替换 `#dcdfe6`） |
| `--border-input` | `#E2E8F0` (slate-200) | 输入框 |

### 1.2 阴影

```scss
--shadow-card: 0 2rpx 8rpx rgba(13,148,136,0.06), 0 8rpx 24rpx rgba(13,148,136,0.08);
--shadow-pop:  0 8rpx 32rpx rgba(15,23,42,0.12);
--shadow-fab:  0 8rpx 24rpx rgba(249,115,22,0.32);
--shadow-hero: 0 12rpx 32rpx rgba(13,148,136,0.18);
```

后台（px 单位）：
```css
--el-box-shadow-card: 0 2px 8px rgba(13,148,136,0.06), 0 8px 24px rgba(13,148,136,0.08);
--el-box-shadow-pop:  0 8px 32px rgba(15,23,42,0.12);
```

### 1.3 字号层级（小程序 rpx / 后台 px）

| 层级 | 小程序 | 后台 | 字重 | 颜色 |
| ---- | ---- | ---- | ---- | ---- |
| 超大数字（核心结果） | 72rpx | 40px | 700 | text-primary |
| 大数字（KPI） | 56rpx | 32px | 700 | text-primary |
| 页面标题 | 40rpx | 20px | 700 | text-primary |
| 卡片标题 | 32rpx | 16px | 600 | text-primary |
| 正文 | 28rpx | 14px | 400 | text-regular |
| 辅助 | 24rpx | 12px | 400 | text-secondary |

### 1.4 圆角

| 元素 | 值 |
| ---- | ---- |
| 卡片 | 16rpx / 12px |
| 按钮 / 输入框 | 12rpx / 8px |
| Chip / Tag | 24rpx / 4px |
| FAB / 头像 | 50% |

### 1.5 三色语义（PRD §5.6.3 保留）

- 充足（目标×80% ≤ 摄入 ≤ 目标）→ `#22C55E`
- 不足（摄入 < 目标×80%）→ `#F59E0B`
- 超标（摄入 > 目标）→ `#EF4444` + 「已超标 XXg」

渗透位置：进度条、首页 hero 进度条、徽章、提示卡。

## 2. 小程序端改造点

### 2.1 全局
- `uni.scss`：替换主色 / 背景 / 描边 / 文字色，新增阴影变量
- `pages.json`：`navigationBarBackgroundColor` 保留白色；`backgroundColor` 改 `#F0FDFA`；tabBar `selectedColor` 改 `#0D9488`
- `App.vue` 全局 `.panel`：border 改 `1rpx solid #CCFBF1`，新增 `box-shadow: var(--shadow-card)`；`.btn-primary` 改橙色 `#F97316`，新增 `:active` 按压态（scale 0.97 + 深色）

### 2.2 页面
| 页面 | 改造 |
| ---- | ---- |
| 我的 mine/index.vue | 6 个 emoji 替换为 inline SVG（user / refresh / bell / scale / droplet / lock），20rpx teal 描边 |
| 首页 home/index.vue | hero 进度条由白改三色（按 rate）；空态 emoji 改大号 SVG |
| 代谢结果 body/result.vue | 主结果数字 40rpx 黑 → 72rpx / 700 / teal-900 |
| 记录页 record/index.vue | FAB 改橙色渐变 + `--shadow-fab`；空态插画化 |
| 食物库 food/index.vue | 空态插画化 |
| 身体数据 body/profile.vue | 输入框 focus 态加 teal 描边 |

## 3. 后台改造点

### 3.1 全局
- `styles/global.css`：注入 EP CSS 变量
  ```css
  --el-color-primary: #0D9488;
  --el-color-primary-light-3: #14B8A6;
  --el-color-primary-light-5: #5EEAD4;
  --el-color-primary-light-7: #99F6E4;
  --el-color-primary-light-8: #CCFBF1;
  --el-color-primary-light-9: #F0FDFA;
  --el-color-primary-dark-2: #0F766E;
  ```
- 同步替换 `--zhenxinjian-primary: #0D9488` 等

### 3.2 页面
| 页面 | 改造 |
| ---- | ---- |
| login/index.vue | 副标题改「生活化减脂 · 管理后台」；背景 teal 渐变；卡片品牌化 |
| dashboard/index.vue | 4 卡加图标（User/Coin/Food/Document）+ 浅色渐变底 + hover 阴影 + 主色数字；ECharts 折线改 teal |
| users/index.vue | 操作列加 `View / Edit / Delete` 图标；`el-empty` 空态 |
| foods/index.vue | 同上 |
| diet-records/index.vue | 操作列 `View` 图标；`el-empty` 空态 |
| layout/MainLayout.vue | 顶栏下方加面包屑（路由 meta.title 驱动） |
| router/index.ts | 路由 meta 配 title；新增 catch-all 404 |
| component/PagePager.vue | 分页 layout 加 `sizes, jumper` |
| view/error/404.vue（新增） | 简单 404 页（插画 + 返回首页按钮） |

## 4. 不做清单（反模式）

- ❌ 不引入 Neumorphism（低对比度、不适合数据密集）
- ❌ 不大面积使用 Glassmorphism / backdrop-filter（小程序低端 Android 性能差）
- ❌ 不保留 teal 与 EP 蓝双主色并存
- ❌ 不改 PRD §5.6.3 三色判定阈值
- ❌ 不删除任何业务逻辑、不改接口

## 5. 兼容与回滚

- 全部改动为 CSS / 模板层，无 schema、无 API 变更，回滚 = `git revert`
- 小程序端 H5 / 微信小程序双端共用同一套 rpx 样式，无需适配差异
- 后台 EP 变量仅覆盖 `--el-color-primary` 一族，其余 EP 出厂变量保留

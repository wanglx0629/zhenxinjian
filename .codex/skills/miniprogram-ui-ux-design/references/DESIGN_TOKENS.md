# 设计 Token

## 1. Token 设计方法论

### 三层架构

```
全局 Token（app.wxss 定义）
  → 项目 Token（组件库/主题级别）
    → 组件 Token（单个组件内）
```

**命名约定：** `{category}-{property}-{variant}`

| 类别 | 示例 |
|------|------|
| color | `color-primary-500` `color-neutral-100` `color-success` |
| spacing | `spacing-xs` `spacing-md` `spacing-2xl` |
| radius | `radius-sm` `radius-md` `radius-full` |
| shadow | `shadow-1` `shadow-2` `shadow-3` |
| font | `font-body` `font-heading` `font-display` |

### WXSS 变量化

```css
/* app.wxss */
page {
  /* 间距 */
  --spacing-xs: 8rpx;
  --spacing-sm: 16rpx;
  --spacing-md: 32rpx;
  --spacing-lg: 48rpx;
  --spacing-xl: 64rpx;
  --spacing-2xl: 96rpx;
  --spacing-3xl: 128rpx;

  /* 圆角 */
  --radius-sm: 8rpx;
  --radius-md: 16rpx;
  --radius-lg: 24rpx;
  --radius-full: 50%;

  /* 阴影 */
  --shadow-1: 0 2rpx 8rpx rgba(0,0,0,0.06);
  --shadow-2: 0 8rpx 24rpx rgba(0,0,0,0.10);
  --shadow-3: 0 16rpx 48rpx rgba(0,0,0,0.14);
}
```

---

## 2. 间距体系

基于 8px 基准，映射到 rpx。基准公式（375px 宽 iPhone 6 画板）：`px × 2 = rpx`

| 语义名 | px | rpx | 用途 |
|--------|----|-----|------|
| xs | 4 | 8 | 极紧凑间距、图标与文字间隙 |
| sm | 8 | 16 | 紧凑间距、列表项内边距、标签间距 |
| md | 16 | 32 | 常规间距、卡片之间、组内间距 |
| lg | 24 | 48 | 组间间距、表单段间距 |
| xl | 32 | 64 | 卡片内边距、页面左右 padding |
| 2xl | 48 | 96 | 段落间距 |
| 3xl | 64 | 128 | 大区块间距、页面顶部/底部留白 |

---

## 3. 字体阶梯

### 系统字体栈

```css
font-family: -apple-system, "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif;
```

覆盖范围：iOS（苹方）、Android（思源黑体/Noto Sans）、Windows（微软雅黑）、Mac（苹方/Helvetica）。

**重要：** 小程序不支持 `@font-face` 加载远程字体，只能使用系统字体。

### 字体阶梯表

| 语义名 | rpx | 行高 | 用途 |
|--------|-----|------|------|
| caption | 20 | 28rpx | 辅助文字、标签、时间戳 |
| body-sm | 24 | 32rpx | 说明文字、列表副标题 |
| body | 28 | 40rpx | 正文 |
| body-lg | 32 | 44rpx | 强调正文 |
| subtitle | 36 | 48rpx | 小标题 |
| title | 40 | 52rpx | 卡片标题 |
| heading | 48 | 60rpx | 区块标题 |
| display | 60 | 72rpx | 大标题 |
| display-lg | 80 | 88rpx | 特大数字/展示型文字 |

### 可读性规则

| 规则 | 值 | 说明 |
|------|-----|------|
| 行宽 | 50~75 个中文字符/行 | 超宽用 `max-width` 限制 |
| 行高（正文） | 1.5~1.6 倍 | 字号的 1.5~1.6 倍 |
| 行高（标题） | 1.2~1.3 倍 | 紧凑但不拥挤 |
| 字间距（标题） | -0.02em | 标题略微收紧更精致 |
| 字间距（正文） | normal | 不调整 |
| 段落间距 | ≥ 一行高度 | 正文段落间至少留一行空间的间距 |

---

## 4. 色彩系统构建

### a) 构建方法

从品牌主色派生出 50-900 色阶（10 级），工具：Huevy.app / Coolors.co。

以蓝色示例：
```
blue-50:  #eff6ff (最浅，背景)
blue-100: #dbeafe
blue-200: #bfdbfe
blue-300: #93c5fd
blue-400: #60a5fa
blue-500: #3b82f6 (品牌主色)
blue-600: #2563eb
blue-700: #1d4ed8
blue-800: #1e40af
blue-900: #1e3a8a (最深，深色文字)
```

### b) 标准色板结构

| 角色 | 用途 | 示例 |
|------|------|------|
| Primary | 品牌色、CTA、链接、选中态 | 品牌主色 500 |
| Neutral | 文字、背景、边框 | 灰阶 50-900 |
| Success | 完成、成功 | 绿色 |
| Error | 错误、危险操作 | 红色 |
| Warning | 警告、提醒 | 黄色/橙色 |
| Info | 信息提示 | 蓝色 |

### c) 功能色使用规则

- **红色仅用于**：错误提示、删除/危险操作按钮
- **绿色仅用于**：成功提示、完成状态
- **品牌色用于**：CTA 按钮、链接、选中态、进度条
- **灰色用于**：文字层级（正文 700/辅助 500/禁用 300）、背景、边框

### d) 暗黑模式策略

| 规则 | 做法 |
|------|------|
| 背景 | 浅色 `#F8F9FA` → 深色 `#1A1A2E` |
| 文字 | 深灰 `#333` → 浅灰 `#E0E0E0` |
| 品牌色 | 降低饱和度 15-20%，避免刺眼 |
| 阴影 | 减少透明度、增大扩散 |
| 边框 | 增亮以在深色背景可见 |

### e) 色彩无障碍检查

**必须满足：**
- 正文对比度 ≥ 4.5:1
- 大文字（≥ 24px 或 19px bold）对比度 ≥ 3:1
- UI 组件（按钮、输入框边框）对比度 ≥ 3:1

检查工具：WebAIM Contrast Checker（https://webaim.org/resources/contrastchecker/）

### f) 2026 设计趋势

- **柔和渐变背景**：subtle ambient color shifts，不是 2015 式刺眼渐变
- **高饱和度 CTA**：让可点击元素在视觉上不可错过
- **避免纯黑纯白**：`#F8F9FA` / `#1A1A2E` 比 `#FFF` / `#000` 更舒适
- **暗黑模式不简单反色**：需调整饱和度和亮度，深色背景上的颜色需要更高亮度和更低饱和度

---

## 5. 示例主题

> 以下主题为参考示例，非固定。每个项目应从自己的品牌色出发构建。

### 示例 A - 暖食主题（食品/生活类小程序）

| Token | 浅色 | 暗黑模式 |
|-------|------|----------|
| Primary | `#FF8C42` 暖橙 | `#FFB366` 浅橙 |
| Secondary | `#F5E6D3` 米色 | `#3D3226` 深米 |
| Accent | `#5D4037` 深棕 | `#A1887F` 浅棕 |

适用场景：菜谱、美食、健康饮食

### 示例 B - 清新主题（工具/效率类小程序）

| Token | 浅色 | 暗黑模式 |
|-------|------|----------|
| Primary | `#4A90D9` 天蓝 | `#7BB3F0` 浅蓝 |
| Secondary | `#E8F0FE` 浅灰蓝 | `#1A2740` 深蓝 |
| Accent | `#1A365D` 藏蓝 | `#4A6FA5` 中蓝 |

适用场景：日程管理、笔记、计时器

### 示例 C - 活力主题（社交/社区类小程序）

| Token | 浅色 | 暗黑模式 |
|-------|------|----------|
| Primary | `#E91E63` 品红 | `#F48FB1` 浅粉 |
| Secondary | `#FCE4EC` 淡粉 | `#3D1A25` 深粉 |
| Accent | `#4A148C` 深紫 | `#9C27B0` 中紫 |

适用场景：社区、打卡、运动

---

## 6. 圆角系统

| Token | 值 | 用途 |
|-------|-----|------|
| sm | 8rpx | 标签、徽章、小按钮 |
| md | 16rpx | 卡片、输入框、列表项 |
| lg | 24rpx | 面板、弹窗、大卡片 |
| full | 50% | 胶囊按钮、头像、圆形图标 |

---

## 7. 阴影层级

| 级别 | 值 | 用途 |
|------|-----|------|
| 1 (浮起) | `0 2rpx 8rpx rgba(0,0,0,0.06)` | 卡片 |
| 2 (弹出) | `0 8rpx 24rpx rgba(0,0,0,0.10)` | 弹窗、下拉菜单 |
| 3 (最高) | `0 16rpx 48rpx rgba(0,0,0,0.14)` | 模态框 |

> 暗黑模式：阴影几乎不可见，改用 `box-shadow` 减淡或替换为细边框。

---

## 8. 图标体系

### 尺寸阶梯

| Token | rpx | 用途 |
|-------|-----|------|
| sm | 32 | 列表项图标、行内图标 |
| md | 48 | 卡片图标、Tab 图标 |
| lg | 64 | 空状态插图、功能入口 |
| xl | 96 | 启动页图标、品牌标识 |

### 风格建议

- **线框风格**：简洁、不妨碍内容——推荐作为默认
- **面性风格**：强调、引导注意力

### 来源

| 来源 | 格式 | 特点 |
|------|------|------|
| iconfont (Alibaba) | 字体图标/Unicode | 体积小、CSS color 控制颜色 |
| SVG 内联 | XML in WXML | 可交互动画、完全可控 |
| PNG 图片 | @2x/@3x | 多色图标、复杂图形 |

**推荐：** 使用单色 iconfont + CSS color 控制颜色，通过 `--icon-color` Token 统一管理。

---

## 9. 项目定制指南

### 从模板派生你的 Token

1. 确定品牌主色 → 用 Coolors/Huevy 生成 50-900 色阶
2. 从间距表选择需要的 Token（至少 xs / sm / md / lg）
3. 从字体表选择需要的 Token（至少 caption / body / subtitle / heading）
4. 定义圆角、阴影的默认值
5. 准备暗黑模式对应值

### app.wxss 全局变量示例

```css
page {
  /* 项目自定义色彩（替换为你的品牌色） */
  --color-primary: #4A90D9;
  --color-primary-light: #E8F0FE;
  --color-primary-dark: #1A365D;
  --color-success: #4CAF50;
  --color-error: #E53935;
  --color-warning: #FF9800;

  /* 文字色 */
  --text-primary: #333333;
  --text-secondary: #888888;
  --text-disabled: #BBBBBB;

  /* 背景色 */
  --bg-page: #F8F9FA;
  --bg-card: #FFFFFF;

  /* 暗黑模式覆写 */
  @media (prefers-color-scheme: dark) {
    --text-primary: #E0E0E0;
    --text-secondary: #999999;
    --text-disabled: #555555;
    --bg-page: #1A1A2E;
    --bg-card: #252540;
  }
}
```

### Token 覆盖策略

```
全局默认（app.wxss）
  → 页面级覆写（页面 .wxss 重新定义变量）
    → 组件内覆写（组件 .wxss 内覆盖）
```

变量作用域逐层收紧，高层默认，低层可覆盖。

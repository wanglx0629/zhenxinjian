# 小程序基础规范

## 1. rpx 单位体系

**换算公式：** `1rpx = 屏幕宽度 / 750`

| 设备 | 屏幕宽度 (px) | 1rpx 约等于 | 换算关系 |
|------|-------------|------------|----------|
| iPhone 6/7/8/SE | 375 | 0.5 px | px × 2 = rpx |
| iPhone 14 Pro | 393 | 0.524 px | px × 1.91 ≈ rpx |
| iPhone 14 Pro Max | 430 | 0.573 px | px × 1.74 ≈ rpx |
| iPad | 768 | 1.024 px | px × 0.98 ≈ rpx |

**实用换算速查（375px 宽画板基准）：**

| px | rpx |
|----|-----|
| 4 | 8 |
| 8 | 16 |
| 12 | 24 |
| 16 | 32 |
| 20 | 40 |
| 24 | 48 |
| 32 | 64 |
| 48 | 96 |
| 64 | 128 |

---

## 2. 安全区域 (Safe Area)

**问题：** iOS 设备底部有 Home Indicator 横条（34px），会遮挡固定定位的底部元素。

**解决方案：**
```css
/* 页面底部容器 */
.bottom-fixed {
  padding-bottom: constant(safe-area-inset-bottom); /* iOS 11.0-11.1 */
  padding-bottom: env(safe-area-inset-bottom);      /* iOS 11.2+ */
}
```

**常见场景：**
- 底部固定按钮：加 `padding-bottom: env(safe-area-inset-bottom)`
- 全屏自定义导航栏：标题栏需偏移状态栏高度（`wx.getSystemInfoSync().statusBarHeight`）
- TabBar 页面：原生 TabBar 会自动处理，自定义 TabBar 需手动适配

---

## 3. 暗黑模式适配

**开启方式（app.json）：**
```json
{
  "darkmode": true,
  "themeLocation": "theme.json"
}
```

**theme.json 定义变量：**
```json
{
  "light": { "bgColor": "#FFFFFF", "textColor": "#333333" },
  "dark":  { "bgColor": "#1A1A2E", "textColor": "#E0E0E0" }
}
```

**WXSS 中使用：**
```css
/* 自动跟随系统主题 */
.page { background-color: var(--bgColor); }

/* 或手动指定深色样式 */
@media (prefers-color-scheme: dark) {
  .card { background: #1A1A2E; }
}
```

**暗黑模式注意事项：**
- 不是简单反色——需要调整饱和度和亮度
- 阴影在暗黑模式下几乎不可见，改用边框或亮度差区分层级
- 纯白文字 on 纯黑背景刺眼，使用 `#E0E0E0` on `#1A1A2E`

---

## 4. 多机型适配策略

**rpx 不是万能的** —— rpx 解决宽度自适应，但不解决长宽比问题。

| 场景 | 策略 |
|------|------|
| 宽度自适应 | rpx 即可，无需 media query |
| 长宽比差异 | 小屏用 `scroll-view` 纵向滚动、大屏用多列或更大内边距 |
| 小屏机（320px 逻辑宽） | 减少装饰性元素、增大可点击区域占比 |
| iPad 大屏 | 限制内容最大宽度 `max-width: 750rpx` 居中，避免完全撑满 |
| 横屏 | `wx.onDeviceOrientationChange` 监听，或 `pageOrientation: "auto"` |

**安全区域 + 多机型综合示例：**
```css
.page {
  width: 100vw;
  min-height: 100vh;
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
}
```

---

## 5. 导航栏与 TabBar 设计

### 原生导航栏
- `navigationBarTitleText`：标题文字
- `navigationBarBackgroundColor`：背景色
- `navigationBarTextStyle`：`"black"` / `"white"`

### 自定义导航栏
```json
{ "navigationStyle": "custom" }
```
- 需手动处理状态栏高度：`wx.getSystemInfoSync().statusBarHeight`
- 标准导航栏高度：`statusBarHeight + 44px`（iOS）/ `statusBarHeight + 48px`（Android）
- 推荐封装为 `navigation-bar` 自定义组件

### TabBar 决策
| 方式 | 优点 | 缺点 |
|------|------|------|
| 原生 TabBar | 自动适配、性能好 | 样式受限、最多 5 个 Tab |
| 自定义 TabBar | 完全控制样式 | 需手动处理安全区域、页面切换动画 |
| 无 TabBar | 最灵活 | 需自行设计导航方案 |

---

## 6. WXSS 能力与限制

### ✅ 完全支持
- `display: flex` (Flexbox)
- `position: relative / absolute / fixed`
- `transform` (translate/scale/rotate)
- `transition` (all properties)
- `animation` + `@keyframes`
- CSS 变量 (`--custom-property`)
- `box-shadow`、`border-radius`
- `z-index`

### ❌ 不支持或不完整
- `display: grid` — 几乎不可用，使用 Flexbox 替代
- `backdrop-filter` — 不支持
- `clip-path` — 不支持
- `::before` / `::after` — 部分基础库版本支持但不稳定
- `@font-face` — 不支持远程字体文件
- `vh` / `vw` — 支持但表现与 Web 有差异

### 选择器注意事项
- 支持：class、id、element、attribute、descendant、`:first-child`、`:last-child`
- 权重计算与 CSS 一致，避免过度使用 `!important`

---

## 7. 布局策略

**核心原则：Flexbox 为王。**

```css
/* 最常用的组合：纵向弹性布局 */
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.content { flex: 1; }      /* 中间内容区自动填满 */
.footer  { flex-shrink: 0; } /* 底部固定不压缩 */

/* 横向排列 */
.row { display: flex; flex-direction: row; align-items: center; }

/* 自动换行（替代 CSS Grid） */
.grid { display: flex; flex-wrap: wrap; }
.grid > .item { width: calc((100% - 2 * 32rpx) / 3); } /* 3 列等宽 */

/* 两端对齐 */
.space-between { display: flex; justify-content: space-between; }
```

**不用 media query** —— rpx 自动处理宽度，只需在极端长宽比时用条件渲染调整结构。

---

## 8. 交互模式设计

| 交互 | 实现方式 |
|------|----------|
| 下拉刷新 | 页面 JSON: `enablePullDownRefresh: true` + `onPullDownRefresh()` |
| 上拉加载 | 页面 JSON: `onReachBottomDistance: 50` + `onReachBottom()` |
| 长按操作 | `bindlongpress="onLongPress"`（如列表项删除） |
| 滑动 | `scroll-view` + `bindscroll`（如横向 Tab 切换） |
| 双击 | `bindtap` 内判断时间间隔 < 300ms |
| 多点触控 | `bindtouchstart` / `bindtouchmove` / `bindtouchend` |

---

## 9. 键盘处理

| 问题 | 方案 |
|------|------|
| 表单聚焦页面自动上移 | 设置 `adjust-position="{{false}}"` 手动控制 |
| 底部按钮被键盘顶起 | 监听 `bindfocus` 隐藏底部固定元素 |
| 光标与键盘间距 | `cursor-spacing` 属性（默认 0，建议设为 24rpx） |
| 键盘收起后页面不回弹 | `bindblur` 中 `wx.pageScrollTo({ scrollTop: 0 })` |

---

## 10. 授权与系统弹窗设计

**授权 UX 原则：** 先告知用途再请求，被拒绝后引导到设置页。

```javascript
// 获取用户信息授权流程
wx.getSetting({
  success(res) {
    if (!res.authSetting['scope.userInfo']) {
      // 显示说明弹窗，让用户理解用途
      wx.showModal({
        title: '需要您的授权',
        content: '用于展示个人头像和昵称',
        success(r) {
          if (r.confirm) wx.authorize({ scope: 'scope.userInfo' })
        }
      })
    }
  }
})
```

**系统弹窗使用：**
- `wx.showModal`：确认/取消操作
- `wx.showActionSheet`：选项列表（最多 6 项）
- `wx.showToast`：轻量提示（成功/失败/加载）

---

## 11. setData 性能指南

| 规则 | 说明 |
|------|------|
| 单次数据量 | < 256KB |
| 高频调用 | 控制在每秒 30 次以内（如倒计时每秒 1 次完全 OK） |
| 局部更新 | 只 setData 变化的部分，不要全量替换 |
| 避免传输视图不需要的数据 | data 中只放渲染需要的数据 |

```javascript
// ✅ 局部更新
this.setData({ 'user.name': '新名字' })

// ❌ 全量更新
this.setData({ user: { ...this.data.user, name: '新名字' } })
```

---

## 12. 图片与资源策略

### image 组件的 mode 属性

| mode | 行为 | 适用场景 |
|------|------|----------|
| `aspectFill` | 等比缩放填满，超出裁剪 | 封面图、头像 |
| `aspectFit` | 等比缩放完整显示 | 详情图 |
| `widthFix` | 宽度固定，高度自适应 | 文章配图 |
| `scaleToFill` | 拉伸填满（变形） | 几乎不用 |

**其他策略：**
- `lazy-load`：长列表图片懒加载
- 占位图：设置默认 `src` 或用背景色块
- 加载失败：`binderror` 切换到 fallback 图
- 网络图优先，本地图用于 UI 装饰

---

## 13. 文件组织最佳实践

```
miniprogram/
├── app.ts / app.json / app.wxss    # 全局配置
├── pages/                          # 页面
│   └── index/
│       ├── index.ts / .json / .wxml / .less
├── components/                     # 公共组件
│   └── navigation-bar/
│       ├── navigation-bar.ts / .json / .wxml / .less
├── assets/
│   ├── images/                     # 图片资源
│   └── audio/                      # 音频资源
├── utils/                          # 工具函数
├── services/                       # API 调用
└── models/                         # 数据模型
```

---

## 14. 反模式与常见错误

| # | 反模式 | 正确做法 |
|---|--------|----------|
| 1 | 照搬 Web（px/rem、CSS Grid、Web 字体） | rpx、Flexbox、系统字体 |
| 2 | 过度嵌套 view（> 5 层） | 扁平化结构，减少渲染层级 |
| 3 | setData 传大对象 / 高频调用 | 局部更新、控制频率 |
| 4 | 忽略 darkmode 适配 | 从设计之初定义双主题 |
| 5 | 触摸区域 < 88rpx | 最小 88rpx，间距 ≥ 16rpx |
| 6 | 忽略安全区域（底部按钮被遮挡） | 见第 2 节 |
| 7 | `scroll-view` 内用 `position: fixed` | 固定元素放到 scroll-view 外部 |

---

## 15. UI 组件库参考

> ⚠️ 本 Skill 所有代码示例均使用原生 WXML + WXSS，不依赖任何第三方组件库。

**已知可选组件库（信息性，非推荐）：**
- **WeUI**：微信官方，极简风格，与微信原生风格完全一致
- **Vant Weapp**：有赞维护，电商风格，组件较丰富

**决策框架：**
- **用组件库**：团队大、需统一规范、快速上线
- **自己写**：轻量需求、包体敏感（主包 2MB 上限）、追求极致定制
- **建议**：先用原生自定义跑通 MVP，再评估是否需要引入组件库

# 无障碍

## 1. 对比度要求

| 类型 | 最小对比度 | 适用范围 |
|------|-----------|----------|
| 正文 | 4.5:1 | 字号 < 24px 或 < 19px bold 的所有文字 |
| 大文字 | 3:1 | 字号 ≥ 24px 或 ≥ 19px bold |
| UI 组件 | 3:1 | 按钮、输入框边框、图标 vs 背景 |

检查工具：WebAIM Contrast Checker（https://webaim.org/resources/contrastchecker/）

**常见失败案例：**
- `#999` 文字 on `#FFF` 背景 = 2.85:1 ❌ → 用 `#767676` 或更深
- 蓝色链接 `#2196F3` on 深色背景 `#1A1A2E` = 仅 2.3:1 ❌ → 增大亮度
- placeholder 文字 `#CCC` on `#FFF` = 1.6:1 ❌ → 用 `#999` 以上

### 暗黑模式额外检查

暗黑模式下文字倾向更亮，背景更暗，同样需要满足对比度要求。

---

## 2. 触摸目标规范

| 规则 | 值 | 说明 |
|------|-----|------|
| 最小触摸区域 | 88 × 88 rpx | 对应 44 × 44 px（Apple HIG / Material Design 标准） |
| 按钮间距 | ≥ 16 rpx | 防止误触相邻按钮 |
| 小图标按钮 | 用 padding 补足到 88rpx | 如 32rpx 图标 + 28rpx padding = 88rpx |

---

## 3. 语义化 WXML

### view vs button

```xml
<!-- ✅ 正确：操作用 button -->
<button bindtap="onSubmit">提交</button>

<!-- ❌ 错误：操作用 view -->
<view bindtap="onSubmit">提交</view>
```

button 元素自动被屏幕阅读器识别为可操作元素，view 则不会。

### 标题层级

WXML 没有 h1-h6 标签，但应通过字号/颜色区分层级，保持**一页一个主标题、层级不跳**：

```
主标题（display 或 heading）
  → 子标题（title）
    → 正文（body）
```

屏幕阅读器虽不感知字号，但视觉层级清晰有助于所有用户。

### aria 支持说明

小程序当前对 `aria-role` / `aria-label` 支持有限。替代方案：
- 通过 `text` 元素显式提供文字描述
- icon-only 按钮添加紧随其后的 `text` 标签
- image 必须设置 `alt` 文本或声明纯装饰用途

---

## 4. 表单无障碍

### 输入框标签

```xml
<!-- ✅ label 关联输入框 -->
<label>
  <text>姓名</text>
  <input placeholder="请输入姓名" />
</label>
```

### 错误信息关联

```xml
<view class="form-item">
  <text class="form-label">手机号</text>
  <input value="{{phone}}" bindinput="onPhoneInput" />
  <!-- 错误信息紧贴输入框下方，而非仅 showToast -->
  <text class="form-error" wx:if="{{phoneError}}">请输入正确的手机号</text>
</view>
```

错误信息必须：
- 出现在对应输入框下方（空间关系）
- 红色 + 文字双重标识（颜色不自成一派）
- `wx.showToast` 可作为补充，但不能替代内联错误文案

### 必填字段

```xml
<label>
  <text class="required">姓名</text>
  <input />
</label>
```

```css
.required::after { content: " *"; color: var(--color-error); }
```

### 验证失败

验证失败时自动聚焦到第一个有错误的输入框：
```javascript
wx.createSelectorQuery().select('.form-error').boundingClientRect().exec(...)
```

---

## 5. 色彩无障碍设计

**核心原则：不唯一依赖颜色区分状态。**

| 状态 | 颜色 + 什么 | 示例 |
|------|------------|------|
| 选中 | 品牌色 + ✓ 图标 + 文字加粗 | 熟度选择 |
| 错误 | 红色 + ✗ 图标 + 错误文字 | 表单验证 |
| 完成 | 绿色 + ✓ 图标 + "已完成" 文字 | 进度完成 |

**色盲友好考量：**
- 红绿色盲占比约 8%（男性中更高）
- 避免红绿对比作为唯一区分手段
- 用图标、文字辅助状态识别

**模拟工具：** Chrome DevTools → Rendering → Emulate vision deficiencies

---

## 6. 字体大小偏好适配

微信支持用户在系统设置中调整字体大小，影响小程序渲染。

**应对策略：**
- 使用 rpx（相对单位），不要用固定 px
- 关键文字内容避免放入固定高度容器（如 `height: 40rpx; overflow: hidden` 可能截断放大后的文字）
- 列表项使用 `min-height` 而非 `height`，允许内容撑高

---

## 7. 冗余提示原则

重要信息应通过多条通道同时传递：

| 通道 | 方式 | 适用场景 |
|------|------|----------|
| 视觉 | 颜色变化、图标、动画 | 所有场景 |
| 文字 | 内联文案、toast | 状态变化、错误 |
| 语音 | 同声传译插件 TTS | 计时完成、警告 |

降级方案：
- TTS 不可用时 → `wx.showToast` 文字提示
- Toast 太短时 → `wx.showModal` 模态确认

---

## 8. 屏幕阅读器

### iOS VoiceOver
- WXML 的 `text` 元素默认可被 VoiceOver 朗读
- `button` 元素朗读时会附加"按钮"语义
- `image` 需要 `alt` 文本，否则静默跳过

### Android TalkBack
- 行为与 VoiceOver 类似
- `aria-label` 在部分 Android 版本上可能不生效，建议优先用显式 `text`

### image / icon 策略
```xml
<!-- 内容图片：必加 alt -->
<image src="{{egg}}" alt="溏心蛋成品图" />

<!-- 装饰性图片：空 alt -->
<image src="{{decorative}}" alt="" />
```

---

## 9. 焦点与键盘

**小程序限制：** 没有 Web 式的 Tab 键盘导航。所有交互依赖触摸。

**仍需注意：**
- 所有可交互元素必须可触摸（≥ 88rpx）
- 页面结构需语义化（见第 3 节）
- 表单类页面确保 `focus="{{true}}"` 合理设置首个输入框

---

## 10. 可访问性测试方法

### 自动检测
- 微信开发者工具 → Audits 面板 → 运行可访问性检查（基础可用性评分）

### 对比度检查
- WebAIM Contrast Checker（输入 HEX 值即可，不依赖浏览器）

### 手动测试三步法

**a) 屏幕阅读器遍历：**
开启系统 VoiceOver（iOS）或 TalkBack（Android），从首页开始遍历关键流程，确保：
- 所有交互元素可被朗读
- 朗读内容有意义（不是"按钮按钮"）
- 图片有 alt 文本或明确为装饰性

**b) 字体放大验证：**
系统设置 → 显示 → 字体大小 → 调到最大，检查：
- 文字不被截断
- 按钮足够大能容纳放大的文字
- 页面不出现水平滚动

**c) 色彩独立性验证：**
系统设置 → 辅助功能 → 灰度模式，或 Chrome DevTools 模拟色盲，检查：
- 所有状态在无颜色下仍可区分
- 重要信息不依赖颜色传达

---

## 11. 动态内容通知

小程序没有 `aria-live` 区域，替代方案：

| 场景 | 方案 |
|------|------|
| 即时反馈 | `wx.showToast`（成功/失败/加载） |
| 异步通知 | 同声传译插件 TTS 语音播报 |
| 状态文字变更 | 改变 `text` 的 `{{content}}`，屏幕阅读器自动感知新文本 |

---

## 12. 检查清单

- [ ] 所有 `image` 有 `alt` 文本或标记为装饰性（`alt=""`）
- [ ] 正文对比度 ≥ 4.5:1，大文字 ≥ 3:1，UI 组件 ≥ 3:1
- [ ] 触摸目标 ≥ 88×88rpx
- [ ] 不依赖颜色作为唯一信息传达方式（颜色 + 图标 + 文字至少选二）
- [ ] 按钮间距 ≥ 16rpx
- [ ] 暗黑模式下对比度已重新检查
- [ ] 屏幕阅读器可获取所有关键信息
- [ ] 字体放大后布局不崩溃
- [ ] 语音播报存在降级方案（showToast / showModal）
- [ ] 标题层级清晰（一页一个主标题、层级不跳）
- [ ] 表单错误信息关联到对应输入框（内联而非仅 toast）
- [ ] 手动测试三步法已执行（屏幕阅读器 + 字体放大 + 色彩独立性）

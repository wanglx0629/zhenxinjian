# 微信小程序 UI/UX 设计

**Name:** miniprogram-ui-ux-design  
**Description:** 微信小程序 UI/UX 设计规范与最佳实践。适用于设计小程序界面、选择配色和字体、实现 WXSS 布局、创建组件模式、确保无障碍合规等场景。覆盖 rpx 单位体系、Flexbox 布局策略、安全区域适配、暗黑模式、Design Token 方法论、30 个 WXML+WXSS 组件模式、WCAG 无障碍指南（含小程序约束）、以及反模式清单。  
**Version:** 1.0.1

---

## 何时使用

在以下场景加载此 Skill：
- 设计微信小程序界面（页面布局、组件选择、配色字体）
- 编写 WXSS 样式（布局策略、响应式适配、动画微交互）
- 确保小程序 UI 的无障碍合规（对比度、触摸目标、语义化）
- 从小程序 UI 角度审查设计稿
- 选择或创建 Design Token 体系

---

## 设计哲学

> "rpx 优先、Flexbox 为主、系统字体、触屏优先"

小程序 UI 的核心约束：单一纵向滚动、触屏交互、rpx 自动适配、WXSS CSS 子集、无 Web 字体。

---

## 设计铁律速查

### 1. 对比度创造层级（大小对比、深浅对比）
- 正文 ≥ 4.5:1，大文字 ≥ 3:1，UI 组件 ≥ 3:1
- 最重要信息用最大反差

### 2. 留白创造呼吸感
- 不怕空白——留白 = 高级感
- 间距基准 8px → rpx 映射（见 DESIGN_TOKENS.md）
- 更多选择 = 更慢决策（Hick's Law），用留白限制视觉噪声

### 3. 一致性建立信任
- 同类组件相同样式、相同行为
- 一套 Design Token 贯穿全局

### 4. 反馈确认操作
- 每个操作必须有即时反馈（动效、状态切换、toast）
- 微交互：0.2-0.3s、transform+opacity（GPU 加速）

### 5. 无障碍包容所有人
- 对比度、触摸目标 88rpx、不唯一依赖颜色
- 语音播报作为视觉信息补充

---

## 视觉层级速查

### 引导用户注意力的技术
| 技术 | 用法 |
|------|------|
| 大小 | 越大越重要（display > heading > body） |
| 颜色 | 高饱和度/高对比 = 吸引注意力（CTA 应突出） |
| 留白 | 周围留白越多 = 越重要 |
| 邻近 | 相关内容放在一起 |
| 对比 | 深 on 浅，或浅 on 深（永远不用低对比度） |

### 阅读模式
- **Z 模式**：左上 Logo → 右上 CTA → 中间内容 → 右下 CTA（适合单屏页面）
- **F 模式**：扫标题 → 读每段首行 → 下移扫描（适合列表/内容页）

---

## rpx 单位速查

```
1rpx = 屏幕宽度 / 750
iPhone 6 (375px):  1rpx = 0.5px  →  px × 2 = rpx
iPhone 14 Pro:     1rpx ≈ 0.524px
iPhone SE:         1rpx = 0.5px
```

**转换公式：** 设计稿 px 值 × 2 = rpx 值（375px 宽画板基准）

---

## WXSS 能力边界速查

| ✅ 支持 | ❌ 不支持或不完整 |
|---------|-------------------|
| Flexbox | CSS Grid |
| position (relative/absolute/fixed) | backdrop-filter |
| transform / transition / animation | clip-path |
| @keyframes | ::before/::after（部分支持） |
| CSS 变量（--token） | @font-face 远程字体 |

---

## 字体策略

小程序**不支持加载 Web 字体**（@font-face 远程字体不可用）。

**系统字体栈：**
```css
font-family: -apple-system, "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif;
```

仅使用系统字体，覆盖 iOS（苹方）、Android（思源/Noto）、Windows（微软雅黑）。

---

## 小程序 Top 5 反模式

| # | 反模式 | 正确做法 |
|---|--------|----------|
| 1 | 照搬 Web 习惯（px/rem、CSS Grid、Web 字体） | rpx、Flexbox、系统字体 |
| 2 | setData 传输大对象或高频调用 | 局部更新、节流、单次 < 256KB |
| 3 | 过度嵌套 view 层级 | 控制在 5 层以内，减少渲染开销 |
| 4 | 忽略 darkmode 适配 | 从设计之初就定义双主题 Token |
| 5 | 触摸区域 < 88rpx | 最小 88rpx，按钮间距 ≥ 16rpx |

---

## 微交互速查

| 场景 | 方式 | 时长 |
|------|------|------|
| 按钮按下 | hover-class + transform: scale(0.97) | 0.15s |
| 元素出现 | opacity 0→1 + translateY | 0.3s ease-out |
| 加载中 | 骨架屏 pulse 动画 | 持续 |
| 成功完成 | 绿色对勾 fade-in | 0.3s |
| 错误 | 轻微抖动（translateX ±4rpx）+ 红色高亮 | 0.3s |

**只动 `transform` 和 `opacity`** —— 这两个属性由 GPU 加速，不触发重排。

---

## Pre-Build 检查清单

设计完成、开始写代码前逐项确认：

- [ ] rpx 单位已用于全部尺寸（无 px/rem 残留）
- [ ] 布局使用 Flexbox（无 CSS Grid）
- [ ] 色彩方案定义了 Primary + Neutral + Success + Error + Warning
- [ ] 字体仅使用系统字体栈（无 Web 字体引用）
- [ ] 暗黑模式配色已准备（@media prefers-color-scheme）
- [ ] 安全区域 bottom 已适配（底部固定元素有 padding-bottom）
- [ ] 所有交互状态已设计（默认/按下/禁用/空/加载/错误）
- [ ] 正文对比度 ≥ 4.5:1，触摸目标 ≥ 88×88rpx
- [ ] 多机型布局已验证（小屏 320px ~ iPad 1024px）
- [ ] 反模式清单已自查（见上方 Top 5）

---

## 设计参考与灵感

### 小程序 UI 参考
- **微信读书**：极简排版、暗黑模式标杆
- **滴滴出行**：地图+信息层级处理
- **豆瓣**：内容驱动设计、评分可视化
- **麦当劳小程序**：品牌色运用、点餐流程

### 设计系统参考
- **WeUI**：微信官方设计语言，与微信原生风格一致
- **Apple HIG**：触控规范、动效指南、安全区域
- **Material Design 3**：间距系统、组件状态、色彩方法

### 灵感搜索
- Dribbble：`"mini program UI"` `"mobile timer"` `"food app"`
- 微信开发者社区：小程序设计案例

## 设计工具汇总

| 工具 | 用途 |
|------|------|
| Figma | 设计稿（375px 宽画板 = iPhone 6 基准） |
| WebAIM Contrast Checker | 检查任意 HEX 色值对比度（4.5:1 / 3:1） |
| Coolors / Huevy | 调色板生成、品牌色 50-900 色阶派生 |
| iconfont (Alibaba) | 字体图标（单色 SVG，通过 CSS color 控制） |
| 微信开发者工具 | WXSS 实时预览、Audits 面板检查 |

---

## 扩展阅读

- 微信官方：《小程序设计指南》《自定义组件》《WXSS 参考》
- 排版：《Web Typography》——行宽 50-75 字符、行高 1.5x
- 动效：《Material Design Motion》——时长/缓动曲线
- 无障碍：《WCAG 2.2 快速参考》《WebAIM 检查清单》

---

## 文件索引

| 文件 | 内容 |
|------|------|
| `references/MINIPROGRAM_BASICS.md` | rpx/安全区/暗黑/多机型/布局/反模式/组件库决策（15 节） |
| `references/DESIGN_TOKENS.md` | Token 方法论/间距/字体/色彩+趋势/3 套示例主题（9 节） |
| `references/DESIGN_WORKFLOW.md` | 设计流程/Figma→WXSS 转换/协作模式（8 节） |
| `references/COMPONENTS.md` | 10 大类 30 个通用组件 WXML+WXSS 示例 |
| `references/ACCESSIBILITY.md` | WCAG 原则 + 小程序无障碍/测试方法/检查清单（12 节） |

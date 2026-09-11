# 设计流程

## 1. 设计流程总览

```
PRD 需求 → 信息架构 → 线框图 → 高保真 → 标注交付 → WXSS 开发
```

---

## 2. 信息架构

### 页面树设计（Page Stack）

在开始画 UI 前，先确定页面结构：
```
首页 → 详情页 → 编辑页
     → 设置页 → 关于页
```

### 用户动线

梳理主要任务路径，确保核心流程 ≤ 3 步完成。

### 页面模式决策

| 模式 | 适用场景 |
|------|----------|
| 一页式 | 功能单一、无需导航（如计算器、计时器） |
| 多页式 | 功能有层级、需要返回导航 |
| Tab 式 | 2-5 个平级功能入口 |
| 混合式 | Tab 内嵌套多页（最常见） |

---

## 3. 线框图阶段

**方法：** 纸上草图 → 低保真数字稿

**关注点：**
- 内容布局（元素位置、大小关系）
- 信息层级（主次分明）
- **不关注配色**（用灰度表示即可）

**工具：** Figma / Sketch / 直接在微信开发者工具中画 WXML 骨架

---

## 4. 高保真阶段

### Figma 设计稿规范

| 规范 | 值 |
|------|-----|
| 画板宽度 | 375px（iPhone 6 基准） |
| 栅格系统 | 8px 基准网格 |
| 图层命名 | 语义化（`btn-primary-default` 而非 `Rectangle 12`） |

### 组件化设计

优先用已定义的组件（Button / Card / List）搭建页面，保持一致性。

### 状态覆盖（每个组件必须覆盖）

- 默认态
- 按下态（active/hover-class）
- 禁用态
- 空状态（无数据时）
- 错误状态（加载失败时）
- 加载中状态（骨架屏）

---

## 5. 标注与切图

### Figma 标注值 → rpx 换算

```
Figma 标注值 (px) × 2 = rpx 值
```

375px 宽画板下 1px = 2rpx。

### 切图规范

| 倍率 | 用途 |
|------|------|
| @2x | 主要输出（适配绝大多数设备） |
| @3x | 可选（高清需求时补充） |

### SVG 图标

- Figma 中导出为 SVG
- 小程序中使用：img 标签引用或内联 SVG 代码
- 单色图标推荐 iconfont 字体图标方案

### 标注清单（需交付的信息）

- [ ] 颜色（HEX 值，含暗黑模式对应色）
- [ ] 间距（所有组件的 padding / margin / gap）
- [ ] 字体（字号 rpx + 行高）
- [ ] 圆角（rpx）
- [ ] 阴影（x / y / blur / spread / rgba）

---

## 6. Figma → WXSS 转换手册

| Figma 属性 | WXSS 属性 | 转换 |
|------------|----------|------|
| Width × 2 | width | 标注值 × 2 = rpx |
| Font Size × 2 | font-size | 标注值 × 2 = rpx |
| Color | background / color | 直接使用 HEX |
| Auto Layout (Vertical) | `display: flex; flex-direction: column` | 1:1 映射 |
| Auto Layout (Horizontal) | `display: flex; flex-direction: row` | 1:1 映射 |
| Auto Layout (gap) | gap | 标注值 × 2 = rpx |
| Drop Shadow | box-shadow | 直接使用 rgba 值 |
| Corner Radius | border-radius | 标注值 × 2 = rpx |

### 常见坑

| Figma 做法 | WXSS 问题 | 解决方案 |
|------------|----------|----------|
| Absolute 定位 | WXSS 的 absolute 相对于最近的 relative 祖先 | 确保父元素 `position: relative` |
| Gradient 渐变 | 小程序支持 linear-gradient | 直接使用，但部分复杂渐变可能失效 |
| 文本居中 | `text-align: center` + `line-height` | 多行文本用 flex 居中 |

---

## 7. 设计审查清单

- [ ] 所有交互状态已覆盖（默认/按下/禁用/空/加载/错误）
- [ ] 暗黑模式配色已定义
- [ ] 安全区域适配已处理
- [ ] 对比度 ≥ 4.5:1（正文）/ 3:1（大文字/UI 组件）
- [ ] 触摸区域 ≥ 88×88rpx
- [ ] 所有图标有替代文字或语义标记
- [ ] 与开发确认了技术可行性（WXSS 限制如 Grid、backdrop-filter）
- [ ] 多机型（小屏/iPad）布局已检查

---

## 8. 设计师与开发者协作模式

- **设计稿管理**：Figma 在线链接 + 编辑权限控制
- **标注同步**：Figma Dev Mode 自动标注，开发按 px×2=rpx 取值
- **设计走查**：开发完成后对照设计稿走查，记录差异
- **迭代反馈**：问题记录在 Figma 评论中，版本迭代时统一处理

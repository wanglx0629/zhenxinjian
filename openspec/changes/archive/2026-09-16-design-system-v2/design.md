# 设计系统 V2.0「能量引擎」— Design

> 概念：减脂的本质是能量收支。品牌身份直接建立在「能量」上——一道绿→琥珀的能量渐变同时讲述品牌与状态；数字本身就是视觉主角。
> 四个关键词落位：健康=叶绿基底；活泼=琥珀暖光与渐变；科技感=几何数字字体与精密环形仪表；新颖=品牌色与语义色合而为一。

## 1. 色板（双端唯一真源）

| Token | V1.1 旧值 | V2.0 色值 | 角色 |
| ---- | ---- | ---- | ---- |
| ink | `#134E4A` | `#10312B` | 墨绿黑：标题/正文强色（带绿调的彩色墨，非纯黑） |
| leaf / primary | `#0D9488` | `#00AC7C` | 叶绿：品牌主色 = 充足态（达标即品牌色） |
| leaf-deep / primary-dark | `#0F766E` | `#008A63` | 叶绿按压/深色态 |
| amber / cta | `#F97316` | `#FFB020` | 琥珀光：能量渐变另一端 = 不足态 |
| amber-deep / cta-active | `#EA580C` | `#E69A00` | 琥珀按压态 |
| flare / danger | `#EF4444` | `#FF4747` | 警示红：超标/危险动作 |
| mist / bg | `#F0FDFA` | `#F4F8F6` | 雾面白：页面底色（冷调微绿，非奶油色） |
| card | `#FFFFFF` | `#FFFFFF` | 卡面（不变） |
| success | `#22C55E` | `#00AC7C` | 充足 = 叶绿（与品牌同源） |
| warning | `#F59E0B` | `#FFB020` | 不足 = 琥珀 |
| border / primary-border | `#CCFBF1` | `#E3EFE9` | 卡片描边、分割（中性微绿） |
| border-input | `#E2E8F0` | `#E2E8F0` | 输入框描边（不变） |
| text-regular | `#1F2937` | `#1F2937` | 正文（不变） |
| text-secondary | `#475569` | `#475569` | 次要说明（不变） |
| text-placeholder | `#94A3B8` | `#94A3B8` | 占位（不变） |

刻意避开 Tailwind 现成色（V1.1 的病根）：`#00AC7C / #FFB020 / #FF4747 / #10312B / #F4F8F6` 均非 Tailwind 默认阶。

## 2. 渐变

- **能量渐变（品牌 = 语义）**：`linear-gradient(135deg, #00AC7C, #FFB020)` —— hero 头卡、能量环、品牌大数字、FAB；绿端=能量充足，琥珀端=尚有缺口
- **超标态**不使用渐变，独立 `#FF4747` + 文案「已超标 XXg」（PRD §5.6.3 判定阈值不变）

## 3. 三色语义（并入品牌后）

| 状态 | 判定（不变，PRD §5.6.3） | 颜色 |
| ---- | ---- | ---- |
| 充足 | 目标×80% ≤ 摄入 ≤ 目标 | 🟢 `#00AC7C` |
| 不足 | 摄入 < 目标×80% | 🟡 `#FFB020` |
| 超标 | 摄入 > 目标 | 🔴 `#FF4747` |

## 4. 字体

- **数字/展示**：`"Space Grotesk", "DIN Alternate", -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`；后台以本地/打包字体文件提供（禁止运行时 CDN 拉取字体），小程序端声明同名 font-family 自然降级系统栈
- **中文/正文**：系统黑体栈（沿用现状），不引入第二中文家族
- 核心结果大数字：72rpx（小程序）/ 40px（后台）、字重 700–800、`font-variant-numeric: tabular-figures`；数字可叠加能量渐变文字
- 层级沿用 V1.1 §7.4（标题 30rpx/600、正文 26–28rpx、辅助 22–24rpx）

## 5. 阴影分层（rpx / px 同比例）

| 层级 | 值 | 用途 |
| ---- | ---- | ---- |
| card | `0 2rpx 8rpx rgba(0,172,124,.05), 0 8rpx 24rpx rgba(16,49,43,.06)` | 内容卡片 |
| pop | `0 8rpx 32rpx rgba(16,49,43,.14)` | 弹层/下拉 |
| fab | `0 8rpx 24rpx rgba(255,176,32,.35)` | 悬浮行动按钮 |
| hero | `0 12rpx 32rpx rgba(0,172,124,.22)` | 顶部能量头卡 |

原则：卡片层级优先靠「白卡面 + 描边 + mist 底色」的明度差表达，阴影只做轻提示，不统一铺灰阴影。

## 6. 能量环组件规范（新视觉语言）

- 结构：conic-gradient 环形仪表（进度弧）+ 中央挖空白底 + 环内大数字（当日摄入/目标）
- 进度弧走能量渐变；未走完的缺口段露出琥珀 → 「缺口即预算」的直觉表达
- 描边宽度 16–20rpx，环直径约 360rpx（首页）；环外右侧放剩余预算等辅助数字，左对齐
- 超标态：环满且切换 `#FF4747` 单色 + 超标文案
- 首页布局（参考）：

```
┌─────────────────────────┐
│ 9月16日 · 减脂第12天      │
│    ╭─────────╮          │
│    │  1380   │ 剩余预算   │
│    │/1800kcal│          │
│    ╰─────────╯          │
│  蛋白 ▓▓▓▓░ 脂肪 ▓▓░░    │
│  ─────────────────────  │
│  今日记录（卡片列表）     │
│                    [+]  │
└─────────────────────────┘
```

（本变更仅定义规范与 token，页面改造列入后续变更。）

## 7. 图标与空态（沿用并微调）

- 沿用 V1.1 §7.5：不用 emoji 当图标、inline SVG（stroke 2px）、`EmptyState.vue` 四型插画
- EmptyState 主 CTA 与描边用色随 token 源自然切换（组件内硬编码色值在后续页面改造变更中一并替换）

## 8. 后台（Element Plus）主题定制

```css
--el-color-primary: #00AC7C;
--el-color-primary-light-3: #33BD96;
--el-color-primary-light-5: #80D6BE;
--el-color-primary-light-7: #B3E7D8;
--el-color-primary-light-8: #CCEFDF;
--el-color-primary-light-9: #F4F8F6;
--el-color-primary-dark-2: #008A63;
--el-color-success: #00AC7C;  --el-color-warning: #FFB020;  --el-color-danger: #FF4747;
```

- 页面背景由「teal/orange 双色 radial 渐变」改为单色微光：`radial-gradient(1200px 480px at 12% -10%, rgba(0,172,124,.08), transparent 55%), #F4F8F6`
- ECharts 图表色板：`[#00AC7C, #FFB020]` 双线起，超标系列用 `#FF4747`

## 9. 动效原则

- 动效只回应人的操作：记录成功→能量环闭合弹一下（300ms 缓动）；按压→缩放 0.97；不做任何页面加载/环境动效
- `prefers-reduced-motion` 下关闭非位移动效（后台侧）

## 10. 不做清单（反模式自审）

- ❌ 奶油底 + 陶土色 + 高对比衬线（AI 默认套路一）
- ❌ 近黑底 + 单一酸性绿/朱红点缀（套路二）
- ❌ 报纸线框零圆角排版（套路三）；统一圆角卡片包 + 同款灰阴影（套路四）
- ❌ ALL-CAPS eyebrow 标签、中点元串、「WORD — fragment」标签、链接尾巴 →（套路五）
- ❌ 大面积 Glassmorphism/backdrop-filter（低端机性能）；❌ 暗色模式（本期无需求）
- ❌ 不改三色判定阈值、不改业务逻辑与接口

## 11. 兼容与回滚

- 改动仅限：1 份设计文档 + 2 份 token 源文件（`uni.scss` / `global.css`）+ 2 处不可引用 SCSS 变量的配置值（`pages.json` 背景色与 tabBar 选中色、`constants.ts` 碳日颜色）
- 变量名保持 `$zhenxinjian-*` / `--zhenxinjian-*` 命名不变，仅换值 → 引用变量的组件自动切换；页面/组件内残留的旧色硬编码（小程序 9 文件、后台 3 文件）列入后续页面改造变更
- 回滚 = `git revert`

# 设计系统 — 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V2.0 |
| 编写日期 | 2026-09-04 |
| 最近更新 | 2026-09-16（V2.0「能量引擎」品牌重铸） |
| 需求基准 | 高保真原型 `MRD-PRD/臻心减小程序V1.1高保真原型/`（16 页 / F01–F27）与 PRD 页面元素表 |

***

## 1. 视觉语言

- **基调**：小白友好、陪伴感、轻量健康风；文案口语化（「帮你算一算」「注意控制哦」）。
- **色语义**（进度状态强约定，PRD §5.6.3）：

| 状态 | 判定 | 颜色 |
| ---- | ---- | ---- |
| 充足 | 目标×80% ≤ 摄入 ≤ 目标 | 🟢 绿色 |
| 不足 | 摄入 < 目标×80% | 🟡 黄色 |
| 超标 | 摄入 > 目标 | 🔴 红色 + 「已超标 XXg」 |

- 微信生态绿色主按钮（登录等品牌动作遵循平台习惯）。
- 数字大字号展示核心结果（总热量卡片「XXXX kcal/天」）。

## 2. 组件体系

- **管理后台**：Element Plus 2.7 + `@element-plus/icons-vue`；表格 / 表单 / 弹窗走 EP 标准件；图表 ECharts 5。
- **用户小程序**：自建轻组件（脚手架 `components/`：CaptchaInput / TeLogo / WsBoard / WsStatusCard 为模式参考）；常用份量快捷按钮（50g/100g/150g/200g/1个/1碗）、卡片式单选（活动水平 5 卡）。
- 通用交互件：底部悬浮「+」按钮、左滑/长按删除、不可关闭强制弹窗（到期授权）。

## 3. 页面清单（16 页原型对照）

P01 游客引导 · P02 首页总览 · P03 身体数据 · P04 代谢结果 · P05 模式选择 · P06 碳循环周期设置 · P07 碳循环计划 · P08 532 月度计划 · P09 体重记录与调碳 · P10 食物库 · P11 添加饮食 · P12 当日记录 · P13 提醒设置 · P14 我的 · P15 到期强制授权 · P16 切换模式确认。

## 4. 无障碍 / 可用性

- 目标用户为「零学习成本」小白：每页有引导文案；空态给行动指引（「今天还没记录饮食哦，点击右下角添加～」）。
- 错误提示就地下标红 + 底部文案，不使用技术术语。

## 5. 主题 / 国际化

- 小程序端 vue-i18n 已预留依赖但一期仅中文；无暗色主题要求（以原型为准）。

## 6. 免责声明（强制 UI 元素）

一切健康计算结果界面（P04 代谢结果等）必须展示免责声明：**仅作生活化减脂参考，非医疗建议**（不可因样式裁剪移除）。

***

## 7. V1.1 增量 — 品牌色板与视觉层级（2026-09-14）

> 背景：双端此前「hero teal + 全局 EP 工厂蓝 #409eff」双色身份割裂、无阴影层级、emoji 充当图标。V1.1 统一为 teal + orange 双品牌色，并建立阴影 / 圆角 / 空态插画规范。落地变更详见 `openspec/changes/2026-09-14-ui-refresh/`。

### 7.1 色板（双端唯一真源）

| 角色 | 色值 | 用途 |
| ---- | ---- | ---- |
| 品牌主色 primary | `#0D9488` | 导航选中、链接、进度、主按钮（非 CTA）、EP `--el-color-primary` |
| primary-light | `#14B8A6` | 渐变副色、hover |
| primary-lighter | `#5EEAD4` | 高亮过渡 |
| primary-bg | `#F0FDFA` | 页面底色、浅底纹 |
| primary-border | `#CCFBF1` | 卡片描边、分割 |
| primary-dark | `#0F766E` | 按压 / 深色态 |
| **CTA 行动橙** | `#F97316` | 主行动按钮（去录入 / 去记录 / FAB）、强调 |
| cta-hover | `#FB923C` | CTA 渐变副色 |
| cta-active | `#EA580C` | CTA 按压态 |
| 充足 success | `#22C55E` | 三色语义（§1） |
| 不足 warning | `#F59E0B` | 三色语义 |
| 超标 danger | `#EF4444` | 三色语义、退出登录等危险动作 |
| 正文 text | `#134E4A` | 标题 / 正文强色 |
| text-regular | `#1F2937` | 正文 |
| text-secondary | `#475569` | 次要说明 |
| text-placeholder | `#94A3B8` | 占位 |

**禁用**：Element Plus 工厂蓝 `#409eff` 一族（`#66b1ff` / `#ECF5FF`）不得再出现在业务代码；双端 `Grep "#409eff"` 应无残留。

### 7.2 渐变

- 品牌渐变（hero / logo / 大数字）：`linear-gradient(160deg, #0D9488, #14B8A6)`
- CTA 渐变（主按钮 / FAB）：`linear-gradient(160deg, #F97316, #FB923C)`

### 7.3 阴影分层（小程序 rpx / 后台 px 同比例）

| 层级 | 值 | 用途 |
| ---- | ---- | ---- |
| card | `0 2rpx 8rpx rgba(13,148,136,.06), 0 8rpx 24rpx rgba(13,148,136,.08)` | 内容卡片 .panel |
| pop | `0 8rpx 32rpx rgba(13,148,136,.16)` | 弹层 / 下拉 |
| fab | `0 8rpx 24rpx rgba(249,115,22,.32)` | 悬浮行动按钮 |
| hero | `0 12rpx 32rpx rgba(13,148,136,.24)` | 顶部渐变头卡 |

### 7.4 圆角与字号层级

- 卡片圆角 16rpx（小程序）/ 12px（后台）；按钮圆角 12rpx，胶囊按钮 32rpx。
- 核心结果大数字：72rpx / 700–800，可用品牌渐变文字（`-webkit-background-clip: text`）强化层级（参考 P04 代谢结果）。
- 标题 30rpx / 600；正文 26–28rpx；辅助说明 22–24rpx。

### 7.5 图标与空态

- **不用 emoji 当 UI 图标**：操作与导航图标一律 inline SVG（stroke 风格、24 viewBox、2px 描边，参考 `pages/mine/index.vue`）。
- 空态统一走 `components/EmptyState.vue`（小程序）：200rpx SVG 插画（bowl / calendar / search / body 四型）+ 标题 + 描述 + 橙色渐变主 CTA。
- 后台空态用 `el-empty` 并配文案 slot。

### 7.6 后台（Element Plus）主题定制

`apps/zhenxinjian-front/src/styles/global.css` 注入 `--el-color-primary` 一族（light-3/5/7/8/9 + dark-2）覆盖 EP 默认蓝；success / warning / danger 同步 §7.1 三色。ECharts 图表色板 `[#0D9488, #F97316]` 双线起。

***

## 8. V2.0 增量 — 能量引擎（2026-09-16）

> 背景：V1.1 的 teal + orange 均来自 Tailwind 现成色板，品牌辨识度低，且品牌色与三色语义互不认账。V2.0 朝「健康、活泼、科技感、新颖」重铸为「能量引擎」品牌语言：减脂的本质是能量收支，一道绿→琥珀的能量渐变同时讲述品牌与状态。落地变更详见 `openspec/changes/2026-09-16-design-system-v2/`。**本节为当前真源，§7 值已过时，仅作历史记录。**

### 8.1 色板（旧→新映射）

| Token | V1.1 | V2.0 | 角色 |
| ---- | ---- | ---- | ---- |
| primary / leaf | `#0D9488` | `#00AC7C` | 品牌主色 叶绿 = 充足态 |
| primary-light | `#14B8A6` | `#33BD96` | hover、渐变辅助 |
| primary-lighter | `#5EEAD4` | `#80D6BE` | 高亮过渡 |
| primary-bg / bg | `#F0FDFA` | `#F4F8F6` | 页面底色 雾面白（冷调微绿） |
| primary-border / border | `#CCFBF1` | `#E3EFE9` | 卡片描边、分割 |
| primary-dark | `#0F766E` | `#008A63` | 按压/深色态 |
| cta / amber | `#F97316` | `#FFB020` | 琥珀光 = 不足态 |
| cta-hover | `#FB923C` | `#FFC24D` | CTA hover |
| cta-active | `#EA580C` | `#E69A00` | CTA 按压态 |
| success | `#22C55E` | `#00AC7C` | 充足 = 品牌叶绿 |
| warning | `#F59E0B` | `#FFB020` | 不足 = 琥珀 |
| danger / flare | `#EF4444` | `#FF4747` | 超标/危险 警示红 |
| text / ink | `#134E4A` | `#10312B` | 墨绿黑（带绿调的彩色墨） |

`#00AC7C / #FFB020 / #FF4747 / #10312B / #F4F8F6` 均刻意避开 Tailwind 现成阶。

### 8.2 能量渐变 = 品牌渐变 = 语义渐变

- `linear-gradient(135deg, #00AC7C, #FFB020)`：hero 头卡、能量环、品牌大数字、FAB、CTA 渐变
- 绿端 = 能量充足，琥珀端 = 尚有缺口；超标不用渐变，独立 `#FF4747` + 文案「已超标 XXg」

### 8.3 三色语义（并入品牌，阈值不变）

| 状态 | 判定（PRD §5.6.3，不变） | 颜色 |
| ---- | ---- | ---- |
| 充足 | 目标×80% ≤ 摄入 ≤ 目标 | 🟢 `#00AC7C` |
| 不足 | 摄入 < 目标×80% | 🟡 `#FFB020` |
| 超标 | 摄入 > 目标 | 🔴 `#FF4747` |

### 8.4 阴影分层（替换 §7.3）

| 层级 | 值 | 用途 |
| ---- | ---- | ---- |
| card | `0 2rpx 8rpx rgba(0,172,124,.05), 0 8rpx 24rpx rgba(16,49,43,.06)` | 内容卡片 |
| pop | `0 8rpx 32rpx rgba(16,49,43,.14)` | 弹层/下拉 |
| fab | `0 8rpx 24rpx rgba(255,176,32,.35)` | 悬浮行动按钮 |
| hero | `0 12rpx 32rpx rgba(0,172,124,.22)` | 顶部能量头卡 |

原则：卡片层级优先靠「白卡面 + 描边 + mist 底色」的明度差表达，阴影只做轻提示，不统一铺灰阴影。

### 8.5 字体

- 数字/展示：`"Space Grotesk", "DIN Alternate", -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`；后台以本地/打包字体文件提供（禁止运行时 CDN 拉取），小程序声明同名 font-family 自然降级
- 中文/正文沿用系统黑体栈（§7 不变），不引入第二中文家族
- 核心结果大数字：72rpx（小程序）/ 40px（后台）、字重 700–800、`font-variant-numeric: tabular-figures`，可叠加能量渐变文字

### 8.6 能量环（新视觉语言，规范定义）

- 结构：conic-gradient 环形仪表（进度弧走能量渐变）+ 中央挖空 + 环内大数字（摄入/目标）
- 未走完的缺口段露出琥珀 → 「缺口即预算」的直觉表达；描边 16–20rpx，首页环直径约 360rpx
- 超标态：环满切 `#FF4747` 单色 + 超标文案
- 本次仅定义规范，页面级落地列入后续变更

### 8.7 动效原则

- 动效只回应人的操作：记录成功→能量环闭合反馈（300ms 缓动）；按压→缩放 0.97；不做页面加载/环境动效
- 后台侧尊重 `prefers-reduced-motion`

### 8.8 反模式清单（自审通过项）

- ❌ 奶油底+陶土色+高对比衬线 / 近黑底+单一酸性色 / 报纸线框零圆角 / 统一圆角卡片包+同款灰阴影
- ❌ ALL-CAPS eyebrow、中点元串、「WORD — fragment」标签、链接尾巴 →
- ❌ 大面积 Glassmorphism / backdrop-filter（低端机）；❌ 暗色模式（本期无需求）
- ❌ 不改三色判定阈值、不改业务逻辑与接口

### 8.9 后台（Element Plus）主题定制（替换 §7.6）

`global.css` 注入 `--el-color-primary: #00AC7C` 一族（light-3 `#33BD96` / light-5 `#80D6BE` / light-7 `#B3E7D8` / light-8 `#CCEFDF` / light-9 `#F4F8F6` / dark-2 `#008A63`）；success / warning / danger 同步 §8.3。body 背景改为单色微光：`radial-gradient(1200px 480px at 12% -10%, rgba(0,172,124,.08), transparent 55%)` + `#F4F8F6`。ECharts 色板 `[#00AC7C, #FFB020]` 双线，超标系列 `#FF4747`。

### 8.10 兼容说明

- 变量名保持 `$zhenxinjian-*` / `--zhenxinjian-*` 不变，仅换值 → 引用变量的组件自动切换
- 页面/组件内旧色硬编码已于 `2026-09-16-page-color-migration` 全量清除（双端 `Grep` V1.1 色值零残留）；能量环组件实现、Space Grotesk 字体文件仍列入后续变更
- 回滚 = `git revert`

## 9. V2.1 增量 — 令牌补全与图标线性化（2026-09-17）

> 背景：V2.0 只换了主色板，页面仍残留 EP 时代的语义浅底（`#fef0f0/#fdf6ec/#fff7f7`）、裸圆角数值（8/12/16/20rpx）与 emoji 充当图标。V2.1 补齐语义浅底 / 圆角 / 间距 / 字体令牌，并以统一线性 SVG 图标库替换全部 emoji。落地见本次「能量引擎双端 token 与页面迁移」。

### 9.1 语义浅底 / 中性令牌（`uni.scss`）

| Token | 值 | 用途 |
| ---- | ---- | ---- |
| success-bg / -border | `#e7f6f0` / `#b7e4d5` | 充足浅底/描边 |
| warning-bg / -border / -deep | `#fff5dd` / `#fce2ae` / `#9a6b00` | 不足浅底/描边/深字（白底对比 ≥4.5:1） |
| danger-bg / -border / -deep | `#ffefef` / `#fbcaca` / `#d63333` | 超标浅底/描边/深字（白底对比 ≥4.5:1） |
| track / divider | `#edf1ef` | 进度槽 / 分割线（冷调微绿，替换 `#f0f0f0`） |

### 9.2 圆角 / 间距 / 字体档位

- 圆角：`radius-sm 8rpx`（标签）· `radius-md 12rpx`（按钮/输入/小控件）· `radius-lg 16rpx`（卡片/面板）· `radius-xl 20rpx`（hero/彩砖）· `radius-pill 999rpx`（胶囊 chip、半高圆按钮、进度槽、搜索框、FAB）
- 间距：`sp-1 8` · `sp-2 16` · `sp-3 24` · `sp-4 32` · `sp-5 40` · `sp-6 48` · `sp-8 64`（rpx）
- 数字字体：`$zhenxinjian-font-num`（Space Grotesk 栈，字体文件落地前自然降级）
- 规则：`<style>` 内禁止裸写 8/12/16/20rpx 圆角与品牌色 hex，一律引用令牌；唯一例外是品牌 App 图标（guide 页 160rpx logo 的 40rpx 大圆角，超出卡片圆角档位，刻意保留）

### 9.3 线性图标库（`src/utils/icons.ts`）

- `iconSrc(name, color, sw)`：统一 24 viewBox / round 线帽 SVG，经 `encodeURIComponent` 编为 data-uri，由 `<image>` 渲染；mp-weixin 不支持内联 `<svg>`，data-uri 方案 H5/小程序双端兼容，带缓存
- `ringSrc(percent, color)`：三色环形进度 data-uri（0–100 封顶）
- 全量替换页面/组件中的 emoji 与 HTML 实体字形（📸🥗›&#9200; 等）；空态插画仍由 `EmptyState.vue` 内置多色 SVG 承担
- SCSS 变量在 `<script>` 不可用，故 JS 侧图标着色直接写 V2 色值：品牌 `#00AC7C`、白底 `#ffffff`、深警示 `#d63333`、深琥珀 `#9a6b00`、次要灰 `#8a8f99/#94A3B8/#475569`；`<switch color="#00AC7C">` 等原生属性同理（取值与令牌严格一致）

***

> **文档版本**：V2.1　**最后更新**：2026-09-17　**维护**：臻心减项目组

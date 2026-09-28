# 提案 — 设计系统 V2.0 P0 收尾：能量环组件 + 数字字体落地

## Why

设计系统 V2.0（2026-09-16）重铸了「能量引擎」品牌语言，但两项核心视觉资产至今停留在「规范已定义、实现未落地」状态（见 `docs/knowledge/design-system/design-system.md` §8.10 挂账）：

1. **能量环**：§8.6 已定义「conic-gradient 环形仪表 + 缺口露琥珀 = 缺口即预算」的品牌最强视觉语言，但首页热量头卡目前用的是 `utils/icons.ts` 里的 `ringSrc` 简易单色环形（`pages/home/index.vue:235`），品牌辨识度与「缺口即预算」的直觉表达均未兑现。
2. **Space Grotesk 数字字体**：§8.5 已定义数字/展示字体栈，后台 `global.css` 与小程序 `uni.scss` 均已声明 font-family，但**字体文件从未落地**，双端全部依赖降级栈（DIN Alternate / 系统黑体）。核心结果大数字（首页 64rpx/800、后台 40px 数字）是本产品最重要的视觉层级，长期降级直接削弱品牌感。

两项欠账不还会导致：品牌规范与实现长期脱节、后续页面（能量环计划推广到记录页/计划页）无组件可复用、数字层级在不同设备上表现不可控。

## What Changes

- **新增小程序组件 `EnergyRing.vue`**（`apps/zhenxinjian-uniapp/src/components/`）：按 §8.6 规范实现能量环——进度弧走能量渐变（绿→琥珀）、未走完缺口段露琥珀底、超标态环满切 `#FF4747` 单色、中央挖空承载大数字；直径/描边可配置（默认对齐首页 260rpx 环、16–20rpx 描边档位）；提供「记录成功 → 环闭合」300ms 缓动反馈（§8.7 动效只回应操作）。
- **首页头卡接入**：`pages/home/index.vue` 热量环从 `ringSrc` data-uri `<image>` 替换为 `EnergyRing` 组件；三色判定阈值（80%/100%）与数据口径不变。
- **下线 `ringSrc`**：`utils/icons.ts` 中 `ringSrc` 函数迁入组件实现并从导出中移除（当前仅首页一处引用）。
- **Space Grotesk 字体文件落地（后台）**：将 Space Grotesk（woff2， latin 子集， 400/500/700 三字重）以本地文件方式引入 `apps/zhenxinjian-front`（`src/assets/fonts/` + `@font-face` 声明于 `global.css`），由 Vite 打包产出，**禁止运行时 CDN 拉取**（§8.5 红线）。
- **小程序端字体策略确认**：mp-weixin 不引字体文件（包体 +2MB 风险、动态加载网络字体需真实域名与审核），继续声明同名 font-family 自然降级——本次仅在后台落地文件，小程序端维持现状并在设计系统文档更新说明。
- **文档收口**：`docs/knowledge/design-system/design-system.md` §8.10「兼容说明」中两项挂账改为已落地。

不改变：三色判定阈值、任何业务逻辑与接口、Element Plus 主题定制、暗色模式（无需求）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无——本变更为纯视觉资产落地：三色语义阈值、数据口径、页面信息架构与交互均不变，无 spec 级行为变化。沿用 2026-09-14-ui-refresh / 2026-09-16-design-system-v2 先例，`.openspec.yaml` 已设 `skip_specs: true`。）

## Impact

- **小程序**（`apps/zhenxinjian-uniapp`）：
  - 新增 `src/components/EnergyRing.vue`（含环形 SVG 生成逻辑，data-uri 方案沿用 icons.ts 先例以保证 H5/mp-weixin 双端兼容）
  - `src/pages/home/index.vue`：头卡环形替换 + 记录成功闭合动效接入
  - `src/utils/icons.ts`：移除 `ringSrc` 导出
- **管理后台**（`apps/zhenxinjian-front`）：
  - 新增 `src/assets/fonts/`（Space Grotesk woff2 × 3 字重，约 90KB）
  - `src/styles/global.css`：新增 `@font-face` 三段声明（`font-display: swap`）
- **文档**：`docs/knowledge/design-system/design-system.md` §8.10 更新
- **不受影响**：后端、API、数据库、Element Plus 组件主题、ECharts 色板

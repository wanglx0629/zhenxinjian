# 设计 — 能量环组件 + Space Grotesk 字体落地

## Context

见 proposal.md（Why/What）。实现层面的现状约束：

- mp-weixin 不支持内联 `<svg>` 节点，也不支持 WXSS `conic-gradient`（基础库版本相关）；本代码库既有跨端矢量方案是 **SVG data-uri 经 `<image>` 渲染**（`utils/icons.ts` 的 `iconSrc` / `ringSrc` 先例，带缓存）。
- 首页头卡现状（`pages/home/index.vue:233-245`）：260rpx `ring-box` + `ringSrc` 单色环 + 中央绝对定位文字层；kcalRate 由 store 计算，三色阈值 80%/100% 在 `kcalBarColor` computed 中。
- 后台 `global.css` 已声明 `--zhenxinjian-font-num: 'Space Grotesk', ...` 但无 `@font-face`，全部降级；`.num` 工具类已在使用。
- 设计系统 §8.6 规范要点：进度弧走能量渐变、缺口段露琥珀（「缺口即预算」）、超标态环满切 `#FF4747`、描边 16–20rpx、首页环直径约 360rpx、中央挖空大数字。

## Goals / Non-Goals

**Goals:**

- `EnergyRing.vue` 双端（H5/mp-weixin）可复用，忠实实现 §8.6 全部视觉语义
- 首页头卡替换为组件，落地「记录成功 → 环闭合」300ms 反馈（§8.7）
- 后台本地打包 Space Grotesk（woff2、无 CDN 运行时请求），数字层级跨设备一致
- 设计系统文档 §8.10 两项挂账收口

**Non-Goals:**

- 不改三色阈值、kcalRate 口径、store 与接口
- 不在 mp-weixin 端引入字体文件（见 D4）
- 不把能量环推广到记录页/计划页等（后续变更）
- 不做暗色模式、不做环境动效

## Decisions

### D1 能量环渲染：SVG data-uri（沿用 icons.ts 模式），否决 conic-gradient / canvas

- **选择**：组件内部生成 SVG data-uri，`<image>` 渲染。结构：底层 track 圆（琥珀缺口）+ 上层进度弧（`linearGradient` 描边）。
- **理由**：与 `iconSrc` 同构，H5/mp-weixin 行为一致；conic-gradient 在低版本基础库/安卓 WebView 支持不可控；canvas 对静态环 + 轻动效过重。
- **备选否决**：
  - *CSS conic-gradient + mask*：H5 可行，mp-weixin 兼容性赌基础库版本，否。
  - *canvas*：维护成本高、DPR 适配繁琐，否。

### D2 渐变弧的实现：空间固定的 linearGradient（userSpaceOnUse），非「沿路径渐变」

- SVG 无原生沿路径渐变。采用**固定于圆坐标系的对角 linearGradient**（起点≈12点方向取绿 `#00AC7C`，终点≈对角方向取琥珀 `#FFB020`），进度弧按 `stroke-dasharray/dashoffset` 在该渐变空间中生长。
- 语义自洽：0% 时全环为琥珀缺口（全部预算）；随摄入增长，绿→琥珀弧沿环生长，缺口收窄；100% 时满环渐变、无缺口——即 §8.6「缺口即预算」的直觉表达。
- **超标态**（percent > 100）：弧满且整环切 `#FF4747` 单色（不走渐变），叠加既有「已超标 XXkcal」文案。
- **缺口色**：track 取 `#FFB020` 半透明（hero 渐变底上保证可读，透明度实现期微调；若琥珀端对比不足降级为 `#E69A00`）。linecap: round，起止端圆头。

### D3 组件 API 与闭合动效

- **Props**：`percent: number`（0–100+，>100 自动超标态）、`size?: number`（rpx，默认 360，§8.6）、`stroke?: number`（rpx，默认 20，档位 16–20）、`trackColor?`/`overColor?`（可选覆盖）。
- **默认插槽**：中央挖空内容（首页传 已摄入大数字/目标 kcal 文案层）。
- **闭合动效**：组件内 watch `percent` 上升时，从当前显示值到新值做 300ms ease-out 补间（~10 步 JS 定时重生成 data-uri，encodeURIComponent 开销可忽略，沿用 Map 缓存）；下降/首次渲染直接跳变（不做加载动效，§8.7「动效只回应操作」——补间仅在用户记录回流 store 时触发）。
- **迁移**：首页 `ringSrc` 引用替换为 `<EnergyRing :percent="summary?.kcalRate ?? 0">` + 中央插槽；`icons.ts` 删除 `ringSrc` 及其缓存（当前全库仅首页一处引用，已核实）。hero 布局随 360rpx 环径微调（remain 胶囊、模式标签间距）。

### D4 字体：后台 Variable Font 单文件，mp-weixin 维持降级

- **后台**：引入 Space Grotesk **可变字体**（`SpaceGrotesk[wght].woff2`，latin 子集，约 35KB，覆盖 wght 300–700）至 `src/assets/fonts/`，`global.css` 声明 `@font-face`（`font-weight: 300 700`、`font-display: swap`、`src: url()` 相对路径由 Vite 打包哈希产出，**零运行时 CDN**）。单文件替代 3 个静态字重：体积更小、字重全覆盖。
  - 来源：google/fonts 仓库（OFL 1.1 协议，可自由嵌入与再分发）；**fonts 目录内随附 OFL.txt 许可副本**（协议要求）。
  - 字重 800 的使用点（§8.5「700–800」）由可变字体上限 700 承接（浏览器就近取 700，不合成伪粗）。
- **mp-weixin 不落地字体文件**：本地字体文件在小程序内仅能 base64 内嵌主包或 `wx.loadFontFace` 网络加载（需真实域名白名单 + 审核）；收益（数字字形）不抵主包体积与审核成本。维持声明同名 font-family 自然降级（iOS DIN Alternate / 安卓系统体），此决策回写设计系统文档。

### D5 文档收口口径

`design-system.md` §8.10：能量环「组件实现」与「Space Grotesk 字体文件」两项挂账改为已落地（注明 mp-weixin 字体维持降级的决策）；§8.6 增加「已由 `EnergyRing.vue` 落地」标注。不改 §8.1–8.9 任何值。

## Risks / Trade-offs

- [渐变弧是空间近似而非严格沿路径] → 视觉语义已满足「绿起→琥珀收」；若评审不认可，备选方案为按 percent 分段离散着色（实现期可切换，不影响 API）。
- [缺口琥珀在 hero 琥珀端可能对比不足] → 透明度/深琥珀 `#E69A00` 微调留为实现期参数（`trackColor` prop 可覆盖）。
- [360rpx 环径使 hero 变高，小屏挤压下方内容] → rpx 天然随屏缩放；若 660px 低档机型实测过高，size prop 可下调，组件不做硬编码。
- [JS 补间重生成 data-uri 在低端机卡顿] → 仅 ~10 帧、300ms 一次性触发，且 Map 缓存避免重复编码；极端情况补间退化为直接跳变（功能无损）。
- [字体文件引入使后台首屏多一次字体请求] → `font-display: swap` 文案先渲染、字体就绪后替换，不阻塞；35KB woff2 在 Vite 产出下 gzip 后更小。
- [OFL 协议合规] → 随附 OFL.txt 于 fonts 目录，满足再分发要求。

## Migration Plan

1. 先落字体（独立、零风险）→ 再落组件与首页接入 → 最后文档收口。
2. 回滚：整包 `git revert`；组件与 ringSrc 删除是同 commit 内原子替换，无中间态。

## Open Questions

（无——缺口色透明度等均为实现期参数，已由 prop 预留覆盖能力。）

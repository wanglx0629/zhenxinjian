# 设计系统 V2.0 P0 收尾 — Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消（注明原因）
> 前置阅读：本 change `design.md`（D1–D5 决策）与 `docs/knowledge/design-system/design-system.md` §8.5–§8.7

## 后台字体落地

- [x] **T-01** `apps/zhenxinjian-front/src/assets/fonts/`：自 google/fonts 仓库（`ofl/spacegrotesk`）获取 `SpaceGrotesk[wght].woff2`（可变字体，wght 300–700，latin 子集）与 `OFL.txt` 许可副本存入该目录
- [x] **T-02** `apps/zhenxinjian-front/src/styles/global.css`：新增 `@font-face` 声明——`font-family: 'Space Grotesk'`、`src: url('../assets/fonts/SpaceGrotesk[wght].woff2') format('woff2')`（相对路径交 Vite 打包）、`font-weight: 300 700`、`font-display: swap`；注释标注「本地打包、禁止 CDN 拉取」与作者注释
- [x] **T-03** 字体生效验证：`npm run build`（front）产出中含打包后的 SpaceGrotesk 字体资源文件，且 `dist` 内 `Grep "fonts.googleapis|fonts.gstatic"` 无任何外部字体 URL；`.num` 元素 DevTools Computed 面板命中 Space Grotesk

## 能量环组件（小程序）

- [x] **T-04** 新增 `apps/zhenxinjian-uniapp/src/components/EnergyRing.vue`：按 design D1/D2 实现——SVG data-uri（`<image>` 渲染）双端兼容；底层 track 圆 `#FFB020` 半透明（琥珀缺口）+ 上层进度弧 `linearGradient`（userSpaceOnUse 对角，`#00AC7C` → `#FFB020`）、round linecap、`stroke-dasharray/dashoffset` 控制弧长；`percent > 100` 时弧满并整环切 `#FF4747` 单色满环（超标态，不走渐变）；Map 缓存 data-uri；props：`percent`（必填，0–100+）、`size`（rpx，默认 360）、`stroke`（rpx，默认 20）、`trackColor?`/`overColor?`（可选覆盖）；默认插槽承载中央挖空内容；令牌色值与 `uni.scss` 严格一致（JS 侧直接写 hex，先例 `icons.ts` §9.3）
- [x] **T-05** `EnergyRing.vue` 闭合动效（design D3）：watch `percent` 上升时从当前显示值做 300ms ease-out 补间（约 10 步定时重生成 data-uri）；首次渲染与下降直接跳变；组件卸载清理定时器

## 首页接入

- [x] **T-06** `apps/zhenxinjian-uniapp/src/pages/home/index.vue`：头卡 `ringSrc` `<image>` 替换为 `<EnergyRing :percent="summary?.kcalRate ?? 0">`，中央插槽迁移现有文字层（已摄入大数字 / 目标 kcal，字号令牌不变）；hero 布局按 360rpx 环径微调（`ring-box` 尺寸、remain 胶囊与模式标签间距、骨架屏 `skeleton-hero` 高度）；`kcalBarColor` computed 移除（三色职责由能量环语言承接：弧长=摄入、琥珀缺口=预算、红满环=超标；`hero-remain.over` 红胶囊与「已超标」文案保留不动）
- [x] **T-07** `apps/zhenxinjian-uniapp/src/utils/icons.ts`：删除 `ringSrc`、`ringCache` 及导出（全库唯一引用已随 T-06 移除）；`Grep "ringSrc"` 双端零残留

## 文档回写

- [x] **D-01** `docs/knowledge/design-system/design-system.md`：§8.6 标注「已由 `components/EnergyRing.vue` 落地」；§8.10 兼容说明中「能量环组件实现、Space Grotesk 字体文件仍列入后续变更」改为已落地，并补记 mp-weixin 字体维持降级的决策（design D4）；更新文首「最近更新」与文末版本行

## 验收

- [x] **V-A1** 双端编译通过：uniapp `npm run dev:mp-weixin` 与 `npm run dev:h5` 编译无报错；front `npm run build` 成功
- [x] **V-A2** H5 截图走查能量环三态：不足（如 52%，弧短缺口大）/ 充足（如 90%，弧长缺口窄）/ 超标（>100%，红色满环 + 「已超标 XX kcal」胶囊）；中央数字层对齐无遮挡、hero 不溢出（像素级验证：52% 弧 54%/缺口 46%、90% 弧 92%/缺口 8%、130% 红满环 100%）
- [x] **V-A3** `Grep "ringSrc"` 于 `apps/zhenxinjian-uniapp/src` 无残留；`Grep "fonts.googleapis|fonts.gstatic"` 于 `apps/zhenxinjian-front/dist` 无残留
- [x] **V-A4** `openspec validate design-system-v2-p0` 通过；`openspec status` 显示全部产物就绪（specs 按 skip_specs 跳过）

## 不做（超范围，列入后续变更）

- 能量环推广至记录页 / 计划页 / 后台 dashboard → 后续变更
- mp-weixin 端字体文件 / wx.loadFontFace → 维持降级（design D4 决策）
- `.num` 800 字重使用点排查改 700 → 可变字体上限 700 自动承接，无需改动

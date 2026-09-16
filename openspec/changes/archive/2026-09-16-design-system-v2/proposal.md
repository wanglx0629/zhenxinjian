# 设计系统 V2.0「能量引擎」— Proposal

## Why

V1.1 的「teal + orange」双色虽统一了双端，但两个主色均来自 Tailwind 现成色板，品牌辨识度低，且品牌色与三色语义（充足/不足/超标）互不认账——同一屏幕出现两套绿。用户要求 V2.0 朝「健康、活泼、科技感、新颖」迭代，并允许突破现有规范。经方向评审，选定方案 A「能量引擎」：把减脂的本质「能量收支」直接铸成品牌语言。

## What Changes

- **BREAKING（token 层）**：品牌主色 `#0D9488`(teal) → `#00AC7C`(叶绿)；CTA `#F97316`(orange) → `#FFB020`(琥珀光)；正文墨色 `#134E4A` → `#10312B`(墨绿黑)；页面底色 `#F0FDFA` → `#F4F8F6`(雾面白)
- **品牌渐变 = 语义渐变**：能量渐变 `135deg, #00AC7C → #FFB020` 同时承载品牌身份与「能量充足→不足」的状态语义；超标仍为独立警示红 `#FF4747`
- **三色语义并入品牌**：充足=叶绿（品牌色）、不足=琥珀、超标=警示红，三色阈值判定（PRD §5.6.3）不变
- **新增字体规范**：数字优先「Space Grotesk」（几何、科技感），中文沿用系统黑体；核心大数字使用 tabular figures
- **新增组件规范**：能量环（conic-gradient 环形仪表）作为首页/结果页核心视觉语言；卡片层级靠底色+描边而非统一灰阴影
- **动效原则**：动效只回应用户操作（记录成功时能量环闭合反馈），不做环境动效

## Capabilities

本次为纯视觉 token 与设计文档迭代，不改变任何接口、数据模型、业务逻辑或页面行为；经核查 `openspec/specs/` 中无任何色值/样式类需求，故无 spec 级行为变更。

### New Capabilities

- 无

### Modified Capabilities

- 无（已在 `.openspec.yaml` 设置 `skip_specs: true`）

## Impact

- `docs/knowledge/design-system/design-system.md`：回写 V2.0 增量章节（保留 V1.0/V1.1 历史）
- `apps/zhenxinjian-uniapp/src/uni.scss`：token 源升级为能量引擎色板
- `apps/zhenxinjian-front/src/styles/global.css`：token 源 + Element Plus 主题变量同步升级
- 不改动：后端、接口、数据库、页面 .vue/.ts 业务代码（页面级旧色残留的替换列入后续变更，不在本次范围）

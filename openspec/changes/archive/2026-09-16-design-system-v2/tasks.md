# 设计系统 V2.0「能量引擎」— Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消（注明原因）

## Token 源升级

- [x] **T-01** `apps/zhenxinjian-uniapp/src/uni.scss`：`$zhenxinjian-*` 变量换值为能量引擎色板（primary #00AC7C / cta #FFB020 / text #10312B / bg #F4F8F6 / border #E3EFE9 / danger #FF4747 / success #00AC7C / warning #FFB020），渐变与阴影 token 按 design §2/§5 更新，文件头注释标注 V2.0
- [x] **T-02** `apps/zhenxinjian-uniapp/src/pages.json`：`backgroundColor` `#F0FDFA` → `#F4F8F6`；tabBar `selectedColor` `#0D9488` → `#00AC7C`（pages.json 不能引 SCSS 变量，硬编码必须与 uni.scss 同源）
- [x] **T-03** `apps/zhenxinjian-uniapp/src/config/constants.ts`：`CYCLE_DAY_TYPES` 中碳日 `#0D9488`/`#CCFBF1` → `#00AC7C`/`#E3EFE9`（TS 常量不能引 SCSS 变量）
- [x] **T-04** `apps/zhenxinjian-front/src/styles/global.css`：`--zhenxinjian-*` 与 `--el-color-primary` 一族换值（EP light 阶按 design §8 重算），三色语义、阴影、渐变、body 背景按 design §1/§2/§5/§8 更新，文件头注释标注 V2.0

## 文档回写

- [x] **D-01** `docs/knowledge/design-system/design-system.md` 新增「§8. V2.0 增量 — 能量引擎」章节：色板映射表（旧→新）、能量渐变=语义、字体（Space Grotesk 数字）、能量环规范、动效原则；保留 V1.0/V1.1 全部历史内容；更新文首版本号/最近更新日期
- [x] **D-02** `AGENTS.md` 无需修改（入口只链接 design-system，不含色值）——核验后勾选

## 验收

- [x] **V-A1** `Grep "#0D9488|#F97316|#14B8A6"` 在 `uni.scss` / `global.css` / `pages.json` / `constants.ts` 四份本次范围文件内无残留（同时核验 V1.1 全部旧色值均无残留）
- [x] **V-A2** 双端 token 交叉核对：uni.scss 与 global.css 的同名语义色值一致；design.md §1 表格与两个源文件一致
- [x] **V-A3** `openspec validate 2026-09-16-design-system-v2` 通过（CLI 实际参数为位置参数）；`openspec status` 显示全部产物就绪（specs 按 skip_specs 跳过，3/3 完成）

## 不做（超范围，列入后续变更）

- 页面/组件内旧色硬编码替换（小程序 9 文件含 EmptyState 等、后台 3 文件含 dashboard/TeLogo）→ 后续「页面改造」变更
- 能量环组件实现、Space Grotesk 字体文件引入、dev server 截图走查 → 后续变更

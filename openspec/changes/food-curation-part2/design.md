# 食物库共建审核 + PART2 交互 — Design

## 1. 现状与问题

- `foods` 单表用 `source(1内置/2自定义)` + `user_id` 隔离。自定义食物（`CustomFoodService`）创建即生效、仅本人可见，**无审核态、无千焦/单位/图片字段、无操作留痕**；管理员后台（`AdminFoodController`/`view/foods`）对自定义食物只读，没有审核动作。
- `AiChatClient`/`ZhipuAiChatClient` 当前只支持「图片多模态」（OCR 拍照识别），不能直接做纯文本营养合理性校验。
- 小程序 PART2 的若干交互与最新原型不符（首页记饮食绕一层、食物库无取消、532 无生成入口、碳循环终止交互旧、切换弹窗无周期明细）。
- 基础食物仅 200 条，覆盖不足；无合法的扩充管线。

## 2. 范围与非目标

**范围**：A 小程序 PART2 交互；C 共建审核子系统（三端）；D TFDA 开放数据导入。

**非目标（B 批或更后）**：P01 协议勾选、P02 经期文案/建档拦截、P03 年龄 14-60、P12 记录具体时间字段、P13 三餐时间窗与超标推送；碳循环 BMR/TDEE 5 步计算口径重算；OFF/USDA 数据源；食物实拍图抓取；共建通过后的「作者再编辑」。

## 3. 数据模型（SQL：`sql/change14_food_curation.sql`，含 schema_migrations 补账 version=14）

### 3.1 `foods` 加列（均给默认值，存量数据平滑迁移）

| 列 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `audit_status` | TINYINT | 3 | 0待审核 1已通过 2已驳回 3无需审核；存量内置=3，存量自定义回填 1（见下迁移） |
| `kj` | INT | NULL | 能量千焦 kJ/100g；与 kcal 二者以 kcal 为入库基准，kj 用于展示/录入联动 |
| `unit` | VARCHAR(16) | NULL | 填报单位（份/个/杯/包…），可空；营养口径仍统一每 100g |
| `image` | VARCHAR(512) | NULL | 共建食物用户上传图相对路径；内置仍走 `food_images`，VO 输出时 foods.image 优先 |
| `audit_remark` | VARCHAR(255) | NULL | 最近一次驳回原因 |
| `ai_verdict` | VARCHAR(16) | NULL | pass/suspect/reject/none（none=AI 未校验/降级） |
| `ai_suggestion` | VARCHAR(500) | NULL | AI 建议（含建议修正值的短文本/JSON） |
| `submit_time` | DATETIME | NULL | 最近一次提交/重提时间 |
| `data_batch` | VARCHAR(32) | NULL | 数据批次（TFDA 导入填 `TFDA:<版本>`；内置 200/共建为空） |

- 迁移回填：存量 200 内置 `audit_status=3`；**存量自定义食物**（source=2）回填 `audit_status=1`（视同历史已上架，避免老用户既有食物突然对自己只读/不可见），`ai_verdict='none'`。
- 索引：`idx_source_audit(source, audit_status, delete_flag)`（后台待审队列）、`idx_user_audit(user_id, audit_status)`（我的列表）。不加会与软删冲突的普通唯一键；名称归属唯一仍沿用现有生成列唯一索引口径。
- 遵守建表/加列规约：状态列配套字典枚举。

### 3.2 新增 `food_audit_log`（只追加流水）

| 列 | 说明 |
| --- | --- |
| `id` BIGINT PK AI | |
| `food_id` BIGINT | 关联 foods.id（不物理删，留痕） |
| `action` VARCHAR(16) | SUBMIT/AI_CHECK/APPROVE/REJECT/RESUBMIT/ADMIN_FIX |
| `operator_id` BIGINT | 操作人（用户或管理员；AI_CHECK 为系统 0） |
| `ai_verdict` / `ai_suggestion` | AI 动作快照（可空） |
| `remark` VARCHAR(255) | 驳回原因等 |
| `snapshot_before` / `snapshot_after` | JSON 关键字段前后值（名称/分类/三宏/kcal/kj/unit/image） |
| `create_time` | |
| `delete_flag` | 逻辑删除列（规约必备；流水业务上永不删除），`status` 列如规约要求则补常量 1 |

### 3.3 字典枚举

新增 `common/enums/FoodAuditStatusEnum`（code+desc+of：0待审核/1已通过/2已驳回/3无需审核）。`FoodSourceEnum` 维持 1基础/2共建；后台文案将「内置」统一表述为「基础食物」、「自定义」表述为「用户共建」。

## 4. 审核状态机与可见性

```
新建/重提 ──▶ 0 待审核（私有，本人立即可用；同步 AI 打标，失败置 none）
   ├─ 管理员 APPROVE ─▶ 1 已通过（公共，全员可见，原作者只读）
   ├─ 管理员 REJECT(必填原因) ─▶ 2 已驳回（私有，本人可用，可改）
   └─ 管理员 ADMIN_FIX ─▶ 1（按修正值落库）
2 编辑保存 ──▶ RESUBMIT → 0（重跑 AI，清空当前 audit_remark；历史在流水）
1 原作者编辑/删除 ──▶ 拒绝；仅管理员可维护（修正/停用/软删）
基础/TFDA：恒 3，不进审核动作
```

**可见性收敛（统一改食物公共查询的 where）**：
- 公共搜索/分类/热门/详情：`(source=1 AND status=1) OR (source=2 AND audit_status=1 AND status=1)`，并 `delete_flag=0`；
- 「我的食物」：`source=2 AND user_id=me`（0/1/2 全返回，带状态）；
- 添加饮食记录时的食物可达性：本人可用 = 公共集 ∪ 本人 source=2（0/1/2）。
- foodId 全程不变，私有期间产生的 `diet_records` 引用在转公共后零迁移。

## 5. AI 文本营养校验

- `AiChatClient` 扩展为同时支持文本：新增 `chatText(model, systemPrompt, userText, timeout)` 默认方法（图片列表传空）；`ZhipuAiChatClient` 在 images 为空时构造纯文本 `ChatMessage`（不再放 image_url block）。复用现有 OCR api-key 与懒构建/错误分类，新增配置键 `ai.food-audit-model`（文本模型名，缺省给一个可用文本模型，可后台改）。
- 新增 `FoodAuditAiService`（或在 `CustomFoodService` 内组合）：构造含名称/分类/kj/kcal/三宏的 prompt，约束只回 JSON `{verdict, suggestion, fixed?}`，温度低；解析失败/超时/限流/未配置 → `verdict=none`，**不阻塞提交**。
- 确定性校验仍由后端强制：敏感词（`SensitiveWordFilter`）、宏量 0–100、kcal 区间、能量守恒 ±10%（`MacroConsistencyValidator`）、serving 5–1000。AI 仅判断「名称/品类与营养是否离谱」，输出供人工参考，不直接改用户数据。

## 6. 后端接口（Result + ExceptionConstant，错误码沿用 404xx/409xx 段位并新增审核段位）

C 端（`CustomFoodController`/`CustomFoodService` 扩展）：
- `POST /api/food/custom`：DTO 增加 `kj/unit/image`（与 `energyUnit` 录入单位，kcal 仍为必填基准或由 kj 换算）；新增时 `source=2, audit_status=0`；编辑 0/2 走本人校验，编辑 1 拒绝；编辑 2 → 回 0、清 remark、重跑 AI；每次提交/重提写 SUBMIT/RESUBMIT + AI_CHECK 流水。
- `GET /api/food/custom/mine`：VO 带 `auditStatus/auditRemark/aiVerdict`。
- 删除：仅 0/2 本人可软删；1 拒绝。
- 公共食物查询（`FoodService`）：按 §4 加可见性条件，VO 带 source 标记。

管理端（`AdminFoodController` + 新增 `AdminFoodAuditService`）：
- `GET /api/admin/foods`：查询增 `auditStatus`、`aiVerdict` 参数与字段（提交人、提交时间、缩略图、AI 结论、驳回原因）。
- `POST /api/admin/foods/{id}/audit`：body `{action: APPROVE|REJECT|ADMIN_FIX, remark?, fix?:{...}}`；仅 source=2；REJECT/ADMIN_FIX 校验；写流水。
- 基础食物（含 TFDA）维护沿用现有内置增删改/停用接口；通过后的共建食物允许管理员停用/软删（普通内置编辑接口对 source=2 仍拒绝，改由审核/维护动作处理）。

周期摘要（A，碳循环）：在现有碳循环计划 VO 上补 `dayIndex/totalDays/startDate/endDate/carbPoolTotal/consumedCarb`；`consumedCarb` 由周期起止日期内该用户 `diet_records` 的碳水汇总（复用饮食汇总口径），无记录为 0。不改任何宏量公式。

532（A）：补一个幂等的「生成/确认当期计划」动作（若现有 taper 仅实时算无持久化，则以最小持久化或在用户档案/计划态打标实现），严格复用现有公式，空态→可展示；不实现体重/调碳/续期。

## 7. 管理后台（apps/zhenxinjian-front）

- `view/foods/index.vue`：筛选区加「数据来源（全部/基础食物/用户共建）」「审核状态」「AI 结论」；列表加来源/状态标签、缩略图、AI 结论、提交人/时间；共建行操作改为【通过】【驳回（弹窗填原因）】【修正（弹窗可改字段）后通过】；基础食物保持现有编辑/停用。
- `api/adminFood.ts`：增 audit 接口与查询参数类型。
- 数据来源署名：食物页页脚或系统配置/关于处标注 TFDA OGL 署名。

## 8. 小程序（apps/zhenxinjian-uniapp）

- `pages/home/index.vue`：`goRecord` 改 `uni.switchTab` 到 `/pages/food/index`（含空态「去记录」/快捷「记饮食」）。
- `pages/food/index.vue`：搜索栏加「取消」（清空 keyword、uni.hideKeyboard、退出搜索态）；列表项支持来源标记与无图占位；公共结果含已通过共建食物。
- `pages/food/custom-edit.vue`：加图片上传（`uni.chooseImage` → 现有文件上传接口 → 存相对路径）、单位、数量克数、千焦/千卡下拉与联动；`api/food.ts` 扩字段。
- `pages/food/custom-list.vue`：状态徽标（待审/已通过只读/已驳回+原因）。
- 无图占位：以现有 design-system 色彩按分类做轻量占位（纯色块+品类名/图标），不新增图片美术资源；可在 `components` 增一个 `FoodThumb` 统一「有图显图/无图显占位」。
- `pages/taper/plan.vue`：加「生成周期计划」按钮（空态）→ 调生成接口；档案缺失引导身体数据页。
- `pages/cycle/plan.vue`：加自定义「返回」；按钮文案改「提前结束/切换模式」；`components/ModeSwitchConfirm.vue` 按 F15 重做（周期日期、第 N/总天数、已消耗/总量碳水、不可恢复警示、两按钮），在计划页引用；确认才调既有切换接口。

## 9. TFDA 导入（D）

- 取数（实施时二选一，均合法）：① 用户用浏览器从 TFDA 官方开放数据页下载数据档放入 data-import；② 写限速礼貌型脚本抓官方 HTML/接口（OGL 允许）。**禁止 ostsc.cn**。
- 脚本纳入后端现有 data-import 体系（参考 change4/foods_200 导入）：OpenCC 繁转简（引入轻量转换库或预置映射）→ 名称归一/去重（与内置 200、已导入按规范名+别名）→ TFDA A–R 18 大类映射到本系统 01–10 分类 → 每 100g kcal/kJ/三宏落库，越界/缺失进异常清单 → `source=1, audit_status=3, status=1, data_batch='TFDA:<版本>'`；无图（image 空，前端占位）。
- 幂等：以来源编号（TFDA 食物 code，如 A0320301 存 `code` 或映射键）+规范名去重，重复跑不新增、不覆盖人工修正。
- 署名：小程序「关于/数据来源」+ 后台标注「卫生福利部食品药物管理署（OGL）」。
- 风险：TFDA 编号/栏位以实际数据档为准；繁体方言名（洋芋/马铃薯）靠别名归一；中式熟菜肴缺失（接受，后续靠共建）。

## 10. 关键边界与兼容

- 老用户自定义食物回填 audit_status=1，保证升级后既有行为不回退（不会突然只读/消失）。
- 图片相对路径经现有 host 重写（最近已修复 localhost→API host），真机可用；上传走 `StorageService`，不接受外链。
- AI 降级、审核未完成都不影响用户用自己的食物记饮食（核心记录链路不被审核阻塞）。
- 后台审核动作、C 端越权（改他人/改公共）都要有权限与归属校验。

## 11. 测试策略

- 后端单测：状态机（提交/通过/驳回/重提/只读）、可见性 SQL（公共集/我的/记录可达）、守恒与区间校验、kj/kcal 换算、AI 降级（超时/限流/坏 JSON→none 不阻塞）、审核越权、TFDA 清洗去重与幂等、周期碳水汇总。
- 接口测试：投稿→AI→后台通过/驳回全链路、来源/状态筛选、越权 403/业务错误。
- 前端：按手测清单验证首页直达食物库、取消、532 生成、F15 弹窗取消/确认、投稿表单与占位图。

## 12. 风险与对策

| 风险 | 对策 |
| --- | --- |
| AI 误判影响上架 | AI 仅打标不决策，人工终审；suspect 重点、pass 批量；全程留痕可回溯 |
| 审核成为共建瓶颈 | 提交即私有可用，不阻塞用户；后台支持按 AI 结论批量处理 |
| TFDA 官网下载受网络限制 | 浏览器手动下载与礼貌脚本双方案；导入脚本可离线复跑，不依赖开发期联网 |
| 图片版权/裂图 | 只接受用户上传到自有存储；无图统一分类占位；不抓第三方图 |
| 存量自定义行为回退 | 升级回填 audit_status=1，平滑过渡 |

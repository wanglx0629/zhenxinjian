# 食物库共建审核 + PART2 交互 — Tasks

> 执行顺序即编号顺序；每步完成后勾选 `[x]`。规约：建表/加列必备 `delete_flag`（+业务表 `status`）、配套字典枚举（code+desc+of）；错误文案进 `ExceptionConstant`、接口返回 `Result`、注释 `作者: wanglx`；MySQL/Redis 守开发规范；遵循 TDD（先写/改测试再实现）。JDK 25（`set JAVA_HOME=D:\App\java\25`）。

## 1. 数据库

- [x] 1.1 新建 `sql/change14_food_curation.sql`：
  - `ALTER TABLE foods` 加列 `audit_status TINYINT NOT NULL DEFAULT 3`、`kj INT NULL`、`unit VARCHAR(16) NULL`、`image VARCHAR(512) NULL`、`audit_remark VARCHAR(255) NULL`、`ai_verdict VARCHAR(16) NULL`、`ai_suggestion VARCHAR(500) NULL`、`submit_time DATETIME NULL`、`data_batch VARCHAR(32) NULL`
  - 回填：内置 source=1 → audit_status=3；存量自定义 source=2 → audit_status=1、ai_verdict='none'
  - 索引 `idx_source_audit(source,audit_status,delete_flag)`、`idx_user_audit(user_id,audit_status)`（不加与软删冲突的普通 UNIQUE）
  - 新建 `food_audit_log`（id、food_id、action、operator_id、ai_verdict、ai_suggestion、remark、snapshot_before/after JSON 或 TEXT、status、delete_flag、create_time + 索引 idx_food）
  - `schema_migrations` 补账 version=14
- [x] 1.2 本地库执行 change14 并验证加列/回填/索引/补账（参照 change13，utf8mb4 连接）
  - 2026-09-24 已执行：9 列 + 2 索引 + food_audit_log + ai.food-audit-model 种子 + v14 补账；存量自定义仅 8 条软删测试数据（delete_flag=1，按规正确跳过回填）

## 2. 后端 · 枚举与常量

- [x] 2.1 新增 `common/enums/FoodAuditStatusEnum`（0待审核/1已通过/2已驳回/3无需审核，code+desc+of）
- [x] 2.2 新增审核动作枚举 `FoodAuditActionEnum`（SUBMIT/AI_CHECK/APPROVE/REJECT/RESUBMIT/ADMIN_FIX）与 AI 结论常量（pass/suspect/reject/none）
  - AI 结论落为 `FoodAiVerdictEnum`（字符串编码枚举，code+desc+of，对应 ai_verdict 列）
- [x] 2.3 `CommonConstant`/`ExceptionConstant` 增补审核相关错误码与文案：非共建食物不可审核、公共食物仅管理员可维护、驳回原因必填、AI 未校验不阻塞等（沿用 404xx/409xx 段位）
  - 40407 FOOD_PUBLIC_READONLY、40915 AUDIT_TARGET_INVALID、40916 REJECT_REASON_REQUIRED、40917 FIX_INVALID
- [x] 2.4 `ProjectConfigKeyConstant` 增 `AI_FOOD_AUDIT_MODEL`；`project_config` 加种子（change14 SQL 内，value_type=4/字符串，给可用文本模型缺省值）
  - 种子 ai.food-audit-model=glm-4-flash（value_type=1 字符串，change14 已入库）

## 3. 后端 · 实体/DTO/VO/Mapper

- [x] 3.1 `Food` PO 与 `FoodVO` 增加 auditStatus/kj/unit/image/auditRemark/aiVerdict/aiSuggestion/submitTime/dataBatch（VO 已有的 image 改为 foods.image 优先、food_images 兜底）
  - VO 另加 submitterName（仅管理端分页填充）；foods.image 优先逻辑在 §5/§6 的 VO 组装处落实
- [x] 3.2 `CustomFoodSaveDTO` 增 kj、unit、image、energyUnit（录入单位 KJ/KCAL；kcal 仍为入库基准）；serving 语义保留
- [x] 3.3 新增 `FoodAuditLog` PO + `FoodAuditLogMapper`
- [x] 3.4 新增后台审核 DTO/VO：`FoodAuditDTO{action,remark,fix}`、后台食物分页项含来源/状态/AI 结论/提交人/提交时间/缩略图
  - `AdminFoodQuery` 增 auditStatus/aiVerdict 筛选；分页项复用 FoodVO 扩展字段

## 4. 后端 · AI 文本校验

- [x] 4.1 `AiChatClient` 增 `chatText(model, systemPrompt, userText, timeout)`（默认传空图片）；`ZhipuAiChatClient` images 为空时构造纯文本消息（不放 image_url）
  - 实现为抽象方法 + ZhipuAiChatClient 纯文本 user content；公共调用链抽 `invoke()`（错误分类/懒构建复用）
- [x] 4.2 新增 `FoodAuditAiService`：组装名称/分类/kj/kcal/三宏 prompt，约束只回 JSON{verdict,suggestion,fixed?}，低温、解析；异常/超时/限流/未配置/坏 JSON → verdict=none（不抛错阻塞）
  - fixed 并入 suggestion 文本约定（列宽 500 截断），模型配置 ai.food-audit-model（空=关闭）
- [x] 4.3 单测：pass/suspect/reject 解析、坏 JSON 与 AI 异常降级为 none（FoodAuditAiServiceTest 9 场景全绿）

## 5. 后端 · 共建服务与状态机

- [x] 5.1 新增 `FoodAuditLogService`：只追加写流水（动作/操作人/AI 快照/原因/前后 JSON）
- [x] 5.2 改 `CustomFoodService.save`：新增置 source=2/audit_status=0/submitTime；编辑仅限本人且状态 0/2；编辑状态 2 → 回 0、清 audit_remark、RESUBMIT；写 SUBMIT/RESUBMIT + AI_CHECK 流水；调用确定性校验（守恒/区间/敏感词/serving）+ kj/kcal 换算
- [x] 5.3 改删除/归属校验：状态 1（公共）原作者不可编辑/删除；0/2 本人可编辑/软删
- [x] 5.4 单测覆盖状态机、只读、重提回流、越权、守恒、换算
  - 2026-09-24：CustomFoodServiceTest 22 场景全绿
- [x] 5.5 改公共食物查询（`FoodService`/Mapper）：可见性 `(source=1,status=1) OR (source=2,audit_status=1,status=1)`；「我的」返回本人 0/1/2；添加记录可达性=公共 ∪ 本人全部；VO 带来源标记
- [x] 5.6 单测：公共集/我的/记录可达三类可见性，私有不泄漏他人
  - 2026-09-24：FoodServiceTest 21 + DietRecordServiceTest 13 + FoodAuditLogServiceTest 5 全绿（合计 61）

## 6. 后端 · 管理审核接口

- [x] 6.1 `AdminFoodController` 分页增 auditStatus/aiVerdict 参数与字段返回；新增 `AdminFoodAuditService` + `POST /api/admin/foods/{id}/audit`（APPROVE/REJECT/ADMIN_FIX；仅 source=2；REJECT 必填原因；ADMIN_FIX 校验后落修正值；均写流水）
  - 分页另填 submitterName/缩略图（foods.image 优先不被预热图覆盖）；审计目标限状态 0/2，已通过走维护入口
- [x] 6.2 普通内置编辑/停用接口对 source=2 维持拒绝（40404）；通过后共建食物允许管理员停用/软删（走明确维护入口）
  - `PUT /api/admin/foods/{id}/custom-status`、`DELETE /api/admin/foods/{id}/custom`（仅 source=2 且已通过，40915 拦截）
- [x] 6.3 接口测试：通过/驳回（缺原因失败）/修正后通过/审核基础食物被拒/非 ADMIN 被拒全链路
  - 2026-09-24：AdminFoodAuditServiceTest 16 + AdminFoodServiceTest 6 + AdminFoodControllerTest 12 全绿（合计 34）

## 7. 后端 · PART2（532 生成 + 碳循环切换摘要）

- [x] 7.1 碳循环计划 VO 补 startDate/endDate/totalDays/dayIndex/carbPoolTotal/consumedCarb；service 用周期起止日期内 diet_records 汇总碳水（复用饮食汇总口径，无记录 0）；不改宏量公式；单测验证汇总与边界
  - startDate/endDate/carbPool 既有字段保留；新增 totalDays/dayIndex（今日不在周期内 null）/carbPoolTotal（=碳水池）/consumedCarb（起止闭区间内 diet_records.carb_g 求和，Numbers.round1）；空态周期不输出摘要
  - 2026-09-24：CyclePlanServiceTest 10 场景全绿（含汇总/无记录 0/越界边界）
- [x] 7.2 532 新增幂等「生成/确认当期计划」接口（最小持久化或计划态打标），严格复用现有四阶段/今日目标公式；档案缺失拦截；重复生成不产生重复；单测
  - 计划态打标：`sql/change15_taper532_plan.sql` user_body 加 `taper_plan_time`（NULL=未生成，本地库已执行并补账 v15）；`POST /api/taper-532/generate` 幂等（单行刷新标记），未建档 40601；Taper532VO 增 generated 供空态判定；数值仍走 buildPlan 实时公式（基线+下调+经期上浮）
  - 2026-09-24：Taper532ServiceTest 11 + Taper532ControllerTest 2 全绿；全量 mvn test 见 §12.1

## 8. 管理后台（apps/zhenxinjian-front）

- [x] 8.1 `api/adminFood.ts`：分页查询加 source/auditStatus/aiVerdict 参数与字段类型；新增 audit 接口
  - 另加共建维护入口 changeCustomFoodStatus/deleteCustomFood；FoodAuditFixPayload/FoodAuditPayload 类型对齐后端 FoodAuditDTO
- [x] 8.2 `view/foods/index.vue`：筛选加数据来源（全部/基础/用户共建）、审核状态、AI 结论；列表加来源/状态标签、缩略图、AI 结论与建议、提交人/时间、驳回原因
  - 字典统一进 `constants/dicts.ts`（FOOD_SOURCE/AUDIT_STATUS/AI_VERDICT）；行操作按来源/审核状态分流；页脚 TFDA OGL 署名
- [x] 8.3 共建行操作：通过、驳回（弹窗必填原因）、修正后通过（弹窗编辑字段）；基础食物保持现有增改/停用；TFDA OGL 署名展示
  - 修正后通过拆 `components/FoodAuditFixDialog.vue`（预填原值、AI 建议提示、整体校验交后端）；已通过共建走停用/软删维护入口
  - 2026-09-24：`npm run build`（vue-tsc + vite）通过
- [ ] 8.4 本地联调：待审队列按 AI 结论筛选、三种审核动作与列表状态刷新（并入 §12.2 手测）

## 9. 小程序 · PART2 交互（A）

- [x] 9.1 `pages/home/index.vue`：记饮食/去记录改 `uni.switchTab('/pages/food/index')`
  - goRecord 统一改食物库 tab（快捷「记饮食」与空态「去记录」共用）
- [x] 9.2 `pages/food/index.vue`：搜索栏加「取消」（清空 keyword、hideKeyboard、退出搜索态）
  - 搜索态（关键字/分类/已出结果）显示取消；取消清空关键字与分类、取消防抖、收起键盘、回发现页
- [x] 9.3 `pages/taper/plan.vue`：空态加「生成周期计划」按钮 → 调 7.2 接口；档案缺失引导身体数据页
  - 空态二分支：stages 空=未建档（去录入）／generated=false=已建档未生成（生成按钮，提交态防重复）；api/taper.ts 增 generateTaper532Plan + generated 字段
- [x] 9.4 `pages/cycle/plan.vue`：加自定义「返回」；按钮文案改「提前结束/切换模式」；引用改造后的确认组件
  - 返回页面栈空时兜底回首页 tab；确认走 F15 弹窗后才调既有切换接口（switchMode(1)，切 532 自动终止周期）
- [x] 9.5 `components/ModeSwitchConfirm.vue` 按 F15 重做：周期日期、第 N/总天数、已消耗/总量碳水、不可恢复警示、「确认切换并重启周期 / 取消，继续当前周期」；确认才调既有切换接口，取消无副作用
  - props 增 plan 摘要（api/cycle.ts 扩 totalDays/dayIndex/carbPoolTotal/consumedCarb）；mode/select 与 cycle/plan 两处引用均传入
- [x] 9.6 体重历史记录维持现状（确认无 `[object Object]`，不改动）
  - 2026-09-24：`npm run type-check`（vue-tsc --noEmit）通过

## 10. 小程序 · 共建投稿（C）

- [x] 10.1 `api/food.ts` 自定义保存/我的列表扩字段（kj/unit/image/energyUnit/auditStatus/auditRemark/aiVerdict）
  - FoodVO 另回填 aiSuggestion/submitTime；新增 `uploadFoodImage`（uni.uploadFile→/files/upload，Bearer 头 + Result 解包同 request.ts）
- [x] 10.2 `pages/food/custom-edit.vue`：图片上传（chooseImage→统一文件上传→存相对路径）、单位、数量克数、千焦/千卡下拉与 4.184 联动、三宏；提交守恒前端预校验
  - KJ 录入按 ÷4.184 四舍五入换 kcal 提交（同后端 resolveEnergy）；编辑态有 kj 记录默认回显 kJ；未换图沿用原值提交（后端绝对 URL 原样透传）；图片 ≤10MB（同后端 multipart 上限）
- [x] 10.3 `pages/food/custom-list.vue`：审核状态徽标（待审/已通过只读/已驳回+原因），已通过项只读样式
  - 已通过(1)隐藏编辑/删除、名称弱化+「只读」标识、点击 toast 提示公共只读；已驳回展示驳回原因
- [x] 10.4 新增/复用 `FoodThumb`：有图显图、无图按分类占位（design-system 色块+品类，无新增美术资源）；food/index、detail、custom-list 统一接入；列表带来源/共建标记
  - 新建 `components/FoodThumb.vue`（10 分类色块+品类简称，V2 色板同族浅底/深字）；food/index 热门+结果、food/detail 头图、custom-list 行统一替换
  - 2026-09-24：`npm run type-check`（vue-tsc --noEmit）通过
- [ ] 10.5 真机/开发者工具联调：投稿→私有立即可用→后台通过后他人可见；驳回改后重提；图片经 API host 可加载（并入 §12.2 手测）

## 11. TFDA 数据导入（D）

- [x] 11.1 获取 TFDA 官方数据档（浏览器手动下载 或 限速礼貌脚本抓官方，OGL；禁止 ostsc.cn），放入 data-import 原始目录并记录来源版本
  - 官方 .gov.tw 对本地网络全屏蔽（TLS 握手被掐、直连不通、真实 Edge 亦被拒）；改经 OGL 授权再分发镜像 GitHub ating1234/food（其 Actions 每月对官方 SHA256 比对同步）
  - 已落 `data-import/tfda/raw/`：18 分类 JSON + foods_index + version.json，**版本 20.5**（2026-09-03 官方生成），2180 项/18 类，溯源见 `raw/SOURCE.json`（官方 dataset 14197 / data.fda.gov.tw DatasetId=43，可回溯复核）
- [x] 11.2 编写可复跑导入脚本（纳入现有 data-import 体系）：OpenCC 繁转简 → 名称/别名归一 → 与内置 200/已导入去重（TFDA code 映射键）→ A–R 18 大类映射本系统 01–10 → kcal/kJ/三宏对齐每 100g，异常进清单 → source=1/audit_status=3/status=1/data_batch='TFDA:<版本>'，无图
  - 离线清洗 `python/tfda_clean.py`（OpenCC 繁转简/规范名去重/内置 200 重复跳过/18 类映射 01–10/营养区间校验/饮料类未检出按 0 人工裁决）产出 `data-import/tfda/clean/foods_tfda.json`（1730 条）+ abnormal.json + report.txt，复制入后端 resources 由 `TfdaFoodInitializer`（@Order(2)）启动幂等导入
  - 导入统计（report.txt）：原始 2180 → 待导入 1730、与内置 200 重复跳过 126、规范名去重 298、营养异常 26（人工清单）、饮料类按 0 入库 28
- [x] 11.3 幂等验证：重复跑不新增、不覆盖人工修正；输出导入/跳过/异常统计并人工抽检营养值
  - 2026-09-24：TfdaFoodInitializerTest 4 场景 + FoodLibraryInitializerTest 5 场景全绿（就绪跳过不插入/空库全量导入字段口径对拍 A05002 稉米/唯一键冲突跳过不覆盖/条数不符拒绝启动）
- [x] 11.4 小程序「关于/数据来源」与后台加「卫生福利部食品药物管理署（OGL）」署名
  - 小程序 mine 关于卡（数据来源：卫生福利部食品药物管理署（OGL））；后台 foods 页脚 TFDA OGL 署名（见 8.2）

## 12. 测试与回归

- [x] 12.1 后端 `mvn test` 全绿（JDK25）；新增单测/接口测试覆盖 §4–§7
  - 2026-09-24：全量 `mvn test` 447 场景全绿（BUILD SUCCESS, 01:01 min）；新增覆盖见 §4–§7 各条（合计新增 84 场景）
  - 2026-09-24 终验：§11 初始化器测试并入后全量 458 场景全绿；发现并修复 2 处回归/缺陷：
    ① fillImages 空码回退 `Map.of()` 上 `get(null)` 抛 NPE（无 code 的共建食物无图时搜索/详情/管理分页 500）——FoodService/AdminFoodService 预热图映射空集改回 null 键容忍的 HashMap
    ② AdminFoodServiceTest 单测隔离缺陷（缺 FoodImage TableInfo 初始化，单类运行报 lambda cache 错误，全量被测试顺序污染掩盖）——setUp 补 initTableInfo
- [ ] 12.2 三端手测清单：首页直达食物库、取消、532 生成、F15 取消/确认、投稿全链路、后台筛选与审核、TFDA 列表/详情占位与署名
- [ ] 12.3 回归：现有食物库搜索/离线兜底、拍照识别 OCR、饮食记录引用（含私有食物转公共后不断链）、游客/登录迁移无回退

## 13. 收尾

- [x] 13.1 `openspec validate food-curation-part2` 通过
  - 2026-09-24：validate 通过（`Change 'food-curation-part2' is valid`）
- [ ] 13.2 自查 SQL/枚举/Result/异常码/注释/软删/status 等规约；准备 verify 与提交（提交信息 `feature：...`，不主动 push）
  - 自查通过：change14/15 SQL（幂等/软删+status/utf8mb4/无软删普通 UNIQUE/版本账）、三枚举（code+desc+of）、控制器全 `Result` 返回、异常文案走 `ExceptionConstant`（40407/40915/40916/40917）、注释 `作者: wanglx`
  - 本地环境复核：库 v13–15 齐、foods 审核列 9 列齐、TFDA:20.5 已入库 1730 条（后端 8080 为最新代码启动时自导入）、临时探针脚本与测试日志已清理
  - 待办：全量 `mvn test` 终验 → 12.2/12.3 手测 → verify → 提交

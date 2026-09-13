# Badsmells — 2026-09-11-full-project-scan（ARCH 架构级）

- **级别**：ARCH（架构级）
- **范围**：仓库全量（三端 + 横切面）
- **提交版本**：`ee2a2fa927a5f66486b85a42a2a40e056ad60903`
- **识别方式**：v1.0 四路并行静态侦察；v1.1 盲区补扫（Migrator 编排族 / 定时任务层 / 引导配置层 / resources / 小程序全局层 / 埋点链路）+ 高危证据复核
- **检出语言**：Java、TypeScript、Vue SFC

---

## §2.0 索引

| BS-ID | 类别 | 严重度 | 状态 |
| --- | --- | --- | --- |
| ARCH-耦合-001 | 耦合 | 高 | 已消除 |
| ARCH-耦合-006 | 耦合 | 高 | 已消除 |
| ARCH-内聚-001 | 内聚 | 高 | 已消除 |
| ARCH-内聚-002 | 内聚 | 高 | 已消除 |
| ARCH-内聚-003 | 内聚 | 高 | 已消除 |
| ARCH-层次-001 | 层次 | 高 | 已消除 |
| ARCH-层次-003 | 层次 | 高 | 已消除 |
| ARCH-演进-001 | 演进 | 高 | 已消除 |
| ARCH-演进-002 | 演进 | 高 | 已消除 |
| ARCH-演进-007 | 演进 | 高 | 已消除 |
| ARCH-边界-004 | 边界 | 高 | 已消除 |
| ARCH-边界-005 | 边界 | 高 | 已消除 |
| ARCH-演进-003 | 演进 | 中 | 已消除 |
| ARCH-演进-004 | 演进 | 中 | 已消除 |
| ARCH-演进-005 | 演进 | 中 | 已消除 |
| ARCH-演进-008 | 演进 | 中 | 已消除 |
| ARCH-耦合-002 | 耦合 | 中 | 已消除 |
| ARCH-耦合-003 | 耦合 | 中 | 已消除 |
| ARCH-耦合-004 | 耦合 | 中 | 已消除 |
| ARCH-耦合-005 | 耦合 | 中 | 已消除 |
| ARCH-内聚-004 | 内聚 | 中 | 已消除 |
| ARCH-内聚-005 | 内聚 | 中 | 未清除 |
| ARCH-内聚-006 | 内聚 | 中 | 未清除 |
| ARCH-内聚-007 | 内聚 | 中 | 未清除 |
| ARCH-层次-002 | 层次 | 中 | 未清除 |
| ARCH-边界-001 | 边界 | 中 | 未清除 |
| ARCH-边界-006 | 边界 | 中 | 未清除 |
| ARCH-边界-007 | 边界 | 中 | 未清除 |
| ARCH-模块化-001 | 模块化 | 低 | 未清除 |
| ARCH-模块化-002 | 模块化 | 低 | 未清除 |
| ARCH-模块化-003 | 模块化 | 低 | 未清除 |
| ARCH-边界-002 | 边界 | 低 | 未清除 |
| ARCH-边界-003 | 边界 | 低 | 未清除 |
| ARCH-边界-008 | 边界 | 低 | 未清除 |
| ARCH-演进-006 | 演进 | 低 | 未清除 |
| ARCH-演进-009 | 演进 | 低 | 未清除 |

状态说明：**未清除** / 已消除 / 部分残余。共 36 条（高 12 / 中 16 / 低 8）。

---

## §2.1 明细

### ARCH-耦合-001 — WebSocket 演示板残留并耦合核心认证链路

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 耦合（兼演进） |
| 严重度 | 高 |
| 症状 | 演示聊天板（广播"加入了房间"）已无业务价值，但默认 `enabled=true`，且三个核心类硬依赖 `WebSocketSessionRegistry.kickUser`，删除 ws 需同步改 3 处 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/websocket/DemoWebSocketHandler.java:30`、`DemoWebSocketInterceptor.java:33`、`WebSocketSessionRegistry.java:20`、`config/WebSocketConfig.java:29`（Origin 白名单硬编码 `localhost:5173/5174` 出现两次 :71-75/:85-90）；依赖方 `service/impl/UserServiceImpl.java:55,87,242`、`WechatAuthServiceImpl.java:53,76,128,286`、`task/GuestCleanupTask.java:36,65`；`application.yml:72`。v1.1 复核：UserServiceImpl :55 字段注入、:87 login 内 `kickUser`、:242 `invalidateUserSession()` 内 `kickUser` 均在 |
| 根因 | 项目由演示工程转型，ws 模块未随业务剥离；会话踢出能力错误地建在演示基建上 |
| 影响 | 牵一发动三处；保留无用端口面与 Origin 配置维护成本 |
| 修复建议 | 先抽象 `SessionEvictor` 空实现替换 `kickUser` 调用，再整体删除 ws 包与配置 |
| 状态 | 已消除（B-T01 于 2026-09-11 完成：`service/SessionEvictor` + `NoopSessionEvictor` 替换 5 处调用点；删除 `websocket/` 包 3 文件、`WebSocketConfig`、pom websocket starter、`zhenxinjian.websocket.*` 配置与 `/ws/**` 白名单） |

### ARCH-耦合-006 — 游客迁移无编排者：双调用点复制 + 顺序无契约

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 耦合（兼层次） |
| 严重度 | 高 |
| 症状 | 6 个 Migrator 实现 `GuestDataMigrator` 接口，靠 `SpringUtils.getBeansOfType()` Map 遍历驱动，无编排类；该循环复制在两处，迁移器间执行顺序依赖 classpath 扫描无契约，且缓存失效早于合并提交 |
| 证据 | 调用点一 `service/impl/WechatAuthServiceImpl.java:275-278`（登录迁移），调用点二 `task/GuestCleanupTask.java:46,59-61`（过期清理）；顺序敏感证据：`service/impl/UserRecordMigrator.java:23-29` 在 `migrate()` 内即时 evict 缓存，而 `WechatAuthServiceImpl.java:280-282` 的 `merged_into` 标记+软删在所有 Migrator 之后才写，并发读可重建合并前旧缓存 |
| 根因 | 迁移族按"可插拔"设计但未定义顺序契约与统一编排入口 |
| 影响 | 数据一致性风险；新增迁移器时两个调用点行为可能分叉 |
| 修复建议 | 引入 `GuestMigrationOrchestrator`（接口 `Ordered` 定序），两个调用点收敛为一处；缓存失效移到合并提交之后 |
| 状态 | 已消除（B-T02 于 2026-09-12 完成：`service/GuestMigrationOrchestrator` List 注入按 `Ordered` 排序，双调用点收敛；`GuestDataMigrator` 扩展 `Ordered` 默认 0 契约；`UserRecordMigrator` 缓存 evict 改 `afterCommit` 后置；新增编排者/缓存后置 6 个测试用例，回归全绿） |

### ARCH-内聚-001 — 能量守恒 ±10% 校验五处复制（跨端）

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚（兼演进） |
| 严重度 | 高 |
| 症状 | 同一 4/4/9 ±10% 校验在后端三个 service 各自定义常量，前端两个页面又各写一份，且上限常量不一致 |
| 证据 | 后端：`service/impl/DietRecordService.java:61-65,333-341`、`CustomFoodService.java:36-40,157-165`、`AdminFoodService.java:40-43,142-150`；前端：`apps/zhenxinjian-uniapp/src/pages/record/add.vue:88-95`（GRAMS_MAX=5000/KCAL_MAX=20000）vs `pages/food/custom-edit.vue:117-129`（硬编码 `carb*4+protein*4+fat*9`，上限 100/900）；另 `pages/food/detail.vue:16-17` GRAMS_MAX=10000 与 add 页冲突 |
| 根因 | 口径无单一归属，各端各页就地实现 |
| 影响 | 口径修改须同步五处，已出现上限值分裂（5000 vs 10000） |
| 修复建议 | 后端收敛为共享校验器；前端收敛到 `utils/validate.ts` 并以后端为准，删除页面私有副本 |
| 状态 | 已消除（B-T03 于 2026-09-12 完成：后端 `common/utils/MacroConsistencyValidator` 收敛 DietRecordService/CustomFoodService/AdminFoodService 三处副本（含 100/900 上限常量锚定）；前端 `utils/validate.ts` 新增 `checkKcalConsistency/kcalFromMacros/KCAL_TOLERANCE` 替换 record/add.vue 与 custom-edit.vue 私有副本；复核裁定 GRAMS_MAX 5000（记录份量）vs 10000（详情试算）为不同场景各自与后端 AMOUNT_MAX/FOOD_CALC_MAX_GRAMS 一致，非真实漂移，detail.vue 加区分注释；新增校验器边界测试 6 用例，回归全绿） |

### ARCH-内聚-002 — 三宏进度展示逻辑页内重复实现

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚 |
| 严重度 | 高 |
| 症状 | `utils/macro.ts` 已收敛 `buildProgressItems/buildAdviceList`，但记录页又手写第二份三色阈值/150% 上限/超标量与建议映射逻辑（数值本身来自后端 summary，非前端算数，问题在展示口径双份维护） |
| 证据 | `apps/zhenxinjian-uniapp/src/utils/macro.ts:25-56` vs `pages/record/index.vue:36-55`（手写进度）与 `:63-72`（手写超标建议映射）；另与 `utils/calculator.ts:407-427 overCheck` 功能重叠却未复用 |
| 根因 | 收敛做了一半，第二处未迁移 |
| 影响 | 进度口径（本轮刚统一为 150%）再次漂移只是时间问题 |
| 修复建议 | `record/index.vue` 改为调用 `utils/macro.ts`，删除本地副本 |
| 状态 | 已消除（B-T04 于 2026-09-12 完成：`pages/record/index.vue` 删除手写 progressItems 38 行与 adviceList 8 行本地副本，改为 `buildProgressItems(dietStore.summary)` / `buildAdviceList(overItems)` 调用 `utils/macro.ts` 单一真源，同步移除 PROGRESS_THRESHOLD/PROGRESS_COLORS 常量直引；口径（三色阈值 80/100、150% 上限、超标量 1 位小数、建议映射）与真源逐行比对无行为差异；vue-tsc 全绿） |

### ARCH-内聚-003 — AI skills 资产 3-4 副本入库且已漂移

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚（兼演进） |
| 严重度 | 高 |
| 症状 | 同一套 skills 复制进 6 个 IDE 目录（222 文件 ~2.4MB 入 git 历史），副本已开始字节级分叉，无单一真源 |
| 证据 | `.agent/`、`.agents/`、`.claude/`、`.codex/`、`.opencode/`、`.trae/`；重复样本：`frontend-design/SKILL.md` 9,461B ×3、`mcp-builder/` 125KB ×3、`khufu-api/SKILL.md` 21,647B ×4；漂移样本：`openspec-explore/SKILL.md` .opencode/.trae=16,221B vs .agents=16,269B；来源提交 `86ec995` |
| 根因 | 各 IDE 约定目录各拷一份，无链接/引用机制 |
| 影响 | 更新必然漂移；仓库体积膨胀 |
| 修复建议 | 保留单一真源目录，其余改为安装脚本软链/复制生成；或仅保留 `.trae/` 全集 |
| 状态 | 已消除（B-T05 于 2026-09-12 完成：全量 MD5 比对裁定重复样本内容完全一致，"漂移样本"实为 per-IDE 包装差异（.agents 用 `$openspec-X (Codex) or /openspec-X (other agents)` 占位符、.opencode opsx 命令无 front-matter `name:` 行），非内容分叉；`.trae/` 定为单一真源，独有资产 `.agent/skills/ui-ux-pro-max`（28 文件）git mv 归入；新增 `scripts/sync-ide-skills.ps1` 分发脚本（5 IDE 目录 = 纯拷贝 + name 行剔除 + 占位符反向映射三类机械生成）；`.agent/.agents/.claude/.codex/.opencode` 入 .gitignore 并 `git rm --cached` 退库 120 文件（24703 行删除）；脚本再生后与原状字节级比对 0 差异，本地 IDE 加载不受影响） |

### ARCH-层次-001 — 小程序页面绕过 store 直调 api

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 层次 |
| 严重度 | 高 |
| 症状 | pages→store→api 主链路被四处穿透；532 计划业务干脆没有 store，数据只活在页面局部变量 |
| 证据 | `pages/menstrual/index.vue:9`（store/menstrual 存在却不用）、`pages/home/index.vue:17`、`pages/mine/index.vue:18`、`pages/taper/plan.vue:8,13`；登出清理责任也漏在页面：`pages/mine/index.vue:139-146` 手工 reset 5 个 store；`pages/mode/select.vue:41-91` 页面编排跨 store 切换事务 |
| 根因 | 逐页增量开发未回补状态层 |
| 影响 | 状态真源分裂；新增 store 时登出清理必漏 |
| 修复建议 | 补 `store/taper.ts`；四处直调改经 store；登出清理收敛到 user store 统一编排 |
| 状态 | 已消除（B-T06 于 2026-09-12 完成：新建 `store/taper.ts` 收敛 532 计划状态、`store/reminder.ts` 收敛提醒状态；taper/plan、reminder/index、mine/index、home/index、menstrual/index 五处页面直调全部改经对应 store；`userStore.logout()/abandonGuest()` 统一编排七大业务 store reset（原 mine/index.vue 手工 reset 5 个且漏 taper/reminder）；`cycleStore.switchMode` 收敛 mode/select.vue 直调 `switchDietMode` 与切回 532 的本地清空；pages/components 对 `@/api/*` 仅剩 type-only import，vue-tsc 全绿） |

### ARCH-层次-003 — 游客清理任务巨型事务

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 层次（兼边界） |
| 严重度 | 高 |
| 症状 | `@Transactional` 标注在方法上，`while(true)` 分批（每批 200）全部落在同一事务，过期游客越多事务越长 |
| 证据 | `task/GuestCleanupTask.java:42-76` |
| 根因 | 批处理与事务边界未分离 |
| 影响 | 锁与 undo log 无上界；高峰期清理任务可拖垮库 |
| 修复建议 | 改为每批独立事务（拆出带 `@Transactional` 的批方法或用编程式事务） |
| 状态 | 已消除（B-T07 于 2026-09-12 完成：拆出 `GuestCleanupBatchExecutor` 独立 Bean 承载 `@Transactional purgeOneBatch`（选批 + 批内清理一事务，经 Spring 代理调用，规避同类自调用事务失效）；任务方法去除大 `@Transactional` 只编排批次循环，单事务长度以 BATCH_SIZE=200 为上界；新增每批 size/costMs 日志；软删经 @TableLogic 自动排除已清理行保证批间幂等推进；补 `GuestCleanupTaskTest` 5 用例，123 测试全绿） |

### ARCH-演进-001 — SQL 迁移无版本管理、无回滚、无幂等

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进 |
| 严重度 | 高 |
| 症状 | 9 个迁移脚本靠文件名序号人脑记账；重跑必报错，靠 `--allow-error` 吞错；破坏性多步无事务编排 |
| 证据 | `sql/change1_users_guest.sql` ~ `change9_admin_analytics.sql`（无 `IF NOT EXISTS`/条件守卫）；`sql/change2_users_delete_flag.sql:10-24`（DROP 生成列→RENAME→重建，中断即半迁移态）；`tools/db/SqlRunner.java:33`（`--allow-error`）；全库无 `schema_migrations`/flyway/liquibase 痕迹 |
| 根因 | 单人快速迭代未建迁移基建 |
| 影响 | 环境间状态不可追溯；新环境搭建高风险 |
| 修复建议 | 引入版本表（最小方案 `schema_migrations` + 脚本幂等守卫）或 Flyway；补回滚说明 |
| 状态 | 已消除（B-T08 于 2026-09-12 完成：`schema_migrations` 版本表 + `SqlRunner` change<N> 自动建表/查账/SKIP/记账、`--allow-error` 移除失败即终止且不记账；9 个 change 脚本补幂等守卫（ALTER 段 information_schema 条件守卫 + CREATE IF NOT EXISTS + change2 破坏性三步断点续跑）；存量环境 `change_backfill_migrations.sql` 回填；全新环境引导成文（建库→data.sql 基线→change 链）；五场景演练全绿：全链/重跑全 SKIP/守卫零重复重建/失败 exit 2 不记账/回填后全 SKIP；123 测试全绿） |

### ARCH-演进-002 — 前端影子算法库残留且口径已漂移

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进（兼边界） |
| 严重度 | 高 |
| 症状 | "计算以后端为准"方针下，`calculator.ts` 443 行中近 400 行无调用方；其中 `isPlateau` 按"7 条记录"实现，与后端"7 天窗口 ≥2 条"口径冲突，属潜伏雷 |
| 证据 | `apps/zhenxinjian-uniapp/src/utils/calculator.ts:407-427`（overCheck 无调用）、`:437-443`（isPlateau 用 `slice(-7)` 按条数）；后端口径 `service/impl/WeightService.java:175-190`（`record_date >= today-6` 且 ≥2 条）——v1.1 复核确认两端分叉且前端该函数全库零调用；实际仅 `pages/body/profile.vue:11` 使用 bmr/tdee/macro532Base；`store/weight.ts:30` 消费后端返回的 `plateau` 字段；`utils/format.ts:5` 反向 import calculator 的 round |
| 根因 | 离线兜底策略未随"后端真源"落地而清理 |
| 影响 | 影子实现随时可能被误启用，口径与后端分叉 |
| 修复建议 | 删除死函数；保留的预览函数标注"仅展示兜底"；isPlateau 立即删除 |
| 状态 | 已消除（B-T09 于 2026-09-12 完成：calculator.ts 443→66 行，isPlateau 口径分叉雷与 overCheck/menstrualPhase/plan532Stages/cycleAlloc/carbonConst 等零调用死函数及全部影子类型全删，仅留录入页预览 bmr/tdee/macro532Base 并标注「仅展示兜底，权威口径在后端」；format.ts 反向 import 解除（round 收为私有）；影子专用配置 PERIOD_PHASES/STAGE_532/DayType 自 constants.ts 连带退库；全量 grep 无悬空引用，vue-tsc 全绿，3 文件 +12 −456） |

### ARCH-演进-007 — data.sql 建库基线半追平且含弱口令哈希

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进（兼边界） |
| 严重度 | 高 |
| 症状 | `resources/data.sql` 只建 users 表，不含 change1 新增的 user_type/guest_expire_at/merged_into 三列，change3-9 全部表缺失；新环境按注释"首次部署前执行本脚本"得到残缺 schema，须再人工按序补 change1-9（无顺序说明）；且提交 admin 弱口令哈希 |
| 证据 | `apps/zhenxinjian-backend/src/main/resources/data.sql:10-92`（缺列缺表）、`:91-92`（admin/admin123 BCrypt 哈希入库）；对照 `sql/change2_users_delete_flag.sql:15`：data.sql 已是 `delete_flag` 列名，呈"半追平"态——既非 change0 基线也不等于终态 |
| 根因 | 基线脚本未随迁移链同步维护 |
| 影响 | 三处（data.sql / change 链 / 现网）各说各话；新环境搭建即踩坑；弱口令哈希进版本库 |
| 修复建议 | 二选一：data.sql 重生成完整基线（含全部表与列），或明确删除、声明仅走 change 链；口令哈希移出仓库 |
| 状态 | 已消除（B-T10 于 2026-09-12 完成：裁定完整基线 — data.sql 重写为全量基线，建库 + 15 张业务表终态与 change0~9 链一致，全部 CREATE IF NOT EXISTS 幂等；admin/admin123 BCrypt 哈希 INSERT 出库，tools/db/README 新增「管理员初始化」小节（部署者自生成哈希手工 INSERT，口令哈希不进版本库），根 README/03-开发规范/uniapp README 三处同步；演练库验证：基线建库 15 表 → change0~9 全链 RECORDED → 重跑 SKIP，账 10 条/表 16 张，123 测试全绿） |

### ARCH-边界-004 — 定时任务共用单线程池且推送任务同步外呼

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 高 |
| 症状 | 全部 `@Scheduled` 共用默认单线程调度器；推送任务循环内逐条同步调微信订阅消息接口 + 每条 4 次 DB 查询（最多 500 条），微信侧变慢即阻塞同池的统计与清理任务 |
| 证据 | `ZhenxinjianApplication.java:16` 仅 `@EnableScheduling`，全库无 `SchedulingConfigurer`/`ThreadPoolTaskScheduler`；`task/ReminderPushTask.java:77`（fixedDelay=60s）、`:187`（同步外呼）、`:152-172`（每批查询链）；同池任务 `task/StatAggregateTask.java`（01:00）、`task/GuestCleanupTask.java`（03:00） |
| 根因 | 未配置任务线程池；外呼未批量化/异步化 |
| 影响 | 调度饥饿：推送慢则统计与清理延迟 |
| 修复建议 | 配置 `ThreadPoolTaskScheduler`（≥3）；推送批量化（微信订阅消息支持批量）或异步化 |
| 状态 | 已消除（B-T11 于 2026-09-12 完成：ScheduleConfig taskScheduler 池 4 线程承载全部 @Scheduled + reminderPushExecutor 外呼执行器 core2/max4/queue200；微信订阅消息无批量 API 裁定限并发异步——ReminderPushTask 扫描循环改投递执行器，调度线程不占外呼耗时；Executor 按构造参数名注入无歧义；补 ReminderPushTaskTest 8 用例 D4 全分支，131 测试全绿，dev 冒烟启动成功） |

### ARCH-边界-005 — 埋点毒批次无限重试占满队列

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 高 |
| 症状 | 后端任一事件码非法整批拒收，前端失败批次原样保留等下次重试，无毒性隔离；一个非法事件码使该批每 10s 重试，直到队列上限 200 被占满，此后新事件全丢 |
| 证据 | 后端 `service/impl/TrackEventService.java:41-45`（整批拒收）；前端 `utils/track.ts:69-90`（失败原样保留）、`:51-53`（队列上限 200 后丢弃新事件） |
| 根因 | 批处理协议未区分"可重试失败"与"永久失败（毒数据）" |
| 影响 | 队列中毒后埋点静默全丢，且无告警 |
| 修复建议 | 后端返回码区分整批拒绝原因；前端对 40901 类契约错误隔离丢弃毒条目；加队列饱和度告警埋点 |
| 状态 | 已消除 |

### ARCH-演进-003 — 业务常量 ≥4 处物理副本

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进（兼边界） |
| 严重度 | 中 |
| 症状 | 经期上浮、缺口档、活动系数、进度阈值等常量散布于后端枚举、前端 constants、原型 js、invariants 文档四处，靠注释"请同步"约定维系；Redis 连接串也存在双真源 |
| 证据 | `apps/zhenxinjian-backend/.../common/enums/MenstrualPhaseEnum.java:10-19`、`DeficitOptionEnum.java:10-19`、`ActivityLevelEnum.java:13` vs `apps/zhenxinjian-uniapp/src/config/constants.ts:7-12,18,27-43,58-79` vs `MRD-PRD/.../miniprogram/config/constants.js:37-50` vs `docs/knowledge/business-rule/invariants.md:13,18`；`store/food.ts:52` 硬编码 `LOCAL_HOT_CODES` 与后端 `FoodHotConstant.java` 双维护；v1.1 新增：`application.yml:146` JetCache uri 与 `spring.data.redis`（application-dev.yml:13-19）指向同一 Redis、密码分别维护 |
| 根因 | 无跨端常量生成/校验机制 |
| 影响 | 口径变更需人肉同步 4+ 处，已在原型副本上出现冗余 |
| 修复建议 | 以 `openspec/specs` + 后端枚举为唯一真源；前端常量加对照注释与单测锚点；删除原型副本；Redis 连接配置收敛单一来源 |
| 状态 | 已消除 |

### ARCH-演进-004 — 双端演示项目残留

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进 |
| 严重度 | 中 |
| 症状 | 注册/验证码/WS/提示音等演示功能已删页面但 API、类型、白名单、静态资源残留；死常量留存 |
| 证据 | 小程序：`api/auth.ts:17-29,52-54`（getCaptcha/login/register/guestRenew 零调用）、`api/types.ts:13-29,65-69`、`api/request.ts:15`（白名单仍含 /auth/register、/auth/captcha）、`data/foods.ts:237-253`（searchFoods/groupByCategory 死函数）、`config/constants.ts:148-154`（defaultMealType 行为函数混入常量文件）；管理后台：`front/src/api/auth.ts:19-21`、`api/types.ts:35-42`、`api/user.ts:22-24`（getUserById 零调用）、`vite-env.d.ts:16-17`（VITE_WS_*）、`vite.config.ts:27-28`（ws:true 指向已删端点）、`public/notify.wav`、`api/request.ts:54`（ws 注释）；后端：`common/constant/CommonConstant.java:239`（LOGIN_FAIL_MAX 零引用） |
| 根因 | 转型时按页面删除，未做全链路残留清理 |
| 影响 | 误导新成员；白名单含死路由 |
| 修复建议 | 一次性清单式清理（均为纯删除，风险低） |
| 状态 | 已消除（B:09-13 完成：逐项全库检索裁定零引用后纯删除——小程序删 getCaptcha/login/register/guestRenew 四死函数 + LoginRequest/RegisterRequest/CaptchaResult 三死类型 + 白名单三死路由 + data/foods.ts searchFoods/groupByCategory 死函数，defaultMealType 下移唯一消费方 record/add.vue 常量文件回归纯数据；管理后台删 register/RegisterRequest/getUserById + 白名单 /auth/register + VITE_WS_* 死声明 + ws:true 死代理 + notify.wav 死资源 + ws 注释；后端删零引用 LOGIN_FAIL_MAX；三端安全网全绿——uniapp vue-tsc、front vue-tsc+vite build、后端 mvn test 132） |

### ARCH-演进-005 — 构建配置版本错位与 BOM 失效

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进 |
| 严重度 | 中 |
| 症状 | 后端逐 starter 显式写版本架空 parent BOM；两端 TypeScript 工具链错位一代 |
| 证据 | `apps/zhenxinjian-backend/pom.xml:53+`（11 处 `<version>${spring-boot.version}</version>`）、`:141`（hutool-all 全家桶）；`apps/zhenxinjian-uniapp/package.json:45-47`（TS ^4.9.4 / vue-tsc ^1.0.24）vs front 端 TS 5.4 / vue-tsc 2；`front/package.json:29`（overrides 手钉 brace-expansion） |
| 根因 | 各端独立初始化，无统一版本策略 |
| 影响 | 升级时版本冲突面大；BOM 失去意义 |
| 修复建议 | pom 移除冗余显式版本；uniapp TS 工具链评估升级（需验证 vue-tsc 兼容） |
| 状态 | 已消除（B:09-13 完成：三批治理——后端 pom 6 处 starter + spring-boot-maven-plugin 去显式版本回归 parent BOM、lombok 手钉 1.18.48 回归 BOM 1.18.46（annotationProcessorPaths 经 BOM 属性解析）、byte-buddy 显式 compile 依赖裁定死声明删除（代码零引用且无传递来源）、hutool-all 收敛 core/json/extra/captcha 按需四模块；小程序 typescript ^4.9.4→^5.4.5 + vue-tsc ^1.0.24→^2.0.19 对齐 front + @vue/tsconfig ^0.1.3→^0.7.0 消除 TS5102；管理后台 overrides brace-expansion 5.0.8 手钉删除自然解析 2.1.4（≥2.0.2 含 CVE-2025-5889 修复且主版本与 minimatch@9 声明一致）；三端安全网全绿——mvn test 132 + dependency:tree 无意外降级、uniapp type-check 零错误 + build:mp-weixin 绿、front vue-tsc+vite build 绿） |

### ARCH-演进-008 — 埋点事件码双真源（魔法字符串散落）

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进（兼边界） |
| 严重度 | 中 |
| 症状 | 后端 `TrackEventEnum`（22 码）自称唯一真源；前端 ~18 个页面以字面量散落调用 `track('xxx')`，无 TS 常量/类型约束，拼错即触发 ARCH-边界-005 毒批次 |
| 证据 | 后端 `common/enums/TrackEventEnum.java:7-70`；前端样本 `pages/record/index.vue:159`、`pages/food/index.vue:56,98,104` 等 ~30 处字面量 |
| 根因 | 埋点契约无共享定义 |
| 影响 | 事件码拼写错误静默进毒批次链路；新增事件无编译期约束 |
| 修复建议 | 前端建 `config/track-events.ts` 常量表（与后端枚举注释互锚），`track()` 参数收窄为联合类型 |
| 状态 | 已消除（B-T16 于 2026-09-13 完成：新建 `config/track-events.ts` 与后端 TrackEventEnum 22 码一一互锚（键名即枚举名、值即 code、改动须先改后端枚举的同步约定入注释）；`track()` 入参收窄为 TrackEventCode 联合类型，表外字面量编译期报错；16 文件 31 处字面量调用全量改引常量——track.ts 自监控/PV、auth guide+expire 登录与游客、record add/index 增删改、food index/detail/custom-edit、home/mode/cycle/taper/reminder/weight/menstrual/body 各页；vue-tsc 零错误 + build:mp-weixin 构建绿） |

### ARCH-耦合-002 — 小程序 api 层反向依赖 store

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 耦合（兼层次） |
| 严重度 | 中 |
| 症状 | `request.ts` 在 api 层 `useUserStore()` 读写 token/清 userInfo，形成 store→api→store 循环，靠 try/catch 掩盖时序风险；埋点管道另起炉灶与统一请求封装双轨 |
| 证据 | `apps/zhenxinjian-uniapp/src/api/request.ts:7`（import useUserStore）、`:61`（续期写 token）、`:102-104` 与 `:117-119`（清会话）；双轨：`api/track.ts:27` 自带裸 `uni.request`，`:8` 与 `request.ts:9` 各自定义 `BASE_URL`（默认值与超时策略两处维护） |
| 根因 | 401 会话失效处理图省事直接在拦截器改 store |
| 影响 | pinia 初始化时序敏感；api 层不可独立测试；请求策略双份 |
| 修复建议 | 改为事件/回调注入：request 发出 `auth:expired` 事件，由 store 或 App 层订阅处理；track 管道复用统一封装或至少共享配置 |
| 状态 | 已消除（B-T17 于 2026-09-13 完成：request.ts 删除 useUserStore 反向依赖，新增 AuthHooks 回调注入契约（onSessionClear/onTokenRefreshed），401 与游客到期两路径改走 hooks，storage 清理与 reLaunch 保留本层；main.ts 组合根 pinia active 后装配 user store 闭包；store/user.ts 新增 syncToken/clearSession 两最小 action（401 被动路径语义不变）；BASE_URL 双轨收敛为 request.ts 导出单一来源、track.ts 引用，15s/10s 超时差异裁定刻意保留；api 层 grep 零 store 依赖、vue-tsc 零错误、build:mp-weixin 构建绿且 Circular chunk store/user→api/auth→api/request→store/user 警告消除） |

### ARCH-耦合-003 — 管理后台 router↔store↔request 三角循环依赖

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 耦合 |
| 严重度 | 中 |
| 症状 | 路由守卫直用 store，store 反向 import router，request 同时依赖两者；清会话逻辑三处各删一遍 localStorage；守卫绕过 store 直读 token |
| 证据 | `apps/zhenxinjian-front/src/router/index.ts:6,53,69,76`、`store/user.ts:10,24,34`、`api/request.ts:8-9,22-33`（v1.1 复核：:53 直读 `localStorage.getItem('token')`、:69/:76 两处 removeItem 成立） |
| 根因 | 登录态横切逻辑无单一归属 |
| 影响 | 状态真源分裂（localStorage vs store）；循环依赖打包顺序敏感 |
| 修复建议 | 清会话收敛到 store 单一入口；守卫经 store 读登录态；request 不 import store |
| 状态 | 已消除（B-T18 于 2026-09-13 完成：request.ts 删 router+useUserStore 双反向 import，新增 AuthHooks 契约（getToken/onTokenRefreshed/onSessionClear）+ setAuthHooks 注入器，请求拦截 token 改经注入不再直读 localStorage，续期与 401 双路径走 hooks；store/user.ts 收敛 clearSession 单一入口（不含导航）+ syncToken，删 router import；守卫登录态改经 userStore.token、双处 removeItem 改 clearSession；main.ts 组合根 pinia active 后装配（清会话+跳登录在此编排）；MainLayout logout 后补 router.push；依赖收敛 router→store→api 单向无环，npm run build（vue-tsc + vite）全绿，单提交 541e7b4） |

### ARCH-耦合-004 — 后端 controller/task 直依赖 service.impl 双轨并存

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 耦合（兼层次） |
| 严重度 | 中 |
| 症状 | 14 个 controller 直接依赖具体实现类，与 `UserService/StorageService` 接口化风格并存；task 层同样直依赖具体类，且推送任务绕过 service 直注 4 个 Mapper 裸写查询 |
| 证据 | `controller/DietController.java:10`、`FoodController.java:9`、`CyclePlanController.java:7`、`AdminStatsController.java:8` 等 14 文件 `import cn.zhenxinjian.service.impl.*`；v1.1 新增：`task/ReminderPushTask.java:16,68`（直依赖 `service.impl.ReminderService`）、`:60-66`（直注 UserReminder/ReminderSendLog/DietRecord/User 四个 Mapper，:152-172 裸写判定查询）、`task/StatAggregateTask.java:3,20` |
| 根因 | 增量开发未定统一约定；推送判定链图省事写在 task |
| 影响 | 替换实现/加装饰器困难；同一业务数据访问分裂在两层 |
| 修复建议 | 二选一统一依赖风格并成文；推送判定查询下沉到 ReminderService |
| 状态 | 已消除（T19 于 2026-09-13 完成：ReminderPushTask 四个 Mapper 裸查询全部下沉——ReminderService 新增 scanDueReminders（到点扫描带 LIMIT）/hasSuccessPushToday（I12 单次）、DietRecordService 新增 hasRecord（已记录不重复）、用户查询改经 UserService.getById，任务只编排与外呼；测试安全网同步切换（ReminderPushTaskTest 8 用例改桩 service 层 + 新增 ReminderServiceTest 2 用例 + DietRecordServiceTest 补 hasRecord 2 用例），136 用例全绿；依赖风格裁定成文——开发规范 §2.1 与 java/standard.md §3 增补「实现类直依赖为主 + 接口化豁免清单」（多实现/替换点与跨层稳定契约五接口豁免）与「controller/task 禁止直依赖 Mapper」规约（豁免 FoodLibraryInitializer 启动引导、GuestCleanupBatchExecutor 批级事务主体）；存量接口化改造按裁定另立任务，单提交 81cba5f） |

### ARCH-耦合-005 — 小程序页面间 URL 传复杂状态

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 耦合 |
| 严重度 | 中 |
| 症状 | 饮食记录编辑回显把 name/三宏/kcal/remark 全量 encodeURIComponent 拼 URL，目标页逐项解析，双端手工同步 |
| 证据 | `pages/record/index.vue:137-142`（拼接）↔ `pages/record/add.vue:103-146`（解析） |
| 根因 | 无编辑态状态容器 |
| 影响 | URL 长度/编码脆弱；字段增改须双端同步 |
| 修复建议 | 改传记录 id 由 add 页拉取，或经 diet store 暂存编辑态 |
| 状态 | 已消除（T20 于 2026-09-13 完成：裁定「URL 仅带 id + add 页从 diet store 当日分组按 id 回显」——dayData.meals[].records 本就缓存完整 DietRecordVO，编辑入口仅在列表页数据已在内存，不新增后端单条接口也不设独立编辑态容器；record/index.vue handleEdit 双分支全量 encodeURIComponent 拼接收敛单一 editId，add.vue 删逐项 decode 改 store 查找回显（中文备注不再经 URL 编解码，深链/日期切换找不到时 toast 引导返回）；encode/decodeURIComponent 全库检索清零，vue-tsc 零错误，单提交 2724839） |

### ARCH-内聚-004 — DietRecordService 超大多职责

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚 |
| 严重度 | 中 |
| 症状 | 446 行，承担 CRUD + 汇总 + 守恒校验 + 目标分发，跨调 Cycle/Taper 两个服务，service 互调网初现；另散落 9 处小重复（`round1` 三处、`operator()` 六处、CORS origin 解析三处） |
| 证据 | `service/impl/DietRecordService.java:49`（446 行）、`:245,250,273`（跨调）；`round1`：`BodyCalcService.java:99`、`DietRecordService.java:424,429`、`Taper532Service.java:171`；`operator()`：`BodyProfileService.java:173` 等 6 处；CORS：`SecurityConfig.java:95-106`、`SecurityStartupChecker.java:64-66`、`WebSocketConfig.java:76-77` |
| 根因 | 饮食记录是业务枢纽，职责持续堆积 |
| 影响 | 改动影响面大；口径重复 |
| 修复建议 | 拆出汇总/校验职责；`round1`/`operator()` 收敛到公共工具 |
| 状态 | 已消除（T21 于 2026-09-13 完成，f6c43c4 批 1 + 641034b 批 2 双提交——批 1 三处小重复收敛公共工具：`round1` 三处私有副本（BodyCalc/Taper532/DietRecord，BigDecimal/double 双 overload）→ `common/utils/Numbers.round1` 单一真源，`operator()` 六处 `"user:"+userId` 副本（BodyProfile/CustomFood/CyclePlan/DietRecord/Menstrual/Weight）→ `common/utils/Operators.user`（AdminFoodService 无参 operator() 管理员用户名不同语义裁定保留），CORS origin env 逗号分隔扁平化解析双份 → `common/utils/CorsOrigins.parse`（过滤空白与星号语义保留调用方本层）；批 2 拆汇总职责：新建 `DietSummaryService` 承接 `summary()` 整方法（532 推进口径/碳循环日型/经期上浮叠加/未建档与无周期空态口径不变），`DietRecordService` 删 UserBodyMapper/CyclePlanService/Taper532Service 三依赖与 rate/toDouble 私有助手回归 CRUD+按日分组+hasRecord 单一职责，跨调网 Cycle/Taper 改由 DietSummaryService 单向依赖零回边 DAG 无环，DietController 注入委派 `/diet/summary`；守恒校验已由 B-T03 `MacroConsistencyValidator` 收口无需再拆；DietRecordServiceTest 15→11 + 新增 DietSummaryServiceTest 5 用例原样迁移，136 用例全绿零行为变更） |

### ARCH-内聚-005 — 小程序空态/模式判断多套口径

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚 |
| 严重度 | 中 |
| 症状 | 空态三分支判定两处各写一遍且条件已漂移；"当前模式"判断存在 summary.mode 与 profile.mode 两个事实源 |
| 证据 | 空态：`pages/home/index.vue:71-80` vs `pages/record/index.vue:84-86`（home 多一个 loaded 前置）；模式：`home/index.vue:43`、`record/index.vue:75`（summary.mode===2）vs `mine/index.vue:50`、`mode/select.vue:26`（bodyStore.profile?.mode） |
| 根因 | 模式真源未定义（档案 vs 当日汇总） |
| 影响 | 两源不同步时各页显示不一致 |
| 修复建议 | 在 store 层定义单一 `currentMode` 口径（建议以档案为准），页面统一引用 |
| 状态 | 未清除 |

### ARCH-内聚-006 — 管理后台列表页模式三处重复

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚 |
| 严重度 | 中 |
| 症状 | "查询重置页码 + 分页回调 + toolbar 布局"三个列表页逐字重复；删后回退页码逻辑两处重复；角色/状态字典在 users 页内声明 4 次 |
| 证据 | `apps/zhenxinjian-front/src/view/users/index.vue:44-48,195-197,201-209,226-233,303-312,331-332,392-412`、`foods/index.vue:178-186,314-334`、`diet-records/index.vue:54-62,141-160` |
| 根因 | 无列表页骨架组件与字典层 |
| 影响 | 每个新列表页复制 ~60 行样板 |
| 修复建议 | 抽 `usePageQuery` composable + 通用分页条组件 + `constants/` 字典层 |
| 状态 | 未清除 |

### ARCH-内聚-007 — 小程序微信登录流程双份复制

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 内聚（兼模块化） |
| 严重度 | 中 |
| 症状 | `uni.login → code → loginByWechat` 整段在两个页面逐行复制 |
| 证据 | `pages/auth/guide.vue:33-55` 与 `pages/auth/expire.vue:31-53` |
| 根因 | 登录能力未下沉 |
| 影响 | 登录链路变更须双改 |
| 修复建议 | 下沉到 `store/user.ts` 的 `loginByWechat()` 方法 |
| 状态 | 未清除 |

### ARCH-层次-002 — 文档三套并行真源不明

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 层次（兼模块化） |
| 严重度 | 中 |
| 症状 | `doscFile/`（目录名拼写错误）+ `docs/knowledge/` + wiki（gitignore 排除独立仓库）三套文档并行且互引 |
| 证据 | `doscFile/01-项目介绍.md` 等 7 文件；`docs/knowledge/business-rule/invariants.md:21` 引用 `doscFile/projectFile/04-食物库数据字典.md`；`zhenxinjian.wiki/` 含同名第二份 |
| 根因 | 文档随不同阶段产物累积，无归属约定 |
| 影响 | 更新不知改哪份；新人困惑 |
| 修复建议 | 修正拼写并明确 `docs/` 为唯一真源；wiki 定位为只读镜像或删除 |
| 状态 | 未清除 |

### ARCH-边界-001 — 自定义食物列表无上限查询

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 中 |
| 症状 | `listMine` 纯 selectList 无 LIMIT 无分页，用户可无限积累（全库唯一真实无上限查询；`DietRecordService.java:153-158` 单日查询天然有限仅低风险） |
| 证据 | `service/impl/CustomFoodService.java:86-93`（v1.1 复核成立：`selectList(eq(source).eq(userId).orderByDesc(id))` 无 LIMIT） |
| 根因 | 初期假设数量少未设防 |
| 影响 | 长尾用户列表膨胀；违反 AGENTS.md "无上限 selectList" Never 条款 |
| 修复建议 | 加 LIMIT（如 200）或分页 |
| 状态 | 未清除 |

### ARCH-边界-006 — 提醒推送分钟精确匹配漏发无补偿

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 中 |
| 症状 | 以 `HH:mm` 字符串全等判定命中；扫描被延迟错过或应用重启跨过该分钟，该分钟所有提醒永久丢失，无补偿机制 |
| 证据 | `task/ReminderPushTask.java:90-101,127-134`（分钟全等匹配）；代码注释自述"失败仅写日志不重试"，无漏发补偿表/窗口扫描 |
| 根因 | 推送按"恰好那一分钟"设计，未考虑调度抖动 |
| 影响 | 用户提醒静默丢失，与"到点未记录才推送"承诺不符 |
| 修复建议 | 改窗口匹配（如命中 ±2 分钟未发即补）+ 发送日志去重；或由调度器精确触发 |
| 状态 | 未清除 |

### ARCH-边界-007 — JWT 弱默认密钥仅靠 prod 门禁

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 中 |
| 症状 | 默认 JWT secret 明文入库，启动拦截仅认 `prod` profile，任何非 prod 命名（production/pre 拼写差异）即带弱密钥上线 |
| 证据 | `resources/application.yml:56`（`zhenxinjian-jwt-secret-key-change-in-production-wanglx`）；`config/SecurityStartupChecker.java:24,50`（仅 prod 拒绝）；缓解项：MinIO 弱默认（`application.yml:119-120`）默认关闭 |
| 根因 | 安全门禁按 profile 名而非部署事实 |
| 影响 | 误配环境即弱密钥生产化 |
| 修复建议 | 改为检测密钥内容本身（含"change-in"即拒）或强制环境变量注入；默认值改为占位符 |
| 状态 | 未清除 |

### ARCH-模块化-001 — 小程序页面超大 + 样式无全局抽象

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 模块化 |
| 严重度 | 低 |
| 症状 | 7 个页面 >400 行（最大 687）；`.panel/.btn-primary/.modal` 等样式在 ≥8 个页面各写一份 |
| 证据 | `pages/record/index.vue`（687）、`home/index.vue`（635）、`record/add.vue`（569）、`mine/index.vue`（473）、`reminder/index.vue`（460）、`food/index.vue`（445）、`weight/index.vue`（440） |
| 根因 | 样式变量只到色板层，无组件级样式沉淀 |
| 影响 | 单页维护成本高；视觉口径靠复制维系 |
| 修复建议 | 提取全局公共样式类（uni.scss 或 app.scss）；超大页拆分子组件 |
| 状态 | 未清除 |

### ARCH-模块化-002 — 原型小程序工程入库未清退

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 模块化（兼演进） |
| 严重度 | 低 |
| 症状 | 原生小程序原型 85 文件 + ~794KB 二进制（5 docx/2 xlsx/1 pdf）全部入库，已被三端取代；目录名带 "zip" 实为解压目录 |
| 证据 | `MRD-PRD/小程序工程骨架（完整版·zip）/miniprogram/`（19 页面 ×4 文件 + 3 云函数）；`git ls-files MRD-PRD` 共 95 文件；`.gitignore:95` 的 `*.zip` 挡不住解压目录 |
| 根因 | 需求阶段产物直接入库 |
| 影响 | 仓库膨胀；原型口径成为常量漂移温床（见 ARCH-演进-003） |
| 修复建议 | 移出版本库（release 附件或 wiki），仅保留 PRD 文档 |
| 状态 | 未清除 |

### ARCH-模块化-003 — 管理后台视图三职责混居

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 模块化 |
| 严重度 | 低 |
| 症状 | users 视图 417 行混合表格+编辑弹窗+详情抽屉；foods 视图 340 行表格+表单弹窗同文件；dashboard 全量引入 echarts |
| 证据 | `apps/zhenxinjian-front/src/view/users/index.vue:1-417`、`foods/index.vue:1-340`、`dashboard/index.vue:7`（`import * as echarts`） |
| 根因 | 单页快速实现未拆分 |
| 影响 | 复用受限；包体积偏大 |
| 修复建议 | 弹窗/抽屉拆为独立组件；echarts 按需引入 |
| 状态 | 未清除 |

### ARCH-边界-002 — 管理后台 Token 存 localStorage + 子串匹配白名单

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 低 |
| 症状 | Token 存 localStorage，XSS 一旦存在即可窃取（管理端掌握用户 CRUD 风险放大）；`AUTH_SKIP_URLS` 用 `includes` 子串匹配，未来含 `/auth/login` 子串的 URL 会被误跳过 |
| 证据 | `apps/zhenxinjian-front/src/store/user.ts:24`、`api/request.ts:17,38,40-43`；缓解项：后端 `@PreAuthorize hasRole('ADMIN')` 兜底（`AdminUserController.java:28`），无 v-html/eval 注入面 |
| 根因 | 后台安全基线未专项评审 |
| 影响 | 纵深防御薄弱（当前无 XSS 入口故实际风险低） |
| 修复建议 | 白名单改精确/前缀匹配；评估 sessionStorage + 短会话 |
| 状态 | 未清除 |

### ARCH-边界-003 — 埋点接口匿名开放无限流

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 低 |
| 症状 | `/track/events` permitAll，仅校验单批 ≤50 与事件白名单，无频率限制，可刷量写库 |
| 证据 | `resources/application.yml:94`、`service/impl/TrackEventService.java:38-59` |
| 根因 | 埋点优先可用性未设防 |
| 影响 | 统计污染 + 写库压力 |
| 修复建议 | 加按用户/IP 的简单限流（Redis 计数） |
| 状态 | 未清除 |

### ARCH-边界-008 — 小程序发布就绪性缺口（隐私/升级）

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 边界 |
| 严重度 | 低 |
| 症状 | `manifest.json` 微信 appid 为空；无隐私协议弹窗流程（仅"我的"页静态文案）、无 `uni.getUpdateManager` 版本升级检查，微信平台合规与灰度升级两个必备项缺失 |
| 证据 | `apps/zhenxinjian-uniapp/src/manifest.json:29`（appid 空串，真值散在后端 application-dev.yml 默认值）；`src/App.vue:10-17`（onLaunch 仅埋点定时器+token 判断）；`pages/mine/index.vue:128-129`（静态隐私文案） |
| 根因 | 一期以开发态验收，未走发布态检查单 |
| 影响 | 提审/合规风险；线上用户无法感知新版本 |
| 修复建议 | appid 经环境变量登记；补 `__usePrivacyCheck__` 流程与 updateManager 检查 |
| 状态 | 未清除 |

### ARCH-演进-006 — tools 本机路径耦合与临时残渣

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进（兼边界） |
| 严重度 | 低 |
| 症状 | 工具脚本硬编码本机路径换机即废；示例模板被 gitignore 误伤；根目录 9 个 tmp-* AI 会话残渣（已忽略未清理）；`.codegraph/` 仅空壳 .gitignore 入库 |
| 证据 | `tools/env/jdk25.cmd:4-5`、`mvn25.cmd:6`（`D:\App\java\25`）、`tools/README.md:18-22`；`.gitignore:111`（`tools/**/*.local.*` 误伤 `db.local.properties.example`）；`.gitignore:77-79`（tmp 规则）；`.codegraph/.gitignore` |
| 根因 | 工具脚本个人环境起步 |
| 影响 | 新成员上手门槛；凭据模板拿不到样例（凭据本身未泄漏，`SqlRunner.java:46-53` 走环境变量/本地文件） |
| 修复建议 | 路径改环境变量/相对定位；`!*.example` 豁免规则；物理清理 tmp 文件 |
| 状态 | 未清除 |

### ARCH-演进-009 — 微信 token 配置内存态

| 字段 | 内容 |
| --- | --- |
| 级别 | ARCH |
| 类别 | 演进 |
| 严重度 | 低 |
| 症状 | `WxMaDefaultConfigImpl` 将 access_token 存内存，多实例部署互相顶替 token |
| 证据 | `config/WxMaConfiguration.java:25`（注释声明一期单实例） |
| 根因 | 一期单实例假设下的简化 |
| 影响 | 扩容即踩坑（与 ShedLock 同类未设防扩展点） |
| 修复建议 | 扩容前切换 `WxMaRedisConfigImpl` 类 Redis 实现；在 ADR 中登记该约束 |
| 状态 | 未清除 |

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-11 | `ee2a2fa927a5f66486b85a42a2a40e056ad60903` | 初版：全项目 ARCH 级盘点，四路并行侦察，检出 26 条（高 7 / 中 13 / 低 6） |
| v1.1 | 2026-09-11 | `ee2a2fa927a5f66486b85a42a2a40e056ad60903`（HEAD 未变） | 盲区补扫（Migrator 编排族/定时任务层/引导配置层/resources/小程序全局层/埋点链路）新增 10 条（耦合-006、层次-003、演进-007/008/009、边界-004~008）；复核 6 条高危证据全部成立；修订 5 条（耦合-002/004、内聚-002、演进-002/003 补证据与纠表述）。共 36 条（高 12 / 中 16 / 低 8） |
| v1.2 | 2026-09-11 | —（未提交） | B-T01 完成，ARCH-耦合-001 置"已消除"（SessionEvictor 抽象 + ws 全链路删除）。余 35 条未清除 |
| v1.3 | 2026-09-12 | —（未提交） | B-T02 完成，ARCH-耦合-006 置"已消除"（GuestMigrationOrchestrator 编排 + Ordered 契约 + 缓存失效 afterCommit 后置）。余 34 条未清除 |
| v1.4 | 2026-09-12 | —（未提交） | B-T03 完成，ARCH-内聚-001 置"已消除"（MacroConsistencyValidator 后端单一真源 + 前端 utils/validate.ts 收敛；5000/10000 裁定为不同场景口径）。余 33 条未清除 |
| v1.5 | 2026-09-12 | —（未提交） | B-T04 完成，ARCH-内聚-002 置"已消除"（record/index.vue 手写副本替换为 utils/macro.ts 单一真源调用）。余 32 条未清除 |
| v1.6 | 2026-09-12 | —（未提交） | B-T05 完成，ARCH-内聚-003 置"已消除"（.trae/ 单一真源 + sync-ide-skills.ps1 分发脚本 + 五副本目录退库 120 文件，字节级回归 0 差异）。余 31 条未清除 |
| v1.7 | 2026-09-12 | —（未提交） | B-T06 完成，ARCH-层次-001 置"已消除"（补 store/taper、store/reminder 两 store，五处页面直调改经 store，登出清理收敛 userStore 统一编排七大 store，cycleStore.switchMode 收敛模式切换；pages/components 对 @/api/* 仅剩 type-only import）。余 30 条未清除 |
| v1.8 | 2026-09-12 | —（未提交） | B-T07 完成，ARCH-层次-003 置"已消除"（GuestCleanupBatchExecutor 独立 Bean 批级独立事务，任务方法不再持大事务，批级 size/costMs 日志，补 5 测试用例）。余 29 条未清除 |
| v1.9 | 2026-09-12 | —（未提交） | B-T08 完成，ARCH-演进-001 置"已消除"（schema_migrations 版本表 + SqlRunner 自动记账/SKIP/失败即终止 + 9 脚本幂等守卫 + 存量回填脚本 + 全新环境引导成文，五场景演练全绿）。余 28 条未清除 |
| v1.10 | 2026-09-12 | —（未提交） | B-T09 完成，ARCH-演进-002 置"已消除"（calculator.ts 443→66 行仅留录入页预览三函数并标注「仅展示兜底」，isPlateau 口径分叉雷清除，format.ts 反向 import 解除，影子专用配置 PERIOD_PHASES/STAGE_532/DayType 连带退库，vue-tsc 全绿）。余 27 条未清除 |
| v1.11 | 2026-09-12 | —（未提交） | B-T10 完成，ARCH-演进-007 置"已消除"（裁定完整基线：data.sql 重写为建库 + 15 张业务表终态全量基线、全 CREATE IF NOT EXISTS 幂等；admin 弱口令哈希出库 + 管理员初始化成文；演练库全链 RECORDED 0~9/重跑 SKIP/账 10 条/表 16 张，123 测试全绿）。余 26 条未清除 |
| v1.12 | 2026-09-12 | —（未提交） | B-T11 完成，ARCH-边界-004 置"已消除"（ScheduleConfig 多线程调度池池 4 线程 + reminderPushExecutor 限并发异步外呼；微信订阅消息无批量 API 裁定异步化；ReminderPushTaskTest 8 用例 D4 全分支，131 测试全绿，dev 冒烟启动成功）。余 25 条未清除 |
| v1.13 | 2026-09-12 | —（未提交） | B-T12 完成，ARCH-边界-005 置"已消除"（裁定「恒 200 + 响应 data 携带剔除码列表」契约替代 40901 整批拒收：TrackEventService 逐条白名单校验毒条目剔除/合法落库/去重保序返回；TrackEventEnum 新增 track_queue_saturated；前端成功整批移除毒条目不再无限重试 + 队列 ≥80% 饱和告警一次回落复位；TrackEventServiceTest 5 用例新契约，132 测试全绿、vue-tsc 通过；openspec spec 同步）。余 24 条未清除 |
| v1.14 | 2026-09-12 | —（未提交） | B-T13 完成，ARCH-演进-003 置"已消除"（全量比对裁定零漂移；constants.ts 四块锚点注释 + MenstrualPhaseEnum 失效引用清理；application-dev.yml.example jetcache uri 改 ${spring.data.redis.*} 单一属性源拼接；原型副本删除并入 B-T30 协同；132 测试全绿、vue-tsc 通过、dev 冒烟验证空密码拼接 uri 零异常）。余 23 条未清除 |
| v1.15 | 2026-09-13 | —（未提交） | B-T14 完成，ARCH-演进-004 置"已消除"（逐项全库检索裁定零引用后纯删除：小程序四死函数/三死类型/白名单三死路由/foods.ts 死函数/defaultMealType 下移消费方；管理后台 register/RegisterRequest/getUserById/白名单死路由/VITE_WS_*/ws:true/notify.wav/ws 注释；后端 LOGIN_FAIL_MAX；三端安全网全绿——uniapp vue-tsc、front vue-tsc+vite build、mvn test 132）。余 22 条未清除 |
| v1.16 | 2026-09-13 | —（未提交） | B-T15 完成，ARCH-演进-005 置"已消除"（三批治理：后端 pom 冗余显式版本回归 parent BOM + lombok 回归 BOM 1.18.46 + byte-buddy 死声明删除 + hutool-all 收敛按需四模块；小程序 TS 5.4.5/vue-tsc 2/@vue/tsconfig 0.7 工具链对齐 front；管理后台 overrides brace-expansion 手钉删除自然解析 2.1.4；三端安全网全绿）。余 21 条未清除 |
| v1.17 | 2026-09-13 | —（未提交） | B-T16 完成，ARCH-演进-008 置"已消除"（config/track-events.ts 常量表与后端 TrackEventEnum 22 码互锚 + track() 收窄 TrackEventCode 联合类型编译期防拼写漂移 + 16 文件 31 处字面量全量改引常量；vue-tsc 零错误 + build:mp-weixin 构建绿）。余 20 条未清除 |
| v1.18 | 2026-09-13 | —（未提交） | B-T17 完成，ARCH-耦合-002 置"已消除"（request.ts 去 useUserStore 反向依赖 + AuthHooks 回调注入契约（onSessionClear/onTokenRefreshed）+ main.ts 组合根 pinia active 后装配 + store/user.ts 新增 syncToken/clearSession 最小 action（401 被动路径语义不变）；BASE_URL 双轨收敛 request.ts 单一来源、track.ts 引用，15s/10s 超时差异裁定保留；api 层 grep 零 store 依赖、vue-tsc 零错误、build:mp-weixin 绿且 Circular chunk store/user→api/auth→api/request→store/user 警告消除）。余 19 条未清除 |
| v1.19 | 2026-09-13 | —（未提交） | B-T18 完成，ARCH-耦合-003 置"已消除"（管理后台三角循环解耦：request.ts 删 router+useUserStore 双反向 import + AuthHooks 契约（getToken/onTokenRefreshed/onSessionClear）+ setAuthHooks 注入器，token 改经注入不再直读 localStorage；store/user.ts 收敛 clearSession 单一入口 + syncToken，删 router import；守卫登录态改经 userStore；main.ts 组合根装配；依赖收敛 router→store→api 单向无环，vue-tsc + vite build 全绿）。余 18 条未清除 |
| v1.20 | 2026-09-13 | —（未提交） | B-T19 完成，ARCH-耦合-004 置"已消除"（ReminderPushTask 四个 Mapper 裸查询全部下沉——ReminderService.scanDueReminders/hasSuccessPushToday、DietRecordService.hasRecord、UserService.getById，任务只编排与外呼；测试安全网同步切换，136 用例全绿；依赖风格裁定成文——开发规范 §2.1 与 java/standard.md §3 增补「实现类直依赖为主 + 接口化豁免清单」与「controller/task 禁止直依赖 Mapper」规约及两基础设施豁免）。余 17 条未清除 |
| v1.21 | 2026-09-13 | —（未提交） | B-T20 完成，ARCH-耦合-005 置"已消除"（饮食编辑态 URL 全量 encodeURIComponent 传参改「URL 仅带 id + add 页从 diet store 当日分组按 id 回显」，中文备注不再经 URL 编解码，深链找不到 toast 引导返回；encode/decodeURIComponent 检索清零，vue-tsc 零错误）。余 16 条未清除 |

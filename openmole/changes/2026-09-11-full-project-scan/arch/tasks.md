# Tasks — 2026-09-11-full-project-scan（ARCH 架构级）

- **级别**：ARCH（架构级）
- **依据坏味道版本**：badsmells.md v1.1（提交版本 `ee2a2fa927a5f66486b85a42a2a40e056ad60903`）
- **生成方式**：mole:plan 全量生成（36 条未清除 → 36 个任务）
- **执行粒度**：每次 `mole:apply` 仅处理一个未完成任务
- **回归命令基线**：后端 `set JAVA_HOME=D:\App\java\25 && mvn -q test`（apps/zhenxinjian-backend）；小程序 `npm run check`（apps/zhenxinjian-uniapp，vue-tsc）；管理后台 `npm run build`（apps/zhenxinjian-front，vue-tsc + vite）

---

## §3 任务清单

| 任务 | 追溯 | 标题 | 严重度 | 状态 |
| --- | --- | --- | --- | --- |
| B-T01 | ARCH-耦合-001 | 剥离 WebSocket 演示板（先抽象 SessionEvictor 再删包） | 高 | 已完成 |
| B-T02 | ARCH-耦合-006 | 引入 GuestMigrationOrchestrator 编排族并后置缓存失效 | 高 | 已完成 |
| B-T03 | ARCH-内聚-001 | 能量守恒 ±10% 校验跨端收敛单一真源 | 高 | 已完成 |
| B-T04 | ARCH-内聚-002 | 三宏进度展示收敛到 utils/macro.ts | 高 | 已完成 |
| B-T05 | ARCH-内聚-003 | AI skills 资产单一真源、冗余副本退库 | 高 | 已完成 |
| B-T06 | ARCH-层次-001 | 小程序 pages→store→api 分层收敛并补 taper store | 高 | 已完成 |
| B-T07 | ARCH-层次-003 | 游客清理任务改批级独立事务 | 高 | 已完成 |
| B-T08 | ARCH-演进-001 | SQL 迁移版本化（版本表 + 幂等守卫） | 高 | 已完成 |
| B-T09 | ARCH-演进-002 | 删除前端影子算法库（isPlateau 优先） | 高 | 已完成 |
| B-T10 | ARCH-演进-007 | data.sql 基线整改 + 弱口令哈希出库 | 高 | 已完成 |
| B-T11 | ARCH-边界-004 | 定时任务线程池配置 + 推送外呼批量化 | 高 | 已完成 |
| B-T12 | ARCH-边界-005 | 埋点毒批次毒性隔离与饱和告警 | 高 | 已完成 |
| B-T13 | ARCH-演进-003 | 业务常量单一真源 + Redis 连接配置收敛 | 中 | 已完成 |
| B-T14 | ARCH-演进-004 | 双端演示残留清单式清理 | 中 | 已完成 |
| B-T15 | ARCH-演进-005 | 构建配置版本治理（BOM 回归 + TS 工具链对齐） | 中 | 已完成 |
| B-T16 | ARCH-演进-008 | 埋点事件码前端常量表 + track() 类型收窄 | 中 | 已完成 |
| B-T17 | ARCH-耦合-002 | 小程序 api 层去 store 依赖（事件化 401 处理） | 中 | 已完成 |
| B-T18 | ARCH-耦合-003 | 管理后台 router↔store↔request 循环解耦 | 中 | 已完成 |
| B-T19 | ARCH-耦合-004 | 后端依赖风格统一 + 推送判定下沉 ReminderService | 中 | 已完成 |
| B-T20 | ARCH-耦合-005 | 饮食编辑态改 id 拉取或 store 暂存 | 中 | 已完成 |
| B-T21 | ARCH-内聚-004 | DietRecordService 职责拆分 + 小重复收敛 | 中 | 已完成 |
| B-T22 | ARCH-内聚-005 | 小程序空态/当前模式单一口径 | 中 | 已完成 |
| B-T23 | ARCH-内聚-006 | 管理后台列表页骨架（usePageQuery + 字典层） | 中 | 已完成 |
| B-T24 | ARCH-内聚-007 | 微信登录流程下沉 store/user.ts | 中 | 已完成 |
| B-T25 | ARCH-层次-002 | 文档归属收敛（docs/ 唯一真源） | 中 | 已完成 |
| B-T26 | ARCH-边界-001 | 自定义食物列表加上限/分页 | 中 | 未开始 |
| B-T27 | ARCH-边界-006 | 提醒推送窗口匹配 + 漏发补偿 | 中 | 未开始 |
| B-T28 | ARCH-边界-007 | JWT 密钥门禁改内容检测 | 中 | 未开始 |
| B-T29 | ARCH-模块化-001 | 小程序全局样式抽象 + 超大页拆分 | 低 | 未开始 |
| B-T30 | ARCH-模块化-002 | 原型小程序工程移出版本库 | 低 | 未开始 |
| B-T31 | ARCH-模块化-003 | 管理后台视图组件拆分 + echarts 按需 | 低 | 未开始 |
| B-T32 | ARCH-边界-002 | 白名单精确匹配 + Token 存储评估 | 低 | 未开始 |
| B-T33 | ARCH-边界-003 | 埋点接口限流 | 低 | 未开始 |
| B-T34 | ARCH-边界-008 | 小程序发布就绪性补齐（隐私/升级） | 低 | 未开始 |
| B-T35 | ARCH-演进-006 | tools 路径解耦 + 临时残渣清理 | 低 | 未开始 |
| B-T36 | ARCH-演进-009 | 微信 token 内存态约束登记 ADR | 低 | 未开始 |

状态说明：**未开始** / 进行中 / 已完成。共 36 个任务（高 12 / 中 16 / 低 8）。

执行顺序备注：

- B-T08（迁移版本化）宜先于 B-T10（data.sql 基线整改），后者产出的基线脚本需纳入版本链。
- B-T11（任务线程池）与 B-T27（推送补偿）同触 `ReminderPushTask.java`，建议先后串行避免冲突。
- B-T16（事件码常量表）先于或伴随 B-T12（毒批次隔离）执行收益最大，但二者无硬依赖。
- B-T14（演示残留清理）执行前先完成 B-T01（WebSocket 剥离），避免清理清单重叠。

### B-T01 — 剥离 WebSocket 演示板（先抽象 SessionEvictor 再删包）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-耦合-001（耦合，高） |
| 目标 | 删除演示 WebSocket 全链路（包/配置/开关），`kickUser` 能力经 `SessionEvictor` 抽象解耦后三个依赖点不再感知 ws 模块 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `DemoWebSocketHandler.java:30`、`DemoWebSocketInterceptor.java:33`、`WebSocketSessionRegistry.java:20`、`config/WebSocketConfig.java:29/:71-75/:85-90`、`application.yml:72` 与三个依赖点（`UserServiceImpl.java:55,87,242`、`WechatAuthServiceImpl.java:53,76,128,286`、`GuestCleanupTask.java:36,65`）现状未变。
2. 影响分析：列出所有 `kickUser` 调用语义（登录顶号 / 游客失效清理），确认空实现（no-op）在业务上可接受或需保留内存会话清理等价物。
3. 测试安全网：为登录顶号与游客清理路径补集成/单测（至少覆盖 `UserServiceImpl` 与 `GuestCleanupTask` 的会话失效分支）。
4. 选择架构模式：端口/适配器 — 新增 `SessionEvictor` 接口 + no-op（或简化内存实现）适配器，替换全部 `WebSocketSessionRegistry` 注入。
5. 迁移计划：先替换依赖（可独立提交）→ 再删 `websocket/` 包、`WebSocketConfig`、`application.yml` 开关与 Origin 白名单 → 验证无残余 import。
6. 增量执行：按迁移计划分两阶段提交，每阶段保持编译与测试绿。
7. 回归测绿：后端 `mvn -q test` 全绿；启动验证无 ws 端点注册日志。
8. 用户确认：展示删除清单与 diff，获确认后收尾（写操作门禁）。

### B-T02 — 引入 GuestMigrationOrchestrator 编排族并后置缓存失效

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-耦合-006（耦合，高） |
| 目标 | 迁移驱动收敛为单一编排入口，执行顺序契约化（`Ordered`），缓存失效移到合并提交之后 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对双调用点（`WechatAuthServiceImpl.java:275-278`、`GuestCleanupTask.java:46,59-61`）与顺序敏感证据（`UserRecordMigrator.java:23-29` 即时 evict vs `WechatAuthServiceImpl.java:280-282` 后置合并标记）。
2. 影响分析：枚举 6 个 `GuestDataMigrator` 实现，梳理彼此数据依赖与幂等性，确定合理 order 值。
3. 测试安全网：为迁移全链路补测试（游客数据 → 合并 → 缓存一致性场景，含并发读时序用例）。
4. 选择架构模式：编排者（Orchestrator）— 新建 `GuestMigrationOrchestrator`，实现按 `Ordered` 排序，事务边界内聚。
5. 迁移计划：建编排者并单测 → 切换调用点一（登录）→ 切换调用点二（清理任务）→ 将 `UserRecordMigrator` 的 evict 移到合并提交后。
6. 增量执行：按上述顺序逐步替换，旧驱动循环保留至双调用点切换完毕再删除。
7. 回归测绿：后端 `mvn -q test` 全绿；人工演练一次游客登录合并流程。
8. 用户确认：展示编排者与调用点收敛 diff，获确认后收尾（写操作门禁）。

### B-T03 — 能量守恒 ±10% 校验跨端收敛单一真源

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-001（内聚，高） |
| 目标 | 4/4/9 ±10% 校验后端收敛为共享校验器；前端收敛到 `utils/validate.ts` 并以后端为准，消除上限值分裂（5000 vs 10000） |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对后端三处（`DietRecordService.java:61-65,333-341`、`CustomFoodService.java:36-40,157-165`、`AdminFoodService.java:40-43,142-150`）与前端三处（`record/add.vue:88-95`、`food/custom-edit.vue:117-129`、`food/detail.vue:16-17`）。
2. 影响分析：裁定权威口径（上限值 5000/10000 与 100/900 的取舍依据，对照 PRD/invariants），明确"以后端为准"的落地方式。
3. 测试安全网：为后端三处校验补边界用例（±10% 临界、上限临界），固化裁定后的口径。
4. 选择架构模式：共享内核 — 后端抽 `MacroConsistencyValidator`（或 static 工具）；前端统一进 `utils/validate.ts`。
5. 迁移计划：后端收敛（单点改、三处引用）→ 前端收敛（删页面私有副本）→ 上限常量对齐。
6. 增量执行：后端、前端分两批提交；每批保持各自类型检查/测试绿。
7. 回归测绿：后端 `mvn -q test` + 小程序 `npm run check` 全绿；手工过一遍饮食记录/自定义食物/管理后台录入三条路径。
8. 用户确认：展示口径裁定结论与收敛 diff，获确认后收尾（写操作门禁）。

### B-T04 — 三宏进度展示收敛到 utils/macro.ts

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-002（内聚，高） |
| 目标 | `pages/record/index.vue` 手写进度/超标建议映射改为调用 `utils/macro.ts`，删除本地副本 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `utils/macro.ts:25-56` 与 `pages/record/index.vue:36-55,63-72` 的双份实现及 `utils/calculator.ts:407-427` 重叠。
2. 影响分析：比对双份实现的口径差异（三色阈值、150% 上限、超标建议映射表），确认以 `macro.ts` 为准无需行为变更。
3. 测试安全网：为 `utils/macro.ts` 的 `buildProgressItems/buildAdviceList` 补快照/断言用例（若无测试框架则至少手工对照记录页渲染结果留档）。
4. 选择架构模式：单一真源复用 — 页面只消费 `macro.ts` 输出。
5. 迁移计划：迁移记录页两处手写逻辑 → 删除本地副本 → 确认无其他页面引用被删代码。
6. 增量执行：单页面改动，一次提交完成。
7. 回归测绿：小程序 `npm run check` 绿；记录页三宏进度与超标建议渲染对照改前截图一致。
8. 用户确认：展示收敛 diff，获确认后收尾（写操作门禁）。

### B-T05 — AI skills 资产单一真源、冗余副本退库

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-003（内聚，高） |
| 目标 | 保留单一真源目录（建议 `.trae/` 全集），其余 5 个 IDE 副本目录移出版本库或改为安装脚本生成 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对六目录（`.agent/`、`.agents/`、`.claude/`、`.codex/`、`.opencode/`、`.trae/`）当前差异（`openspec-explore/SKILL.md` 16,221B vs 16,269B 漂移样本）。
2. 影响分析：确认各 IDE 是否支持符号链接或外部 skills 目录；确定真源选型（`.trae/` 或新建 `skills/` 中枢）。
3. 测试安全网：无代码测试适用；以"各 IDE 技能可被正确加载"为验收基线，操作前记录当前加载清单。
4. 选择架构模式：单一真源 + 分发 — 副本目录改由安装脚本复制/软链生成，或仅保留真源入 git。
5. 迁移计划：先漂移对齐（以最新副本为基线合并差异）→ 选定真源 → 冗余目录 `git rm -r` 并补安装脚本与 .gitignore 规则。
6. 增量执行：对齐、退库、脚本三步分开提交，避免巨型 diff。
7. 回归测绿：各 IDE 重新加载技能验证；仓库体积与文件数对照下降。
8. 用户确认：展示真源选型与退库清单，获确认后收尾（写操作门禁）。

### B-T06 — 小程序 pages→store→api 分层收敛并补 taper store

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-层次-001（层次，高） |
| 目标 | 四处页面直调 api 改经 store；新建 `store/taper.ts`；登出清理收敛到 user store 统一编排 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `pages/menstrual/index.vue:9`、`pages/home/index.vue:17`、`pages/mine/index.vue:18`、`pages/taper/plan.vue:8,13`、`pages/mine/index.vue:139-146`、`pages/mode/select.vue:41-91`。
2. 影响分析：梳理各直调点的状态需求（哪些需要缓存/哪些一次性），确定各 store 新增 action 的最小面。
3. 测试安全网：前端无单测基线，以 `npm run check` + 关键页面（首页/记录/我的/532 计划）手工回归清单为安全网，改前记录接口调用序列。
4. 选择架构模式：分层状态管理 — pages→store→api 单向；登出由 user store 遍历清理注册表。
5. 迁移计划：建 `store/taper.ts` → 逐页切换直调为 store action（每页一次提交）→ 登出清理收敛 → 删除页面手工 reset。
6. 增量执行：按页面粒度小步提交，保持每步可回退。
7. 回归测绿：`npm run check` 绿；按回归清单逐页验证数据加载与登出后状态清空。
8. 用户确认：展示分层收敛 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，五步五提交 + 文档收尾）：

1. 新建 `store/taper.ts` 收敛 532 计划状态，`taper/plan.vue` 直调 `getTaper532Plan` 改经 `taperStore.fetch`（0ca09b5）。
2. 新建 `store/reminder.ts` 收敛提醒状态（含 masterEnabled computed 与 submitting 态），`reminder/index.vue` 直调 `getReminder/saveReminder/reportSubscribe` 与 `mine/index.vue` 直调 `getReminder` 改经 reminderStore（c4f3e6b）。
3. `home/index.vue` 直调 `getMenstrual` 改经已有 `menstrualStore.fetch`，periodPhase 徽标收敛为 store computed（df66795）。
4. `menstrual/index.vue` 直调 `getMenstrual/saveMenstrual` 改经 `menstrualStore.fetch/save`，vo 收敛为 store computed（2e2cd78）。
5. 登出清理与模式切换编排收敛：`userStore.logout()/abandonGuest()` 统一 reset 七大业务 store（原 mine/index.vue 手工 reset 5 个且漏 taper/reminder）；`cycleStore.switchMode` 收敛 `mode/select.vue` 直调 `switchDietMode` 与切回 532 本地清空（1e94ae1）。
6. 回归：`vue-tsc --noEmit` 每步全绿；全量 grep 核验 pages/components 对 `@/api/*` 仅剩 type-only import，运行时分层单向闭环。

### B-T07 — 游客清理任务改批级独立事务

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-层次-003（层次，高） |
| 目标 | `GuestCleanupTask` 的 `while(true)` 分批改为每批独立事务，事务长度有上界 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `task/GuestCleanupTask.java:42-76` 方法级 `@Transactional` 包全循环的现状。
2. 影响分析：确认批间无跨批数据依赖；确认编程式事务或自调用代理方案在现有结构下的可行性（注意同类内调用事务失效问题）。
3. 测试安全网：为清理任务补测试（过期游客命中/未过期跳过/批次边界），改前跑绿。
4. 选择架构模式：批处理事务分离 — 拆出独立事务批方法（经代理调用）或 `TransactionTemplate`。
5. 迁移计划：单任务内改造，无跨模块影响，一次性完成。
6. 增量执行：改造 + 日志（每批耗时/条数）同批提交。
7. 回归测绿：后端 `mvn -q test` 全绿。
8. 用户确认：展示事务边界改造 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，a19ad60）：

1. 拆出 `GuestCleanupBatchExecutor` 独立 Bean 承载 `@Transactional purgeOneBatch`（选批 + 批内清理一事务），规避同类自调用事务失效；软删经 @TableLogic 自动排除已清理行，批间无依赖。
2. `GuestCleanupTask.purgeExpiredGuests` 去除方法级 `@Transactional`，只编排批次循环；新增每批 `size/costMs` 日志；单批失败仅回滚本批，已提交批次不回吐，次日任务自然续扫。
3. 补 `GuestCleanupTaskTest` 5 用例（空批零写 / 批内全链路清理 / 首空批即停 / 满批继续+不满批终止 / 恰好满批再探一次），`mvn test` 123 用例全绿。

### B-T08 — SQL 迁移版本化（版本表 + 幂等守卫）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-001（演进，高） |
| 目标 | 建立 `schema_migrations` 版本记账，9 个 change 脚本补幂等守卫，`SqlRunner` 去除 `--allow-error` 吞错 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `sql/change1_users_guest.sql` ~ `change9_admin_analytics.sql` 无守卫、`change2_users_delete_flag.sql:10-24` 破坏性多步、`tools/db/SqlRunner.java:33` `--allow-error`。
2. 影响分析：盘点各环境（本机/现网）当前迁移进度差异；评估引入 Flyway 与最小自研版本表的成本取舍（一期建议最小方案）。
3. 测试安全网：在独立测试库演练：全新库执行全链、已迁移库执行全链（幂等验证），记录前后 schema diff。
4. 选择架构模式：最小版本表 — `schema_migrations(version, script, applied_at)` + 脚本头 `IF NOT EXISTS`/条件守卫；`SqlRunner` 先查版本表再执行。
5. 迁移计划：建版本表与 runner 改造 → 逐脚本补守卫（change2 破坏性段加事务包裹）→ 现网回填版本记录脚本。
6. 增量执行：先合入基建，再逐脚本补守卫，最后出回填说明。
7. 回归测绿：测试库双场景（新库/旧库）全链跑通；`SqlRunner` 无 `--allow-error` 且失败即终止。
8. 用户确认：展示版本表 DDL 与守卫改造清单，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，09d9c44 + 3e9c4ff 两提交）：

1. 步骤一基建（09d9c44）：`schema_migrations` 版本表（change0 脚本 + runner 内嵌 DDL）；`SqlRunner` change<N> 命名自动建表/查账/SKIP/记账，`--allow-error` 移除失败即终止（exit 2）且不记账；`change_backfill_migrations.sql` 存量回填（INSERT IGNORE 幂等）。
2. 步骤二守卫（3e9c4ff）：9 个 change 脚本补幂等守卫——change1/6/8 ALTER 段改 information_schema 实时条件守卫（DO 0 占位），change2 破坏性三步各自前置状态守卫支持断点续跑，change3/4/5/7/9 CREATE 补 IF NOT EXISTS；脚本头补 `-- 幂等:` 注释。
3. 演练暴露并修复：`run-sql.cmd` 硬编码路径失效（javac 改 PATH 解析、connector jar 改 maven 3.9.15）；README 同步并成文全新环境引导（建库→data.sql 基线→change 链；MYSQL_URL 必须带默认库，否则版本表 DDL 报 No database selected）。
4. 五场景演练全绿：A 全新库全链（data.sql 基线→change0~9，账 10 条/16 表/守卫列齐）/ B 重跑全 SKIP / C 删账重跑 change1/2/9 守卫零重复重建账自动补回 / D 错误脚本 exit 2 后续不执行且不记账 / E 清账→回填→全 SKIP。
5. 回归：`mvn test` 123 用例全绿；演练发现 data.sql 为 users 基线（change 链守卫前置），现状输入移交 B-T10。

### B-T09 — 删除前端影子算法库（isPlateau 优先）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-002（演进，高） |
| 目标 | 删除 `calculator.ts` 死函数（含口径分叉的 `isPlateau` 与零调用的 `overCheck`），保留函数标注"仅展示兜底"，解除 `format.ts` 反向 import |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `utils/calculator.ts:407-427,437-443` 零调用、`pages/body/profile.vue:11` 仅用 bmr/tdee/macro532Base、`store/weight.ts:30` 消费后端 `plateau`、`utils/format.ts:5` 反向 import。
2. 影响分析：全库检索 `calculator` 各导出函数调用方，确认删除清单无漏网（含动态引用/字符串引用）。
3. 测试安全网：小程序 `npm run check`（vue-tsc 会暴露删除后的悬空引用）为安全网；`profile.vue` 保留函数改前跑一遍身体档案页。
4. 选择架构模式：死代码删除 + 注释契约 — 保留函数加"仅展示兜底，权威口径在后端"标注。
5. 迁移计划：删 `isPlateau`/`overCheck` 等死函数 → `round` 迁移到 `format.ts` 或公共工具解除反向 import → 标注保留函数。
6. 增量执行：删除与迁移分步提交。
7. 回归测绿：`npm run check` 绿；身体档案页与体重页手工回归。
8. 用户确认：展示删除清单 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，0dbac67）：

1. 删除清单（calculator.ts 443→66 行）：`isPlateau`（按条数 slice(-7) 与后端 7 天窗口口径分叉的潜伏雷）、`overCheck`（零调用）、`menstrualPhase`/`plan532Stages`/`macro532`/`carbonConst`/`cycleAlloc`/`targetKcal` 导出/`parseDate`/`todayStart`/`clamp` 及全部影子类型（PhaseInfo/Plan532Item/CycleDay/CarbonConst/CycleAlloc/OverResult 等）全删。
2. 保留函数：`bmr`/`tdee`/`macro532Base`（录入页 profile.vue 实时预览唯一消费方）+ 精简版 `CalcProfile`/`MacroResult`，文件头与逐函数标注「仅展示兜底，权威口径在后端」；`round` 收为模块私有。
3. `format.ts` 反向 import 解除：删除 `import { round } from './calculator'` 与 `export { round }` 再导出（全库无 round 消费方）。
4. 连带退库：`constants.ts` 中仅影子算法消费的 `PERIOD_PHASES`/`PeriodPhaseCfg`（经期四阶段）、`STAGE_532`/`Stage532`（532 四阶段）、`DayType` 一并删除；`MACRO_532`/`KCAL_PER_G`/`CYCLE`/`RANGES` 仍有页面与预览消费，保留。
5. 回归：全量 grep 核验无悬空引用（含字符串/动态引用），`npm run type-check`（vue-tsc --noEmit）全绿；共删 456 行（3 文件 +12 −456）。

### B-T10 — data.sql 基线整改 + 弱口令哈希出库

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-007（演进，高） |
| 目标 | data.sql 二选一整改（重生成完整基线或删除），弱口令哈希移出仓库，新环境搭建路径成文 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `resources/data.sql:10-92`（缺列缺表）、`:91-92`（admin/admin123 哈希）、与 `sql/change2_users_delete_flag.sql:15` 的半追平对照。
2. 影响分析：确认 data.sql 是否被代码/启动流程引用（Spring `spring.sql.init` 配置）；裁定"完整基线"还是"仅走 change 链"。
3. 测试安全网：测试库演练裁定后的新环境搭建全流程（从零到可登录），记录步骤。
4. 选择架构模式：基线单一来源 — 基线脚本与 change 链（B-T08 版本表）统一记账。
5. 迁移计划：移除弱口令哈希（改为部署文档说明初始化方式或环境变量注入）→ 按裁定重生成/删除 data.sql → 更新 README/部署说明。
6. 增量执行：哈希出库与基线整改分开提交。
7. 回归测绿：后端 `mvn -q test` 绿（确认无测试依赖 data.sql）；新库搭建演练通过。
8. 用户确认：展示裁定结论与整改 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，10dad43 + 步骤二提交）：

1. 裁定：完整基线 — data.sql 重写为全量基线（建库 + 15 张业务表终态，与 change0~9 链终态一致），全部 `CREATE TABLE IF NOT EXISTS` 幂等，重复执行安全；不采用"删除 data.sql 仅走 change 链"（保留全新环境一步到位引导）。
2. 步骤一（10dad43）：admin/admin123 BCrypt 哈希 INSERT 出库；tools/db/README 新增「管理员初始化」小节（部署者自生成 BCrypt 哈希手工 INSERT，口令哈希不进版本库）；根 README/03-开发规范/uniapp README 三处 admin123 表述同步改指引。
3. 步骤二：data.sql users 主表基线 → 全量基线（+user_body/user_body_history/foods/diet_records/carb_cycle_plan/carb_cycle_day/user_reminders/reminder_send_log/user_menstrual/weight_record/adjust_log/track_event/stat_daily_active/stat_event_daily，含 change1 游客三列、change2 软删 + username_active 等生成列唯一键、change6/8 全量列）；根 README §2.4/§4.7、03-开发规范 §4.7、tools/db/README 全新环境引导同步改「全量基线」表述。
4. 演练（演练库 zhenxinjian_bt10，已 DROP）：data.sql 建库 + 15 表全 OK → change0~9 全链执行 RECORDED version 0~9（守卫零变更）→ 重跑 change1/9 全 SKIP → schema_migrations 账 10 条、表 16 张（15 业务 + 版本表）。
5. 回归：`mvn test` 123 用例全绿（0 失败 0 错误，确认无测试依赖 data.sql 初始数据）。
6. 演练中发现的工具链问题（非本任务代码问题，仅记录）：run-sql.cmd 会追加传入 db.local.properties 覆盖命令行 properties（SqlRunner 取最后一个），演练须直接调 java 绕开；本机 JDK 实际路径 D:\App\java\jdk-25.0.4.1。

### B-T11 — 定时任务线程池配置 + 推送外呼批量化

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-004（边界，高） |
| 目标 | 配置 `ThreadPoolTaskScheduler`（≥3 线程），推送任务改批量/异步外呼，消除调度饥饿 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `ZhenxinjianApplication.java:16` 仅 `@EnableScheduling`、`task/ReminderPushTask.java:77,187,152-172`、同池 `StatAggregateTask`/`GuestCleanupTask`。
2. 影响分析：统计各 `@Scheduled` 任务的频率与耗时特征；确认微信订阅消息批量接口可用性（subscribeMessage.send 是否支持批量或需并发）。
3. 测试安全网：为推送任务现有判定逻辑补测试（改前跑绿）；记录当前调度日志时序基线。
4. 选择架构模式：独立调度池 + 外呼批量化 — `SchedulingConfigurer` 注入 `ThreadPoolTaskScheduler`；推送循环改批量调用或限并发异步。
5. 迁移计划：先配线程池（低风险独立提交）→ 再改推送外呼方式 → 观察日志验证并行度。
6. 增量执行：两步分开提交。
7. 回归测绿：后端 `mvn -q test` 全绿；启动后确认调度线程池日志与推送耗时下降。
8. 用户确认：展示线程池配置与外呼改造 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，e1e2ad6 + a05ded6）：

1. 裁定外呼方式：微信订阅消息 subscribeMessage.send 为单用户接口（无批量 API），采「限并发异步」而非批量调用——调度线程只编排扫描，外呼投递独立执行器。
2. 步骤一（e1e2ad6）：新增 `config/ScheduleConfig.java`——`taskScheduler`（ThreadPoolTaskScheduler 池 4 线程，Bean 名被 Spring 调度自动识别，承载全部 3 个 @Scheduled 任务留一余量）+ `reminderPushExecutor`（core2/max4/queue200，优雅停机 30s）。
3. 步骤二（a05ded6）：`ReminderPushTask` 扫描循环改投递 `reminderPushExecutor` 异步执行 processOne（判定链与异常静默语义不变）；Executor 按构造参数名注入（全库唯一 Executor 消费方，javap 验证 MethodParameters 元数据在，两个 Executor Bean 无歧义）。
4. 测试安全网：补 `ReminderPushTaskTest` 8 用例（同步 directExecutor 驱动）覆盖 D4 判定链全分支——模板未配置空转/微信服务不可用空转/当日已发跳过/已记录跳过/游客无 openid 跳过/额度 0 跳过/成功下发 onPushSuccess/微信异常 onPushFail 静默；全套 131 用例全绿。
5. 冒烟：补齐本机缺失的 application-dev.yml（gitignored，application-dev.yml.example 副本改本机凭据；此前本机 `mvn spring-boot:run` 因缺 dev 配置无法启动），dev profile 启动 3.163s 成功，无 BeanCreationException/UnsatisfiedDependency，Tomcat 8080 正常，验证后干净停机。

### B-T12 — 埋点毒批次毒性隔离与饱和告警

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-005（边界，高） |
| 目标 | 后端区分"可重试失败/永久失败"返回码；前端隔离毒条目不再原样重试；队列接近上限时告警埋点 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对后端 `TrackEventService.java:41-45` 整批拒收、前端 `utils/track.ts:69-90` 失败原样保留、`:51-53` 上限 200 丢弃。
2. 影响分析：定义错误码契约（如契约类错误 40901 → 永久失败；服务类 5xx → 可重试）；确认前端对单条毒数据的识别方式（按后端返回的非法事件码列表剔除）。
3. 测试安全网：后端为批量校验补测试（混批：合法+非法事件）；前端以 `npm run check` 为基线。
4. 选择架构模式：毒性隔离（poison message 模式）— 永久失败条目剔除并本地计数，可重试批次保留；队列饱和度（如 ≥80%）上报自监控事件或日志。
5. 迁移计划：后端返回码细化 → 前端 track.ts 按码分流 → 告警埋点。
6. 增量执行：后端、前端分两批提交，契约先行对齐。
7. 回归测绿：后端 `mvn -q test` + 小程序 `npm run check` 全绿；构造非法事件码验证不再无限重试。
8. 用户确认：展示错误码契约与分流 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，a576ac8 单提交契约先行）：

1. 裁定错误码契约：放弃"40901 整批拒收 + 端上识别契约错误码"方案，采更简「恒 200 + 响应 data 携带剔除码列表」——永久失败（毒数据）显式枚举在 200 响应里，5xx/网络异常天然属可重试失败，端上无需按错误码分流；`TRACK_EVENT_INVALID_CODE`(40901) 常量随之无引用删除。
2. 后端：`TrackEventService.saveBatch` 整批拒收改逐条白名单校验——毒条目剔除不入库（LinkedHashSet 去重保序收集）、合法条目落库、全非法跳过 insert，返回剔除码列表；`TrackController` 改 `Result<List<String>>`；`TrackEventEnum` 新增 `track_queue_saturated` 自监控事件码。
3. 前端：`api/track.reportEvents` 改 resolve 剔除码列表；`utils/track` flush 成功整批移除（毒条目已被服务端剔除随响应永久丢弃，不再每 10s 无限重试占满队列）；新增 `checkSaturation`——队列 ≥80%（160/200）入队一条 `track_queue_saturated`（extra 携带 queueSize/queueMax），回落至阈值下复位标记避免重复告警。
4. 测试安全网：`TrackEventServiceTest` 重写 5 用例匹配新契约——合法落库无剔除/混批毒性隔离（验证仅合法条 insert）/全非法跳插且剔除码去重保序/未登录允许/extra 截断；全套 132 用例全绿。
5. 回归：`mvn test` BUILD SUCCESS（132/0/0）；小程序实际脚本为 `npm run type-check`（tasks 基线命令笔误 `npm run check`，以 package.json 为准），vue-tsc 通过；`openspec/specs/track/event/spec.md` 同步——整批拒收场景改毒性隔离场景、新增队列饱和自监控场景。

### B-T13 — 业务常量单一真源 + Redis 连接配置收敛

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-003（演进，中） |
| 目标 | 以后端枚举 + openspec/specs 为唯一真源；前端常量加对照锚点；删除原型副本；JetCache 与 spring.data.redis 连接配置收敛 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对后端枚举（`MenstrualPhaseEnum.java:10-19`、`DeficitOptionEnum.java:10-19`、`ActivityLevelEnum.java:13`）、前端 `constants.ts:7-12,18,27-43,58-79`、原型 `miniprogram/config/constants.js:37-50`、`invariants.md:13,18`、`store/food.ts:52` 与 `FoodHotConstant.java`、`application.yml:146` vs `application-dev.yml:13-19`。
2. 影响分析：逐项确认常量当前值是否一致；找出已漂移项并裁定权威值。
3. 测试安全网：前端为关键常量（经期系数/缺口档/活动系数）补对照后端枚举值的断言用例或注释锚点；Redis 连接改动在开发环境验证启动。
4. 选择架构模式：真源锚定 — 前端常量注释标注后端枚举来源；Redis 连接参数单一属性源（`${spring.data.redis.*}` 复用）。
5. 迁移计划：删原型副本（随 B-T30 协同）→ 前端注释锚点 → Redis 配置收敛。
6. 增量执行：按端分批提交。
7. 回归测绿：后端 `mvn -q test` + 小程序 `npm run check`；开发环境启动验证 JetCache 与 Redis 连接正常。
8. 用户确认：展示漂移裁定与收敛 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-12，8e193f8 单提交）：

1. 全量比对裁定零漂移：ACTIVITY 系数 1.2/1.375/1.55/1.725 = ActivityLevelEnum.factor（数组序即 code 1-4，`form.activityLevel` 直接索引）；DEFICIT_OPTIONS [200,300,400,500] = DeficitOptionEnum 四档；CYCLE carbCoef 2.5/fatCoef 0.8·1.0/proteinCoef 1.5/ratio/template = CycleCalcService（CARB_POOL_FACTOR、DEFAULT_CFC、CFC_OPTIONS、DayTypeParam、TEMPLATE）；RANGES = BodyProfileSaveDTO @Min/@Max（12-80）、WeightService WEIGHT_MIN/MAX（25-200）、MenstrualService CYCLE_LEN/PERIOD_DAYS（21-35/3-10）、CyclePlanService（7-14）；经期上浮前端无副本（MenstrualPhaseEnum 经 VO 下发 carbUplift/kcalUplift）；store/food.ts LOCAL_HOT_CODES 与 FoodHotConstant 同序同编号（注释锚点已在）。
2. 前端锚点：constants.ts 四块（ACTIVITY/DEFICIT_OPTIONS/CYCLE/RANGES）加后端对照锚点注释（前端无测试基建，采注释锚点而非断言用例）； MenstrualPhaseEnum 清理 B-T09 退库 PERIOD_PHASES 的失效注释引用，改为「经期 VO 下发键」。
3. Redis 收敛：application-dev.yml.example jetcache uri 改 `redis://:${spring.data.redis.password}@${host}:${port}/${database}` 单一属性源拼接（改连接只改 spring.data.redis 一处），特殊字符密码需 URL 编码场景注释指引改显式写死；本机 dev.yml 同步拼接验证空密码边界。
4. 原型副本：MRD-PRD 原型 constants.js（PERIOD_PHASES/ACTIVITY 等副本）删除并入 B-T30（原型工程整体移出版本库）协同，本任务不单独删。
5. 回归：mvn test 132 全绿；vue-tsc 通过；dev 冒烟启动 3.072s 成功、Tomcat 8080、JetCache 统计日志正常、空密码拼接 uri（redis://:@host）Lettuce 可用零异常（冒烟中发现并清理 B-T11 残留 8080 实例 10152）。

### B-T14 — 双端演示残留清单式清理

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-004（演进，中） |
| 目标 | 按清单删除小程序/管理后台/后端三处演示残留（死 API、死类型、白名单死路由、死常量、静态资源） |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对证据清单全部位点（小程序 `api/auth.ts:17-29,52-54`、`api/types.ts:13-29,65-69`、`api/request.ts:15`、`data/foods.ts:237-253`、`config/constants.ts:148-154`；后台 `api/auth.ts:19-21`、`api/types.ts:35-42`、`api/user.ts:22-24`、`vite-env.d.ts:16-17`、`vite.config.ts:27-28`、`public/notify.wav`、`api/request.ts:54`；后端 `CommonConstant.java:239`）。
2. 影响分析：逐项全库检索调用方确认真零引用（含模板内动态字符串引用）。
3. 测试安全网：双端类型检查为安全网；后端编译 + 现有测试为安全网。
4. 选择架构模式：纯删除，无新模式。
5. 迁移计划：按端三批提交（小程序 / 管理后台 / 后端）。
6. 增量执行：每批删除后立即跑对应检查。
7. 回归测绿：小程序 `npm run check`、后台 `npm run build`、后端 `mvn -q test` 全绿。
8. 用户确认：展示删除清单与 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，99e72a1 单提交）：

1. 影响分析逐项裁定：getCaptcha/login/register/guestRenew 全库零调用；LoginRequest/RegisterRequest/CaptchaResult 仅被死函数引用；store/food.ts 的 searchFoods 来自 `@/api/food`（data/foods.ts 本地同名死函数无引用）；groupByCategory 零调用；defaultMealType 非死码而是「行为函数混入常量文件」，唯一消费方 pages/record/add.vue；管理后台 register/RegisterRequest/getUserById 零调用、VITE_WS_* 仅 vite-env.d.ts 自声明、notify.wav 零引用；后端 LOGIN_FAIL_MAX 零引用（实际口径以 zhenxinjian.redis.login-fail-max 配置为准，注释自证）。
2. 小程序批次：api/auth.ts 删四死函数（连带类型 import 收窄）；api/types.ts 删三死类型；request.ts AUTH_SKIP_URLS 5→2（仅留 /auth/wechat/login、/auth/guest）；data/foods.ts 删 searchFoods/groupByCategory；constants.ts defaultMealType 下移 record/add.vue（hour 参数无人使用一并收窄）；`npm run type-check`（实际脚本名，tasks 基线 `npm run check` 笔误同 B-T12 记录）vue-tsc 通过。
3. 管理后台批次：api/auth.ts 删 register；api/types.ts 删 RegisterRequest；api/user.ts 删 getUserById；request.ts AUTH_SKIP_URLS 剔 /auth/register + ws 注释改「其他模块」；vite-env.d.ts 删 VITE_WS_PATH/VITE_WS_PROTOCOL；vite.config.ts 删 ws:true + 注释；public/notify.wav 删除；`npm run build`（vue-tsc + vite）通过。
4. 后端批次：CommonConstant.java 删 LOGIN_FAIL_MAX；`mvn -q test` BUILD SUCCESS，surefire 23 类 132 用例 0 失败 0 错误。
5. 用户确认：删除清单 + diff stat（14 文件 +18/-119）展示并获「执行完提交」指令后提交，写操作门禁通过。

### B-T15 — 构建配置版本治理（BOM 回归 + TS 工具链对齐）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-005（演进，中） |
| 目标 | pom 移除冗余显式版本回归 parent BOM；uniapp TS 工具链评估升级至 5.x；front overrides 手钉项评估去除 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `pom.xml:53+` 11 处显式版本、`:141` hutool-all、`uniapp/package.json:45-47`（TS ^4.9.4 / vue-tsc ^1.0.24）vs front TS 5.4、`front/package.json:29` overrides。
2. 影响分析：确认哪些依赖版本由 spring-boot parent BOM 管理可直接移除；uniapp 升级对 `@dcloudio/*` 兼容矩阵的影响。
3. 测试安全网：后端 `mvn dependency:tree` 改前留档对比；双前端改前 `npm run check`/`npm run build` 基线。
4. 选择架构模式：依赖治理 — BOM 优先，显式版本仅保留 BOM 未覆盖项（如 hutool 收敛为按需模块或保留注释说明）。
5. 迁移计划：pom 清理 → uniapp TS 工具链升级（独立验证分支）→ front overrides 评估。
6. 增量执行：三端分开提交，uniapp 升级单独验证。
7. 回归测绿：后端 `mvn -q test` + dependency:tree 无意外降级；双前端类型检查/构建全绿。
8. 用户确认：展示版本对比与改动 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，16de29e / c472b5a / c8e0898 三批提交）：

1. 影响分析裁定：dependency:tree 改前留档比对——6 处 starter 与 spring-boot-maven-plugin 显式版本（均 3.5.16）与 parent BOM 管理值相同属冗余；lombok 手钉 1.18.48 可回归 BOM 1.18.46（annotationProcessorPaths 处 ${lombok.version} 经 parent properties 继承链解析 BOM 值，无需本地覆盖）；byte-buddy 1.18.11 显式 compile 依赖全库代码零引用且无传递来源（test scope byte-buddy/-agent 1.17.8 由 mockito 传递），裁定死声明；hutool 使用面全库 import 裁定仅 core（StrUtil/CollUtil/DateUtil/FileUtil/IdUtil/BeanUtil）、json（JSONUtil）、extra.servlet（JakartaServletUtil）、captcha（CaptchaUtil/LineCaptcha）四模块；front overrides brace-expansion 5.0.8 系 CVE-2025-5889 手钉，minimatch@9.0.9 声明 ^2.0.1 自然解析 2.1.4 已含修复（≥2.0.2）且主版本与声明一致。
2. 后端批次（16de29e）：properties 删 spring-boot.version/lombok.version/bytebuddy.version 三本地覆盖；6 处 starter 与 spring-boot-maven-plugin 去显式版本回归 BOM；lombok 依赖去版本（BOM 管理）annotationProcessorPaths 保留 ${lombok.version}；byte-buddy 死声明整段删除；hutool-all 收敛 core/json/extra/captcha 四模块（注释标注按需缘由）；`mvn test` 132 用例 0 失败 0 错误 + dependency:tree 对比仅预期三项变化（lombok 1.18.48→1.18.46 BOM 值、byte-buddy compile 移除、hutool 按需）无意外降级。
3. 小程序批次（c472b5a）：typescript ^4.9.4→^5.4.5、vue-tsc ^1.0.24→^2.0.19 对齐 front 版本线；升级后 vue-tsc 2 + TS 5.5 报 TS5102（@vue/tsconfig 0.1.x 内置 importsNotUsedAsValues/preserveValueImports 已被 TS 移除），@vue/tsconfig ^0.1.3→^0.7.0（改 verbatimModuleSyntax）消除；`npm run type-check`（实际脚本名，基线 `npm run check` 笔误同 B-T12/B-T14 记录）零错误、`npm run build:mp-weixin` 构建绿。
4. 管理后台批次（c8e0898）：overrides brace-expansion 5.0.8 删除，npm install 后自然解析 2.1.4（minimatch@9 声明线，CVE 已修复且较跨大版本手钉 5.x 更稳）；`npm run build`（vue-tsc + vite）全绿。
5. 用户确认：三批改动清单与 diff 展示，获「最高权限自行处理、每任务提交推送」指令后逐批提交，写操作门禁通过。

### B-T16 — 埋点事件码前端常量表 + track() 类型收窄

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-008（演进，中） |
| 目标 | 新建 `config/track-events.ts` 常量表与后端 `TrackEventEnum` 互锚；`track()` 参数收窄为联合类型，~30 处字面量调用改引常量 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对后端 `TrackEventEnum.java:7-70`（22 码）与前端 ~30 处字面量样本（`pages/record/index.vue:159`、`pages/food/index.vue:56,98,104` 等）。
2. 影响分析：全库检索 `track('` 调用点，核对字面量与后端枚举的一一对应（找出拼错/缺漏项）。
3. 测试安全网：`npm run check` 收窄类型后会自动暴露非法字面量，即安全网。
4. 选择架构模式：共享契约常量 — 常量表注释标注后端枚举来源与同步约定。
5. 迁移计划：建常量表与收窄 `track()` 签名 → 逐页替换字面量 → 修正发现的拼写错误。
6. 增量执行：签名收窄一次性提交（编译错误驱动逐页修复）。
7. 回归测绿：`npm run check` 绿（零非法事件码）。
8. 用户确认：展示常量表与替换 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，0c1ff70 单提交）：

1. 影响分析：全库检索 `track('`/`track("` 字面量调用点 16 文件 31 处，逐一对照后端 TrackEventEnum 22 码裁定——全部字面量拼写与后端一致（零拼错零缺漏）；`mode_select` 为后端枚举保留码（前端统一以 MODE_SWITCH 上报），常量表注释标注该差异。
2. 常量表：新建 `config/track-events.ts`——TRACK_EVENT 常量表键名即枚举名、值即 code，`as const` 派生 TrackEventCode 联合类型；文件头同步约定（后端枚举为契约真源，新增/修改事件码必须先改后端枚举再同步本表）。
3. 签名收窄：`utils/track.ts` 的 `track(eventCode: string)` 改 `track(eventCode: TrackEventCode)`，表外字面量编译期报错；内部自监控（track_queue_saturated）与 PV（page_view）同步改引常量。
4. 逐页替换 31 处：track.ts 2（自监控/PV）、auth/guide 4（LOGIN_WECHAT/LOGIN_GUEST/LOGIN_FAIL×2）、auth/expire 3（LOGIN_WECHAT/LOGIN_FAIL×2）、record/add 4（RECORD_ADD×2/RECORD_EDIT×2）、record/index 1（RECORD_DELETE）、food/index 3（FOOD_SEARCH/FOOD_HOT_CLICK/FOOD_HISTORY_CLICK）、food/detail 1（FOOD_DETAIL）、food/custom-edit 1（FOOD_CUSTOM_ADD）、home/index 2（HOME_QUICK_ENTRY×2）、mode/select 2（MODE_SWITCH×2）、cycle/plan 1 + taper/plan 1（PLAN_VIEW mode 2/1）、reminder/index 3（REMINDER_SAVE/REMINDER_SUBSCRIBE×2）、weight/index 1（WEIGHT_ADD）、menstrual/index 1（MENSTRUAL_SAVE）、body/profile 1（BODY_SAVE）；替换后全库 `track('`/`track("` 检索零命中。
5. 回归：`npm run type-check`（实际脚本名，tasks 基线 `npm run check` 笔误同 B-T12/B-T14/B-T15 记录）vue-tsc 零错误——收窄类型未暴露任何非法字面量；`npm run build:mp-weixin` 构建绿（circular chunk store/user↔api 警告为 B-T17 在治项，sass legacy 警告为存量）。
6. 用户确认：常量表 + 替换 diff（17 文件 +107/-32）展示，按「最高权限自行处理、每任务提交推送」既有指令提交 0c1ff70，写操作门禁通过。

### B-T17 — 小程序 api 层去 store 依赖（事件化 401 处理）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-耦合-002（耦合，中） |
| 目标 | `request.ts` 不再 import store，401/续期改事件或回调注入；track 管道复用统一请求封装或共享配置 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `api/request.ts:7,61,102-104,117-119` 与 `api/track.ts:27,8`、`request.ts:9` 双轨 `BASE_URL`。
2. 影响分析：确认事件机制选型（uni.$emit 或注入回调）；确认 401 清理会话的订阅方（App.vue 或 user store）。
3. 测试安全网：以 `npm run check` + 登录/401 续期手工回归清单为安全网。
4. 选择架构模式：依赖倒置 — request 发布 `auth:expired`/token 提供器注入，store 层订阅。
5. 迁移计划：定义事件契约 → request.ts 去 store 化 → App/store 层接订阅 → track.ts 复用封装或共享配置。
6. 增量执行：契约与改造同批提交，订阅方先就位再切发布方。
7. 回归测绿：`npm run check` 绿；手工验证 401 场景跳登录与会话清理。
8. 用户确认：展示解耦 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，311ac2d 单提交）：

1. 确认坏味道与选型：核对 `api/request.ts:7,61,102-104,117-119` 四处 store 反向依赖与 `api/track.ts:8`、`request.ts:9` 双轨 `BASE_URL`；事件机制裁定回调注入（AuthHooks 接口 + setAuthHooks 注入器）而非 uni.$emit——类型安全、组合根显式装配、无全局事件名魔法字符串，订阅方即 user store（经 main.ts 闭包）。
2. 契约定义：`request.ts` 新增 `AuthHooks` 接口（onSessionClear 清内存会话态 / onTokenRefreshed 续期同步内存态）与 `setAuthHooks(hooks)` 注入器，模块级单例持有，`?.` 可选链调用天然覆盖 hooks 未装配时序（等价原 try/catch 的 Pinia 未就绪兜底）。
3. request.ts 去 store 化：删除 `import { useUserStore }`；续期路径改 `setToken(newToken); authHooks?.onTokenRefreshed(newToken)`；clearSessionAndGoLogin/GoExpire 两路径改 `removeToken(); authHooks?.onSessionClear(); uni.reLaunch(...)`——storage 清理与导航保留 api 层本层，hooks 仅同步内存态，401 被动路径语义与改造前逐行等价（不 reset 业务 store，业务 reset 仍属 logout/abandonGuest 主动路径）。
4. 装配与最小 action：`main.ts` 组合根 `app.use(createPinia())` 后 `setAuthHooks({ onSessionClear: () => useUserStore().clearSession(), onTokenRefreshed: (t) => useUserStore().syncToken(t) })`——pinia active 后请求期调用天然安全；`store/user.ts` 新增 `syncToken(t)`/`clearSession()` 两最小 action 并挂出。
5. BASE_URL 双轨收敛：`request.ts` 导出 `BASE_URL` 单一来源，`track.ts` 删本地定义改 `import { BASE_URL } from './request'`；15s（业务）/10s（埋点静默）超时差异裁定刻意保留并注释说明；track.ts 维持裸 `uni.request`（静默上报不弹 toast、不触发 401 跳登录，复用统一封装会引入不需要的拦截语义）。
6. 回归与用户确认：`src/api/` 目录 grep `@/store` 零命中；`npm run type-check`（实际脚本名，tasks 基线 `npm run check` 笔误同前序记录）vue-tsc 零错误；`npm run build:mp-weixin` 构建绿且 Circular chunk `store/user→api/auth→api/request→store/user` 警告消除（仅剩 sass legacy 存量警告）；解耦 diff 展示后按「最高权限自行处理、每任务提交推送」既有指令提交 311ac2d，写操作门禁通过。

### B-T18 — 管理后台 router↔store↔request 循环解耦

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-耦合-003（耦合，中） |
| 目标 | 清会话收敛 store 单一入口；守卫经 store 读登录态；request 不 import store/router |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `router/index.ts:6,53,69,76`、`store/user.ts:10,24,34`、`api/request.ts:8-9,22-33`（:53 直读 localStorage、:69/:76 双处 removeItem）。
2. 影响分析：确认登录态真源收敛方向（store 为准，localStorage 仅持久化介质）；确认 request 401 回调注入方式。
3. 测试安全网：`npm run build`（含 vue-tsc）为安全网；登录/登出/401 三场景手工回归清单。
4. 选择架构模式：单向依赖 — router→store、request→（注入回调），禁止反向 import。
5. 迁移计划：store 收敛清会话入口 → 守卫改经 store → request 去 import。
6. 增量执行：按模块分步提交。
7. 回归测绿：`npm run build` 绿；三场景回归通过；`madge` 或人工确认无循环 import。
8. 用户确认：展示解耦 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，541e7b4 单提交）：

1. 确认坏味道与选型：核对 router/index.ts 守卫直读 localStorage + 双处 removeItem、store/user.ts 反向 import router、request.ts 双反向 import（router+useUserStore）三处现状成立；注入机制裁定 AuthHooks 回调注入（与 B-T17 小程序侧同模式，双端契约对齐）——类型安全、组合根显式装配、无全局事件名魔法串。
2. request.ts 去双 import：删除 router 与 useUserStore 反向依赖，新增 `AuthHooks` 接口（getToken 提供 token/onTokenRefreshed 续期同步/onSessionClear 清会话跳登录）与 `setAuthHooks` 注入器；请求拦截 token 改 `authHooks?.getToken()`（不再直读 localStorage，真源归 store）；x-refresh-token 续期与 401（HTTP 与业务码）两路径改走 hooks；ElMessage 错误提示保留本层。
3. store/user.ts 收敛单一入口：新增 `clearSession()`（清内存态 token/userInfo + localStorage，不含导航——导航属调用方/组合根职责）与 `syncToken(t)` 两最小 action；logout finally 段改调 clearSession，删除 router 反向 import；注释标注 store 为登录态唯一真源、localStorage 仅持久化介质。
4. 守卫与装配：router/index.ts 守卫登录态改经 `userStore.token` 读取，两处 `localStorage.removeItem` 改 `userStore.clearSession()`；main.ts 组合根 `app.use(createPinia())` 后 `setAuthHooks({ getToken, onTokenRefreshed: syncToken, onSessionClear: clearSession + router.push('/login') })`——pinia active 后请求期调用天然安全；MainLayout handleLogout 在 store logout 后补 `router.push('/login')`（导航上移页面层）。
5. 回归与用户确认：grep 全库 import 核验依赖方向收敛 router→store→api 单向无环（store 仅剩 api import、api 层零内部 import）；`npm run build`（vue-tsc + vite）全绿；解耦 diff 展示后按「最高权限自行处理、每任务提交推送」既有指令提交 541e7b4 并推送，写操作门禁通过。

### B-T19 — 后端依赖风格统一 + 推送判定下沉 ReminderService

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-耦合-004（耦合，中） |
| 目标 | 成文统一依赖风格（接口化或实现类直依赖二选一）；推送任务 4 个 Mapper 裸查询下沉到 ReminderService |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 14 个 controller `import service.impl.*`、`task/ReminderPushTask.java:16,68,60-66,152-172`、`task/StatAggregateTask.java:3,20`，对照已接口化的 `UserService/StorageService`。
2. 影响分析：裁定统一风格（一期建议接口化或明确豁免清单）；梳理推送判定查询下沉后的 service 方法签名。
3. 测试安全网：为推送判定查询补测试（命中/未命中/免打扰分支），改前跑绿。
4. 选择架构模式：依赖约定成文（写入开发规范）+ 查询下沉。
5. 迁移计划：先下沉推送判定（小范围）→ 依赖风格成文 → 存量按新改动遵循、存量改造另立任务（避免本任务膨胀）。
6. 增量执行：下沉与文档两步提交。
7. 回归测绿：后端 `mvn -q test` 全绿。
8. 用户确认：展示风格裁定与下沉 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，81cba5f 单提交）：

1. 影响分析与裁定：依赖风格裁定「实现类直依赖为主 + 接口化豁免清单」——业务 Service 单一实现、无多态/替换点诉求时调用方直依赖 impl 类不强抽接口（避免接口膨胀），豁免五接口：多实现/替换点（SessionEvictor Noop 适配、GuestDataMigrator 编排族 Ordered 多态、StorageService 存储适配）与跨层稳定契约（UserService 继承 MP IService 被多 controller+task 依赖、WechatAuthService 登录契约）；controller/task 禁止直依赖 Mapper 规约，豁免两基础设施类（FoodLibraryInitializer 启动引导种子导入非业务判定、GuestCleanupBatchExecutor B-T07 批级事务主体下沉会割裂事务边界）；存量接口化改造按裁定另立任务避免本任务膨胀。
2. 推送判定下沉：ReminderPushTask 删 UserReminder/ReminderSendLog/DietRecord/User 四个 Mapper 直注与三处裸查询——ReminderService 新增 `scanDueReminders(hhmm, limit)`（到点扫描，LIMIT 上限保留）与 `hasSuccessPushToday(userId, mealType, date)`（I12 单次判定）、DietRecordService 新增 `hasRecord(userId, date, mealType)`（已记录不重复）、用户查询改经 `UserService.getById`；任务只编排与外呼，类注释补「判定查询一律经 service 层」约定。
3. 测试安全网同步切换：ReminderPushTaskTest 8 用例 D4 全分支桩定改 service 层（判定链逐关拦截断言不变）；新增 ReminderServiceTest 2 用例（scanDueReminders 透传带上限 / hasSuccessPushToday 三分支，无 MyBatis 环境下 TableInfoHelper 幂等初始化支撑 LambdaWrapper 列名解析）；DietRecordServiceTest 补 hasRecord 2 用例（有记录 true / 无记录与 null false）。
4. 规约成文：开发规范 §2.1 增补依赖风格裁定与 controller/task 禁直依赖 Mapper 两规约（含豁免清单与存量遵循约定）；docs/knowledge/code-standard/java/standard.md §3 同步摘要。
5. 回归：本机 JDK 路径 D:\App\Java\jdk-25.0.4.1（tasks 基线 `D:\App\java\25` 与实际安装不符，以实际为准），`mvn test` BUILD SUCCESS 136 用例 0 失败 0 错误（较基线 132 +4：ReminderServiceTest×2 + hasRecord×2）；task/controller 层 grep mapper import 仅剩两豁免文件。
6. 用户确认：下沉 diff 与风格裁定展示，按「最高权限自行处理、每任务提交推送」既有指令提交 81cba5f 并推送，写操作门禁通过。

### B-T20 — 饮食编辑态改 id 拉取或 store 暂存

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-耦合-005（耦合，中） |
| 目标 | 饮食记录编辑回显不再 URL 全量编码传参，改为传记录 id 由 add 页拉取（或 diet store 暂存） |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `pages/record/index.vue:137-142`（拼接）↔ `pages/record/add.vue:103-146`（解析）。
2. 影响分析：确认后端是否有单条查询接口可用；对比 id 拉取与 store 暂存两案（编辑页刷新丢态问题）。
3. 测试安全网：`npm run check` + 编辑回显手工回归清单（含特殊字符备注的编码场景）。
4. 选择架构模式：状态经标识传递 — URL 仅带 id。
5. 迁移计划：add 页支持 id 入参拉取 → index 页改传 id → 删除 URL 编码/解析段。
6. 增量执行：双页同批提交。
7. 回归测绿：`npm run check` 绿；编辑回显各字段（含中文备注）回归通过。
8. 用户确认：展示改造 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，2724839 单提交）：

1. 影响分析裁定：后端无单条查询接口，新增接口需动后端契约过重；diet store 当日分组（`dayData.meals[].records`）本就缓存完整 DietRecordVO（编辑入口仅在 record 列表页，点击时数据已在内存）——采「URL 仅带 id + add 页从 store 按 id 回显」，非独立编辑态容器，无刷新丢态面（深链/日期切换/记录已删找不到时 toast 引导返回）。
2. index.vue：handleEdit 双分支（手动/食物来源）全量 encodeURIComponent 拼接收敛为单一 `?editId=${record.id}`。
3. add.vue：onLoad 编辑分支删逐项 decode 解析，改从 dietStore.dayData.meals flatMap 按 id 查找——mealType/remark 直取 VO；source!==3 且 foodId 走 foodStore.detail 回显食物、克数取 amountG；source===3 回显 foodName/carbG/proteinG/fatG/kcal；中文备注不再经 URL 编解码。
4. 回归：全库 `encodeURIComponent|decodeURIComponent` 检索清零；`npm run type-check`（实际脚本名，tasks 基线 `npm run check` 笔误同前序记录）vue-tsc 零错误。
5. 用户确认：按「依次自动执行、每任务提交推送、期间无需确认」既有指令提交 2724839 并推送，写操作门禁通过。

### B-T21 — DietRecordService 职责拆分 + 小重复收敛

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-004（内聚，中） |
| 目标 | 拆出汇总/守恒校验职责；`round1`/`operator()`/CORS origin 解析三处小重复收敛到公共工具 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `DietRecordService.java:49`（446 行）、`:245,250,273` 跨调、`round1` 三处、`operator()` 六处、CORS 三处。
2. 影响分析：确定拆分边界（DietSummaryService / 守恒校验并入 B-T03 共享校验器）；评估 service 互调网是否引入新循环。
3. 测试安全网：为 DietRecordService 现有行为补关键测试（保存/汇总/守恒），改前跑绿。
4. 选择架构模式：职责分离 + 公共工具（`common/util`）。
5. 迁移计划：先收敛三个小重复（低风险）→ 再拆汇总/校验职责 → 调整跨调方向。
6. 增量执行：小重复与拆职责分开提交。
7. 回归测绿：后端 `mvn -q test` 全绿。
8. 用户确认：展示拆分与收敛 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，f6c43c4 批 1 + 641034b 批 2 双提交）：

1. 批 1（f6c43c4 小重复收敛，零行为变更）：`round1` 三处私有副本（BodyCalcService/Taper532Service/DietRecordService，BigDecimal/double 双 overload）收敛到 `common/utils/Numbers.round1` 单一真源；`operator()` 六处 `"user:"+userId` 副本（BodyProfile/CustomFood/CyclePlan/DietRecord/Menstrual/Weight）收敛到 `common/utils/Operators.user`，AdminFoodService 无参 `operator()` 返回管理员用户名属不同语义裁定保留；CORS origin env 逗号分隔扁平化解析（SecurityConfig/SecurityStartupChecker 双份，WebSocketConfig 第三处已随 B-T01 删除）收敛到 `common/utils/CorsOrigins.parse`，过滤空白与星号语义保留调用方本层（解析共享/过滤分层）。
2. 批 2（641034b 职责拆分）：新建 `DietSummaryService` 承接 `summary()` 整方法（532 推进口径经 `Taper532Service.todayTarget`、碳循环日型经 `CyclePlanService.findActiveDay`、经期上浮经 `resolvePhase` 跨模式叠加、未建档/无周期空态 `recorded=false` 口径不变）；`DietRecordService` 删 `UserBodyMapper`/`CyclePlanService`/`Taper532Service` 三依赖与 `rate`/`toDouble` 私有助手，回归 CRUD + 按日分组 + hasRecord 单一职责；跨调网 Cycle/Taper 改由 `DietSummaryService` 单向依赖零回边，DAG 无环。
3. 守恒校验职责无需新拆：B-T03 已抽 `MacroConsistencyValidator` 共享校验器，DietRecordService 仅一处调用 `withinTolerance`（已在批 1 完成对齐）。
4. DietController 注入 `DietSummaryService` 委派 `/diet/summary`；`/diet/records` 增删改查路径行为不变。
5. 测试安全网同步拆分：DietRecordServiceTest 15→11 用例（移除 summary 5 用例 + 构造器去 3 mock），新增 DietSummaryServiceTest 5 用例原样迁移（532 分发/碳循环日型/未建档空态/无周期空态/未来日期 40505），136 用例全绿。
6. 用户确认：按「依次自动执行、每任务提交推送、期间无需确认」既有指令提交 f6c43c4 与 641034b 并推送（首推 Recv failure，重推通过），写操作门禁通过。

### B-T22 — 小程序空态/当前模式单一口径

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-005（内聚，中） |
| 目标 | 空态判定与"当前模式"在 store 层定义单一口径，页面统一引用（模式建议以档案为准） |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对空态双份（`pages/home/index.vue:71-80` vs `pages/record/index.vue:84-86`）与模式双源（`home:43`、`record:75` summary.mode vs `mine:50`、`mode/select.vue:26` profile.mode）。
2. 影响分析：裁定模式真源（档案 profile.mode）；确认 home 页 `loaded` 前置条件保留为页面本地状态。
3. 测试安全网：`npm run check` + 首页/记录页/我的页模式显示对照回归。
4. 选择架构模式：单一真源 getter — body store 暴露 `currentMode`。
5. 迁移计划：store 定义口径 → 四页面切换引用 → 删除本地判定。
6. 增量执行：一批提交。
7. 回归测绿：`npm run check` 绿；模式切换场景（均衡/532）各页显示一致。
8. 用户确认：展示口径裁定与收敛 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，15d02c5 单提交）：

1. 模式真源裁定档案 `profile.mode`：bodyStore 新增 `currentMode`（未建档默认 532）与 `isCycleMode` 派生 getter；`summary.mode` 显示源废止——`home:43`/`record:75` 的 `summary.mode===2` 与 `mine:50`/`mode/select:26` 的 `profile?.mode ?? 1` 双源四页归一。
2. 空态三分支收敛 dietStore：新增 `showBodyEmpty`/`showCycleEmpty` getter（`summary.recorded=false` 时按 bodyStore 档案 `recorded` 与 `isCycleMode` 分流——未建档/碳循环无周期两口径单一真源）；home/record 双页内联判定删除改委派；home 页 `loaded` 前置随 `noProfile` getter 内聚（`loaded && summary!==null && !recorded`）一并吸收，页面不再自持加载态判定。
3. 保留项裁定：`record:177` 进度区 `!dietStore.noProfile` 直引 store getter 不属重复判定；`showMealEmpty`（已建档当日无记录第三分支）为 home 页独有业务口径保留本页。
4. 回归：vue-tsc `--noEmit` 零错误；`profile?.mode ??`/`summary?.mode` 全库检索清零，零行为变更。
5. 用户确认：按「依次自动执行、每任务提交推送、期间无需确认」既有指令提交 15d02c5 并推送（首推 Recv failure，随文档收尾一并重推），写操作门禁通过。

### B-T23 — 管理后台列表页骨架（usePageQuery + 字典层）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-006（内聚，中） |
| 目标 | 抽 `usePageQuery` composable + 通用分页条 + `constants/` 字典层，三个列表页迁移 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `view/users/index.vue:44-48,195-197,201-209,226-233,303-312,331-332,392-412`、`foods/index.vue:178-186,314-334`、`diet-records/index.vue:54-62,141-160`。
2. 影响分析：提取三页公共形态（查询重置页码/分页回调/toolbar/删后回退），确认差异点参数化方式。
3. 测试安全网：`npm run build` 为安全网；三页查询/分页/删除回归清单。
4. 选择架构模式：组合式骨架 + 字典层。
5. 迁移计划：建 composable 与字典 → 逐页迁移（每页一次提交）。
6. 增量执行：按页小步提交。
7. 回归测绿：`npm run build` 绿；三页回归清单通过。
8. 用户确认：展示骨架与迁移 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，0634cc0 批 1 + 5cc81db 批 2 + 4dbcba3 批 3 + d965e95 批 4 四提交）：

1. 批 1（0634cc0 骨架三件套）：新建 `constants/dicts.ts` 字典层——DictItem 统一 value/label/type 形态 + dictMap 码表 + dictLabel 兜底 '-'，USER_STATUS/USER_ROLE/DIET_MODE/ACTIVITY_LEVEL/PLAN_STATUS/FOOD_CATEGORY/FOOD_SOURCE/FOOD_STATUS/MEAL_TYPE/RECORD_SOURCE 十组与后端枚举互锚；新建 `composables/usePageQuery.ts` 查询骨架——loading/表格数据/总数托管 + 查询重置页码 + 分页回调 + 删后回退页码 + 挂载首载；新建 `component/PagePager.vue` 通用分页条含右对齐布局；`global.css` 新增 .list-page/.list-toolbar/.list-filters 三公共类自三页 scoped 副本收敛。
2. 批 2（5cc81db users 页迁移）：loading/tableData/total/loadUsers/handleSearch/handlePageChange/onMounted 七处样板收敛 usePageQuery，删后回退页码改 reloadAfterDelete；角色/状态字典页内 4 处声明全量收敛 USER_ROLE_OPTIONS/USER_STATUS_OPTIONS 单一真源，MODE_MAP/ACTIVITY_MAP/PLAN_STATUS_MAP 三本地字典删除改 dictLabel；分页器副本改 PagePager；.users-page/.toolbar/.filters/.pager 四 scoped 副本删除改全局类；usePageQuery 复用 api/types.ts 既有 PageResult 删本地重复定义。
3. 批 3（4dbcba3 foods 页迁移 + API 类型收敛）：foods 页七处样板收敛 usePageQuery，删后回退页码改 reloadAfterDelete；分类/来源/状态三本地字典删除改 FOOD_CATEGORY_OPTIONS/FOOD_SOURCE_MAP/FOOD_STATUS_MAP 单一真源，categoryName() 局部函数删除改 dictLabel；分页器副本改 PagePager；四 scoped 副本删除改全局类；adminFood.getFoodPage 与 adminDiet.getDietRecordPage 返回类型自内联 { records; total } 收敛 api/types.ts PageResult<T> 消除重复定义。
4. 批 4（d965e95 diet-records 页迁移）：七处样板收敛 usePageQuery；MEAL_MAP/SOURCE_MAP 两本地字典删除改 MEAL_TYPE_MAP/MEAL_TYPE_OPTIONS/RECORD_SOURCE_OPTIONS 单一真源，displayUser 参数类型自 AdminDietRecord 收敛行内结构；分页器副本改 PagePager；四 scoped 副本删除改全局类；只读页无删后回退。
5. 回归：四批各自 `vue-tsc + vite build` 全绿；三页样板（七处查询/分页/挂载 + 四 scoped 样式 + 分页器副本 + 本地字典）全量收敛骨架与字典层单一真源，零行为变更。

### B-T24 — 微信登录流程下沉 store/user.ts

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-内聚-007（内聚，中） |
| 目标 | `uni.login → code → loginByWechat` 收敛为 `store/user.ts` 的 `loginByWechat()`，两页面改调用 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `pages/auth/guide.vue:33-55` 与 `pages/auth/expire.vue:31-53` 逐行复制。
2. 影响分析：比对两份实现的差异（错误提示/游客续期分支），合并时保留并集行为。
3. 测试安全网：`npm run check` + 登录/游客续期手工回归。
4. 选择架构模式：能力下沉到状态层。
5. 迁移计划：store 新增方法 → 双页切换 → 删除重复段。
6. 增量执行：一批提交。
7. 回归测绿：`npm run check` 绿；登录链路回归。
8. 用户确认：展示下沉 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13，644b269 单提交）：

1. 确认坏味道：核对 `guide.vue` 与 `expire.vue` 双页「uni.login → [err,res] 兼容取 code → wechatLogin(guestKey) → track(LOGIN_WECHAT/LOGIN_FAIL) → toast → 400ms switchTab 跳首页」约 30 行逐字复制，差异仅在成功/失败文案（guide「登录成功/登录失败，请重试」、expire「授权成功，数据已保留/授权失败，请重试」）。
2. 影响分析：双页并集行为=同流程+文案差异，裁定 opts 参数化（successText/failText 可选，默认 guide 文案）；tuple 兼容取值（Array.isArray(result)）保留 store 层；handleGuest 游客登录仅 guide 单页无副本，裁定保留页内 track(LOGIN_GUEST) 不下沉（ARCH-内聚-007 范围仅微信流程双份复制）。
3. 测试安全网：`npm run type-check`（vue-tsc --noEmit）+ `npm run build:mp-weixin`。
4. 选择架构模式：能力下沉到状态层——store loginByWechat(opts?) 返回 Promise<boolean> 承接 uni.login → code → wechatLogin → token/userInfo/setToken/removeGuestKey → track → toast → 导航全链路，页面只管 loading 与 canSubmit 节流。
5. 迁移执行：store 新增全流程 loginByWechat（旧 code 入参签名替换，全库无其他调用方）；guide handleWechatLogin 与 expire handleAuth 删副本改 `await userStore.loginByWechat(...)`；guide 游客登录保留 track/TRACK_EVENT 导入。
6. 回归：vue-tsc 零错误 + build:mp-weixin 构建绿；uni.login 全库检索唯 store/user.ts 一处，登录链路单一入口，零行为变更。
7. 用户确认：按「重新推送，往后所有任务自动化按推荐方案执行，无需再中途问我」既有指令提交 644b269 并推送（首推 Connection was reset / 443 超时，随文档收尾一并重推），写操作门禁通过。

### B-T25 — 文档归属收敛（docs/ 唯一真源）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-层次-002（层次，中） |
| 目标 | 修正 `docsFile/` 拼写，明确 `docs/` 为唯一真源，wiki 定位只读镜像或删除，互引修正 |
| 状态 | 已完成 |

步骤：

1. 确认坏味道：核对 `docsFile/` 7 文件、`docs/knowledge/business-rule/invariants.md:21` 互引、`zhenxinjian.wiki/` 同名副本。
2. 影响分析：确认各文档的时效性（哪份是最新）；目录改名对现有引用（AGENTS.md 等）的影响面。
3. 测试安全网：无代码测试；以"全库无死链引用"为验收（grep `docsFile` 清零）。
4. 选择架构模式：单一真源 + 镜像声明。
5. 迁移计划：改名并批量修正引用 → 内容去重合并 → wiki 声明只读。
6. 增量执行：改名与内容合并分开提交。
7. 回归测绿：全库引用检索无死链；AGENTS.md 导航可达。
8. 用户确认：展示归属裁定与改名 diff，获确认后收尾（写操作门禁）。

执行记录（2026-09-13 完成）：

1. 确认坏味道：docsFile/（原目录名为 dosc 前缀拼写错误）7 文件与 docs/knowledge/ 内容核查——docsFile 为项目自述叙述性文档（项目介绍/技术栈说明/开发规范 + projectFile 四份提取件），docs/ 为 Harness 约束资产，二者归属清晰非副本；zhenxinjian.wiki/ 本地不存在（独立仓库未 clone），裁定出本仓库治理范围。
2. 影响分析：全库 grep 旧名命中 48 处引用跨 32 文件（AGENTS/Constitutions/README、docs 知识库 8 件、openspec 归档 7 件、sql 注释 9 件、代码注释 3 件、openmole 2 件）。
3. 测试安全网：无代码测试；验收 = grep 旧名清零 + mvn compile + vue-tsc 双绿。
4. 架构模式：单一真源 + 归属标记（.gitattributes docsFile/** attribution=project-narrative + 提交约定 [docsFile] 标记成文）。
5. 迁移执行：git mv 保历史更名 + 32 文件 48 处引用批量修正 + .gitattributes 新建，单提交。
6. 回归：grep 旧名零命中；mvn -q compile 绿；npm run type-check（vue-tsc）绿；AGENTS.md 导航可达指向新名。
7. 用户确认：按「重新推送，往后所有任务自动化按推荐方案执行，无需再中途问我」既有指令提交并推送（写操作门禁通过）。

### B-T26 — 自定义食物列表加上限/分页

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-001（边界，中） |
| 目标 | `CustomFoodService.listMine` 加 LIMIT（如 200）或分页，消除无上限查询 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `service/impl/CustomFoodService.java:86-93`。
2. 影响分析：确认小程序端列表页消费方式（一次性渲染还是滚动加载），决定 LIMIT 还是分页参数。
3. 测试安全网：为该查询补测试（超限截断/分页边界）。
4. 选择架构模式：有界查询 — 与全库其他列表接口风格对齐。
5. 迁移计划：单点改造，一次完成。
6. 增量执行：一批提交。
7. 回归测绿：后端 `mvn -q test` 全绿；小程序自定义食物列表页回归。
8. 用户确认：展示改造 diff，获确认后收尾（写操作门禁）。

### B-T27 — 提醒推送窗口匹配 + 漏发补偿

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-006（边界，中） |
| 目标 | 分钟全等匹配改窗口匹配（如 ±2 分钟未发即补），发送日志去重，漏发可补偿 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `task/ReminderPushTask.java:90-101,127-134` 分钟全等与"失败仅写日志不重试"注释。
2. 影响分析：确定窗口宽度与去重键（提醒 id + 日期）；评估对发送日志表的查询压力。
3. 测试安全网：为窗口匹配与去重补测试（命中窗口内/窗口外/已发送重复扫描）。
4. 选择架构模式：窗口补偿 — 扫描"到点未发"记录补发，幂等去重。
5. 迁移计划：改匹配逻辑 → 补去重 → 日志验证。
6. 增量执行：一批提交（建议在 B-T11 完成后执行，同文件）。
7. 回归测绿：后端 `mvn -q test` 全绿；模拟调度延迟场景验证补发。
8. 用户确认：展示窗口匹配与去重 diff，获确认后收尾（写操作门禁）。

### B-T28 — JWT 密钥门禁改内容检测

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-007（边界，中） |
| 目标 | 启动门禁从"仅 prod profile"改为检测密钥内容（含弱默认特征即拒）或强制环境变量注入 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `application.yml:56` 明文默认密钥、`SecurityStartupChecker.java:24,50` 仅 prod 拒绝。
2. 影响分析：确认各环境当前密钥注入方式；改造后对开发环境启动的影响（开发默认值是否触发拒绝）。
3. 测试安全网：为启动检查器补测试（弱密钥拒绝/强密钥通过/非 prod 命名也拒绝）。
4. 选择架构模式：内容门禁 — 检测密钥含 `change-in` 等弱特征或长度不足即启动失败。
5. 迁移计划：改检查器 → 默认值改占位符 → 开发环境配置同步。
6. 增量执行：一批提交。
7. 回归测绿：后端 `mvn -q test` 全绿；本地启动验证正常、弱密钥模拟启动被拒。
8. 用户确认：展示门禁改造 diff，获确认后收尾（写操作门禁）。

### B-T29 — 小程序全局样式抽象 + 超大页拆分

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-模块化-001（模块化，低） |
| 目标 | `.panel/.btn-primary/.modal` 等公共样式提取全局类；7 个 >400 行页面按组件拆分 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 7 个超大页面清单（最大 `record/index.vue` 687 行）与样式重复样本。
2. 影响分析：盘点重复样式类的差异（确认是否可无损全局化）；确定拆页优先级（先最大页面）。
3. 测试安全网：无单测；以视觉回归清单（每页截图对照）为安全网。
4. 选择架构模式：全局样式层（app.scss/uni.scss）+ 页面子组件化。
5. 迁移计划：先提取全局样式类并逐页替换 → 再按业务块拆子组件（每页一次提交）。
6. 增量执行：样式与拆页分开提交，逐页推进。
7. 回归测绿：`npm run check` 绿；视觉回归清单对照无走样。
8. 用户确认：展示样式收敛与拆页 diff，获确认后收尾（写操作门禁）。

### B-T30 — 原型小程序工程移出版本库

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-模块化-002（模块化，低） |
| 目标 | `MRD-PRD/小程序工程骨架（完整版·zip）/miniprogram/` 85 文件移出版本库，仅保留 PRD 文档 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `MRD-PRD/小程序工程骨架（完整版·zip）/miniprogram/`（19 页面 + 3 云函数）与 `git ls-files MRD-PRD` 95 文件。
2. 影响分析：确认原型无活跃引用（B-T13 的常量原型副本随本任务一并退库）；确定移出去向（本地归档或 release 附件）。
3. 测试安全网：不适用代码测试；确认删除后构建无引用断裂。
4. 选择架构模式：仓库瘦身 — 需求产物与应用代码分离。
5. 迁移计划：`git rm -r` 原型目录 → 补 .gitignore 防复发 → PRD 文档保留。
6. 增量执行：一批提交。
7. 回归测绿：三端构建/测试无影响。
8. 用户确认：展示退库清单，获确认后收尾（写操作门禁）。

### B-T31 — 管理后台视图组件拆分 + echarts 按需

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-模块化-003（模块化，低） |
| 目标 | users/foods 视图弹窗与抽屉拆独立组件；dashboard echarts 按需引入 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `view/users/index.vue:1-417`、`foods/index.vue:1-340`、`dashboard/index.vue:7`。
2. 影响分析：确定组件拆分粒度（编辑弹窗/详情抽屉/表单弹窗）；echarts 按需模块清单（按实际使用图表类型）。
3. 测试安全网：`npm run build` 为安全网；users/foods CRUD 与 dashboard 渲染回归。
4. 选择架构模式：视图-组件分层 + 按需导入。
5. 迁移计划：拆 users → 拆 foods → echarts 按需（各一批提交）。
6. 增量执行：按视图小步提交。
7. 回归测绿：`npm run build` 绿（对比产物体积下降）；页面功能回归。
8. 用户确认：展示拆分与按需导入 diff，获确认后收尾（写操作门禁）。

### B-T32 — 白名单精确匹配 + Token 存储评估

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-002（边界，低） |
| 目标 | `AUTH_SKIP_URLS` 改精确/前缀匹配；评估 Token 存储介质（sessionStorage + 短会话） |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `front/src/store/user.ts:24`、`api/request.ts:17,38,40-43`。
2. 影响分析：确认精确匹配后的白名单集合；评估存储介质变更对刷新保持登录的影响。
3. 测试安全网：`npm run build` + 登录/免登录路径回归。
4. 选择架构模式：白名单精确化；存储介质按评估结论执行（可仅出评估结论不改实现）。
5. 迁移计划：白名单改造先行；存储评估结论成文后再决定是否改造。
6. 增量执行：分开提交。
7. 回归测绿：`npm run build` 绿；免鉴权路径回归。
8. 用户确认：展示改造/评估结论，获确认后收尾（写操作门禁）。

### B-T33 — 埋点接口限流

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-003（边界，低） |
| 目标 | `/track/events` 增加按用户/IP 的 Redis 计数限流 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `application.yml:94` permitAll 与 `TrackEventService.java:38-59`。
2. 影响分析：确定限流阈值与维度（游客无 userId 用 IP）；Redis 计数 key 设计（含 TTL）。
3. 测试安全网：为限流逻辑补测试（阈值内放行/超限拒绝/窗口过期恢复）。
4. 选择架构模式：简单计数器限流（Redis INCR + EXPIRE）。
5. 迁移计划：单点改造，一次完成。
6. 增量执行：一批提交。
7. 回归测绿：后端 `mvn -q test` 全绿；超限场景手工验证。
8. 用户确认：展示限流实现 diff，获确认后收尾（写操作门禁）。

### B-T34 — 小程序发布就绪性补齐（隐私/升级）

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-边界-008（边界，低） |
| 目标 | appid 经环境变量登记；补 `__usePrivacyCheck__` 隐私流程与 `uni.getUpdateManager` 升级检查 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `manifest.json:29`（appid 空串）、`App.vue:10-17`、`pages/mine/index.vue:128-129` 静态文案。
2. 影响分析：确认微信隐私协议接口当前版本要求；updateManager 的时机（onLaunch）。
3. 测试安全网：`npm run check` + 真机/开发者工具验证隐私弹窗与更新提示。
4. 选择架构模式：发布态检查单 — 合规流程与升级流程入 App 生命周期。
5. 迁移计划：appid 登记 → 隐私流程 → updateManager，三步分开提交。
6. 增量执行：按步提交。
7. 回归测绿：`npm run check` 绿；开发者工具验证两流程触发。
8. 用户确认：展示发布就绪改造 diff，获确认后收尾（写操作门禁）。

### B-T35 — tools 路径解耦 + 临时残渣清理

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-006（演进，低） |
| 目标 | 工具脚本路径改环境变量/相对定位；`.gitignore` 补 `!*.example` 豁免；清理根目录 tmp-* 残渣与 `.codegraph/` 空壳 |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `tools/env/jdk25.cmd:4-5`、`mvn25.cmd:6`、`tools/README.md:18-22`、`.gitignore:111,77-79`、`.codegraph/.gitignore`、根目录 9 个 tmp-*。
2. 影响分析：确认各脚本的使用者（本机/团队）；环境变量命名约定。
3. 测试安全网：脚本改造后本机演练 `jdk25.cmd`/`mvn25.cmd`/`SqlRunner` 可用。
4. 选择架构模式：环境解耦 — 路径经环境变量注入，脚本内相对定位。
5. 迁移计划：脚本改造 → .gitignore 豁免 → 残渣清理。
6. 增量执行：分开提交。
7. 回归测绿：工具链演练通过；`git status` 干净。
8. 用户确认：展示改造与清理清单，获确认后收尾（写操作门禁）。

### B-T36 — 微信 token 内存态约束登记 ADR

| 字段 | 内容 |
| --- | --- |
| 追溯 | ARCH-演进-009（演进，低） |
| 目标 | 在 ADR/知识库登记"一期单实例约束：WxMaDefaultConfigImpl 内存 token，扩容前须切 Redis 实现" |
| 状态 | 未开始 |

步骤：

1. 确认坏味道：核对 `config/WxMaConfiguration.java:25` 注释。
2. 影响分析：确认登记位置（`docs/knowledge/arch/` ADR 目录）。
3. 测试安全网：不适用代码测试。
4. 选择架构模式：约束显式化（ADR 记录）。
5. 迁移计划：撰写 ADR 条目 + 代码注释互链。
6. 增量执行：一批提交。
7. 回归测绿：不适用；文档引用可达。
8. 用户确认：展示 ADR 内容，获确认后收尾（写操作门禁）。

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-11 | —（未提交） | 初版：依据 badsmells.md v1.1 生成 36 个任务（高 12 / 中 16 / 低 8），ARCH 8 步模板 |
| v1.1 | 2026-09-11 | —（未提交） | B-T01 执行完成：SessionEvictor 抽象 + ws 包/配置/依赖删除，后端回归全绿，状态置已完成 |
| v1.2 | 2026-09-12 | —（未提交） | B-T02 执行完成：GuestMigrationOrchestrator 编排入口 + Ordered 契约 + UserRecordMigrator 缓存失效改 afterCommit + 双调用点收敛，后端回归全绿，状态置已完成 |
| v1.3 | 2026-09-12 | —（未提交） | B-T03 执行完成：后端 MacroConsistencyValidator 单一真源替换三处副本；前端 utils/validate.ts 新增 checkKcalConsistency/kcalFromMacros 替换两页面私有副本；裁定 5000/10000 为不同场景各自与后端一致（非漂移），detail.vue 加锚点注释；后端测试 + vue-tsc 全绿 |
| v1.4 | 2026-09-12 | —（未提交） | B-T04 执行完成：record/index.vue 本地 38 行 progressItems 与 adviceList 手写副本替换为 buildProgressItems/buildAdviceList 单一真源调用，删除 PROGRESS_THRESHOLD/PROGRESS_COLORS 直引；vue-tsc 全绿，状态置已完成 |
| v1.5 | 2026-09-12 | —（未提交） | B-T05 执行完成：全量哈希比对裁定"漂移"仅为 per-IDE 包装差异（/opsx-X↔$openspec-X 占位符 + opsx front-matter name 行），内容零漂移；ui-ux-pro-max 归入 .trae SoT；新增 scripts/sync-ide-skills.ps1 分发脚本（拷贝 + 两类机械变换）；五副本目录入 .gitignore 并 git rm --cached 退库 120 文件；再生字节级回归比对 0 差异，状态置已完成 |
| v1.6 | 2026-09-12 | —（未提交） | B-T06 执行完成：新建 store/taper.ts、store/reminder.ts；taper/plan、reminder/index、mine/index、home/index、menstrual/index 五处页面直调全部改经 store；userStore.logout/abandonGuest 统一编排七大业务 store reset；cycleStore 新增 switchMode 收敛 mode/select.vue 直调 switchDietMode；vue-tsc 每步全绿，pages/components 对 @/api/* 仅剩 type-only import，状态置已完成 |
| v1.7 | 2026-09-12 | —（未提交） | B-T07 执行完成：拆出 GuestCleanupBatchExecutor 独立 Bean 承载 @Transactional 批方法（规避同类自调用事务失效），任务方法只编排批次不再持大事务，新增每批 size/costMs 日志；补 GuestCleanupTaskTest 5 用例，123 测试全绿，状态置已完成 |
| v1.8 | 2026-09-12 | —（未提交） | B-T08 执行完成：schema_migrations 版本表 + SqlRunner 自动建表/查账/SKIP/记账 + --allow-error 移除失败即终止不记账 + 9 脚本幂等守卫（information_schema 条件守卫/IF NOT EXISTS/change2 断点续跑）+ 存量回填脚本 + run-sql.cmd/README 本机路径修正与全新环境引导成文；五场景演练全绿（全链/SKIP/守卫/失败终止/回填），123 测试全绿，状态置已完成 |
| v1.9 | 2026-09-12 | —（未提交） | B-T09 执行完成：calculator.ts 443→66 行，isPlateau 口径分叉雷与 overCheck 等零调用死函数全删，保留 bmr/tdee/macro532Base 标注「仅展示兜底」；format.ts 反向 import 解除；影子专用配置 PERIOD_PHASES/STAGE_532/DayType 一并退库；vue-tsc 全绿，状态置已完成 |
| v1.10 | 2026-09-12 | —（未提交） | B-T10 执行完成：裁定完整基线 — data.sql 重写为全量基线（建库 + 15 张业务表终态与 change0~9 链一致，全 CREATE IF NOT EXISTS 幂等）；步骤一 admin/admin123 哈希出库 + 管理员初始化成文（10dad43），步骤二全量基线与三处文档同步；演练库全链 RECORDED 0~9/重跑 SKIP/账 10 条/表 16 张，123 测试全绿，状态置已完成 |
| v1.11 | 2026-09-12 | —（未提交） | B-T11 执行完成：ScheduleConfig 多线程调度池（taskScheduler 池 4 线程）+ reminderPushExecutor 外呼执行器（core2/max4/queue200）；微信订阅消息无批量 API 裁定限并发异步——ReminderPushTask 扫描循环改投递执行器，Executor 按构造参数名注入（javap 验证）；补 ReminderPushTaskTest 8 用例 D4 全分支，131 测试全绿；dev 冒烟启动成功，状态置已完成 |
| v1.12 | 2026-09-12 | —（未提交） | B-T12 执行完成：裁定「恒 200 + 响应 data 携带剔除码列表」契约替代 40901 整批拒收——TrackEventService 逐条白名单校验毒条目剔除/合法落库/去重保序返回，TRACK_EVENT_INVALID_CODE 常量删除；TrackEventEnum 新增 track_queue_saturated；前端成功整批移除毒条目不再无限重试 + 队列 ≥80% 饱和告警一次回落复位；TrackEventServiceTest 5 用例新契约，132 测试全绿、vue-tsc 通过；openspec spec 同步，状态置已完成 |
| v1.13 | 2026-09-12 | —（未提交） | B-T13 执行完成：全量比对裁定零漂移（活动系数/缺口档/碳循环系数/录入区间前后端一致、经期上浮 VO 下发无前端副本、热门清单同序）；constants.ts 四块锚点注释 + MenstrualPhaseEnum 失效引用清理；application-dev.yml.example jetcache uri 改 ${spring.data.redis.*} 单一属性源拼接；原型副本删除并入 B-T30 协同；132 测试全绿、vue-tsc 通过、dev 冒烟验证空密码拼接 uri 零异常，状态置已完成 |
| v1.14 | 2026-09-13 | —（未提交） | B-T14 执行完成：逐项全库检索裁定零引用后纯删除——小程序四死函数（getCaptcha/login/register/guestRenew）+ 三死类型 + 白名单三死路由 + foods.ts searchFoods/groupByCategory 死函数 + defaultMealType 下移唯一消费方 record/add.vue；管理后台 register/RegisterRequest/getUserById + 白名单 /auth/register + VITE_WS_* + ws:true + notify.wav + ws 注释；后端 LOGIN_FAIL_MAX；三端安全网全绿（uniapp vue-tsc、front vue-tsc+vite build、mvn test 132），14 文件 +18/-119，状态置已完成 |
| v1.15 | 2026-09-13 | —（未提交） | B-T15 执行完成：三批治理——后端 pom 6 处 starter + maven 插件去显式版本回归 parent BOM、lombok 回归 BOM 1.18.46、byte-buddy 死声明删除、hutool-all 收敛 core/json/extra/captcha 四模块；小程序 TS ^4.9.4→^5.4.5 + vue-tsc ^1.0.24→^2.0.19 对齐 front + @vue/tsconfig ^0.1.3→^0.7.0 消除 TS5102；管理后台 overrides brace-expansion 手钉删除自然解析 2.1.4（CVE 已修复）；三端安全网全绿（mvn test 132 + dependency:tree 无意外降级、uniapp type-check + build:mp-weixin、front build），状态置已完成 |
| v1.16 | 2026-09-13 | —（未提交） | B-T16 执行完成：新建 config/track-events.ts 与后端 TrackEventEnum 22 码一一互锚（键名即枚举名、改动须先改后端枚举）；track() 入参收窄 TrackEventCode 联合类型编译期防拼写漂移；16 文件 31 处字面量全量改引常量（零拼错零缺漏，mode_select 裁定为后端保留码）；vue-tsc 零错误 + build:mp-weixin 构建绿，单提交 0c1ff70，状态置已完成 |
| v1.17 | 2026-09-13 | —（未提交） | B-T17 执行完成：request.ts 删除 useUserStore 反向依赖，新增 AuthHooks 回调注入契约（onSessionClear/onTokenRefreshed）+ setAuthHooks 注入器，401 与游客到期两路径改走 hooks（storage 清理与 reLaunch 保留本层）；main.ts 组合根 pinia active 后装配 user store 闭包；store/user.ts 新增 syncToken/clearSession 两最小 action（401 被动路径语义不变）；BASE_URL 双轨收敛 request.ts 单一来源、track.ts 引用（15s/10s 超时差异裁定保留）；api 层 grep 零 store 依赖、vue-tsc 零错误、build:mp-weixin 绿且 Circular chunk 警告消除，单提交 311ac2d，状态置已完成 |
| v1.18 | 2026-09-13 | —（未提交） | B-T18 执行完成：管理后台三角循环解耦——request.ts 删 router+useUserStore 双反向 import，新增 AuthHooks 契约（getToken/onTokenRefreshed/onSessionClear，与 B-T17 小程序侧同模式）+ setAuthHooks 注入器，请求拦截 token 改经注入（不再直读 localStorage）、续期与 401 双路径走 hooks；store/user.ts 收敛 clearSession 单一入口（不含导航）+ syncToken，删 router import；守卫登录态改经 userStore.token、双处 removeItem 改 clearSession；main.ts 组合根装配（清会话+跳登录在此编排）；MainLayout logout 后补 router.push；依赖收敛 router→store→api 单向无环，npm run build（vue-tsc + vite）全绿，单提交 541e7b4，状态置已完成 |
| v1.19 | 2026-09-13 | —（未提交） | B-T19 执行完成：依赖风格裁定「实现类直依赖为主 + 接口化豁免清单」（多实现/替换点 SessionEvictor/GuestDataMigrator/StorageService、跨层稳定契约 UserService/WechatAuthService）与「controller/task 禁止直依赖 Mapper」规约成文（开发规范 §2.1 + java/standard.md §3，豁免 FoodLibraryInitializer/GuestCleanupBatchExecutor 两基础设施类）；ReminderPushTask 四个 Mapper 裸查询全部下沉——ReminderService.scanDueReminders/hasSuccessPushToday、DietRecordService.hasRecord、UserService.getById，任务只编排与外呼；测试同步切换（ReminderPushTaskTest 8 用例改桩 service 层 + 新增 ReminderServiceTest 2 用例 + DietRecordServiceTest 补 hasRecord 2 用例），mvn test 136 用例全绿，单提交 81cba5f，状态置已完成 |
| v1.20 | 2026-09-13 | —（未提交） | B-T20 执行完成：裁定「URL 仅带 id + add 页从 diet store 当日分组按 id 回显」（dayData.meals[].records 本就缓存完整 DietRecordVO，编辑入口仅在列表页数据已在内存，非独立编辑态容器无刷新丢态面）；record/index.vue handleEdit 双分支全量 encodeURIComponent 拼接收敛单一 editId；add.vue onLoad 编辑分支删逐项 decode 改 store 查找回显（餐别/备注/三宏热量直取 VO、食物来源 foodStore.detail + amountG、中文备注不再经 URL 编解码，深链找不到 toast 引导返回）；encode/decodeURIComponent 检索清零，vue-tsc 零错误，单提交 2724839，状态置已完成 |
| v1.21 | 2026-09-13 | —（未提交） | B-T21 执行完成（补登版本行，08f1a40 收尾时漏登）：f6c43c4 批 1 三处小重复收敛公共工具（round1 三处私有副本 → common/utils/Numbers.round1；operator() 六处 "user:"+userId 副本 → common/utils/Operators.user，AdminFoodService 无参 operator() 管理员用户名不同语义裁定保留；CORS origin env 解析双份 → common/utils/CorsOrigins.parse，过滤语义保留调用方本层）；641034b 批 2 拆汇总职责（新建 DietSummaryService 承接 summary() 整方法，DietRecordService 删 UserBodyMapper/CyclePlanService/Taper532Service 三依赖回归 CRUD+按日分组+hasRecord 单一职责，跨调网 Cycle/Taper 单向依赖零回边 DAG 无环，DietController 委派 /diet/summary）；守恒校验已由 B-T03 MacroConsistencyValidator 收口无需再拆；DietRecordServiceTest 15→11 + 新增 DietSummaryServiceTest 5 用例原样迁移，136 用例全绿，状态置已完成 |
| v1.22 | 2026-09-13 | —（未提交） | B-T22 执行完成：模式真源裁定档案 profile.mode——bodyStore 新增 currentMode（未建档默认 532）/isCycleMode 派生 getter，summary.mode 显示源废止，home:43/record:75（summary.mode===2）与 mine:50/mode-select:26（profile?.mode ?? 1）双源四页归一；空态三分支收敛 dietStore.showBodyEmpty/showCycleEmpty（summary.recorded=false 按档案 recorded 与 isCycleMode 分流），home/record 双页内联判定删除改委派，home 页 loaded 前置随 noProfile getter 内聚吸收；record:177 直引 store getter 与 showMealEmpty 页独有口径裁定保留；vue-tsc 零错误、双源检索清零，单提交 15d02c5，状态置已完成 |
| v1.23 | 2026-09-13 | —（未提交） | B-T23 执行完成：四批治理——0634cc0 批 1 骨架三件套（constants/dicts.ts 字典层 DictItem+dictMap+dictLabel 十组枚举互锚 / composables/usePageQuery.ts 查询骨架七处样板托管 / component/PagePager.vue 通用分页条 / global.css 三公共类）；5cc81db 批 2 users 页迁移（七处样板收敛 + 角色/状态/模式/活动/周期五本地字典收敛 + 分页器副本改 PagePager + 四 scoped 副本改全局类 + usePageQuery 复用 api/types.ts PageResult）；4dbcba3 批 3 foods 页迁移 + API 类型收敛（七处样板收敛 + 分类/来源/状态三本地字典收敛 + categoryName() 删除改 dictLabel + adminFood/adminDiet 返回类型收敛 PageResult<T>）；d965e95 批 4 diet-records 页迁移（七处样板收敛 + MEAL_MAP/SOURCE_MAP 两本地字典收敛 + displayUser 参数类型收窄 + 只读页无删后回退）；四批各自 vue-tsc + vite build 全绿，三页样板全量收敛骨架与字典层单一真源零行为变更，状态置已完成 |
| v1.24 | 2026-09-13 | —（未提交） | B-T24 执行完成：微信登录流程下沉——guide/expire 双页「uni.login → [err,res] 兼容取 code → wechatLogin(guestKey) → track → toast → 400ms 跳首页」约 30 行逐字复制收敛 store/user.ts loginByWechat(opts?) 单一入口（返回 Promise<boolean>，tuple 兼容取值保留 store 层，successText/failText 参数化保留双页并集文案）；双页删副本改 await store 调用只管 loading 与 canSubmit 节流；handleGuest 游客登录单页无副本裁定保留页内 track(LOGIN_GUEST)；uni.login 全库检索唯 store 一处，vue-tsc 零错误 + build:mp-weixin 绿，单提交 644b269，状态置已完成 |
| v1.25 | 2026-09-13 | —（未提交） | B-T25 执行完成：文档归属收敛——原拼写错误目录（dosc 前缀）git mv 更名 docsFile 保历史，全库 32 文件 48 处引用批量修正（AGENTS/Constitutions/README + docs 知识库 8 件 + openspec 归档 7 件 + sql 注释 9 件 + 代码注释 3 件 + openmole 2 件），grep 旧名清零；新建 .gitattributes 归属标记（docsFile/** attribution=project-narrative + 提交约定 [docsFile] 标记成文）；内容去重裁定——docsFile 叙述性文档与 docs/ Harness 约束资产归属清晰非副本不合并；zhenxinjian.wiki 本地不存在裁定出本仓库范围；mvn compile + vue-tsc 双绿，单提交，状态置已完成 |

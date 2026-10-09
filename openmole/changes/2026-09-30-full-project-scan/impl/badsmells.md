# Badsmells — 2026-09-30-full-project-scan（IMPL 实现级）

- **级别**：IMPL（实现级）
- **范围**：仓库全量（后端 + 小程序 + 管理后台三端）
- **提交版本**：`4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`
- **识别方式**：三端三路并行静态侦察（IMPL-COMMON + Java/TypeScript/Vue 语言特定惯用法）；本级别为首次扫描
- **检出语言**：Java、TypeScript、Vue SFC

---

## §2.0 索引

| BS-ID | 类别 | 严重度 | 状态 |
| --- | --- | --- | --- |
| IMPL-安全-001 | 安全 | 高 | 已消除 |
| IMPL-安全-002 | 安全 | 中 | 已消除 |
| IMPL-语言惯用法-001 | 语言惯用法 | 中 | 已消除 |
| IMPL-语言惯用法-002 | 语言惯用法 | 中 | 已消除 |
| IMPL-语言惯用法-003 | 语言惯用法 | 中 | 已消除 |
| IMPL-语言惯用法-004 | 语言惯用法 | 中 | 已消除 |
| IMPL-语言惯用法-005 | 语言惯用法 | 低 | 已消除 |
| IMPL-魔法数字-001 | 魔法数字 | 中 | 已消除 |
| IMPL-异常-001 | 异常 | 低 | 已消除 |
| IMPL-空值-001 | 空值 | 低 | 未清除 |
| IMPL-硬编码-001 | 硬编码 | 中 | 未清除 |
| IMPL-硬编码-002 | 硬编码 | 中 | 未清除 |
| IMPL-参数-001 | 参数 | 低 | 未清除 |
| IMPL-类型-001 | 类型 | 低 | 未清除 |
| IMPL-死代码-001 | 死代码 | 低 | 未清除 |

状态说明：**未清除** / 已消除 / 部分残余。共 15 条（高 1 / 中 8 / 低 6）。

---

## §2.1 明细

### IMPL-安全-001 — `.last("LIMIT " + var)` 字符串拼接 SQL（受控 int 仍无上界风险面）

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 安全 |
| 严重度 | 高 |
| 症状 | 多处 MyBatis-Plus `.last()` 以字符串拼接 `LIMIT`；当前变量均为受控 int 故无实际注入，但部分 `limit` 来自请求参数且无上界，且拼接绕过类型安全通道，一旦参数源将来变为字符串即升级为注入 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/ReminderService.java:82`、`WeightService.java:124`（`limit` 来自 `@RequestParam` 无 `@Max`）、`AdminStatsService.java:118,146`（`limit` 经 `checkLimit` 1..50 有界）、`AdminDietService.java:76`、`CustomFoodService.java:111`、`task/GuestCleanupBatchExecutor.java:46`（`batchSize` 常量） |
| 根因 | 团队惯用 `.last()` 快捷限流，未统一走分页插件/占位参数 |
| 影响 | `.last()` 属原始 SQL 拼接，与「避免 `${}` 拼接」约定精神相悖；`WeightService.list` 的 `limit` 无上界可传 `Integer.MAX_VALUE` |
| 修复建议 | `WeightController` limit 加 `@Max` 或 service 内封顶；有界场景统一收敛到 `boundedSelect(wrapper, limit)` 封装或 `Page` 分页机制 |

### IMPL-安全-002 — 管理后台用户列表返回未脱敏原始拷贝

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 安全 |
| 严重度 | 中 |
| 症状 | `pageUsers()` 直接 `BeanUtil.copyProperties(user, UserVO.class)` 不脱敏，admin 列表接口返回 `phone`/`wechatOpenid`/`wechatUnionid`/`wechatNickname`/`wechatAvatar`；而 `getUserById` 对非 admin 走 `maskPublicUser` 只隐藏了 openid/unionid/remark，未隐藏 `phone` |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/UserServiceImpl.java:129-130`（pageUsers 无 mask） vs `:144-147`（getUserById mask）、`:263-267`（maskPublicUser 未处理 phone） |
| 根因 | 脱敏逻辑未做单一 `toUserVO(User, boolean masked)` 工厂，各查询点各自实现 |
| 影响 | 隐私字段泄露面不一致；`phone` 在脱敏与非脱敏路径口径不统一 |
| 修复建议 | 抽单一 `toUserVO` 工厂，`pageUsers` 与 getUserById 统一按 admin 判定脱敏，默认隐藏 `phone` |

### IMPL-语言惯用法-001 — `.setSql` 硬编码列名自增/自减

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 语言惯用法 |
| 严重度 | 中 |
| 症状 | 订阅额度自增/自减用 `.setSql("subscribe_credit = subscribe_credit + 1")` / `- 1`，列名与运算写死在字符串，绕过 MyBatis-Plus 字段映射，编译期重构不可见 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/ReminderService.java:151,167` |
| 根因 | 直接 SQL 运算避免「读改写」并发丢更新 |
| 影响 | 列漂移（改列名）会静默失效；绕过字段映射不可重构 |
| 修复建议 | 列名抽常量；或改用独立 mapper increment update |

### IMPL-语言惯用法-002 — MySQL 方言 `CAST(SUBSTRING(...) AS UNSIGNED)` 不可移植

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 语言惯用法 |
| 严重度 | 中 |
| 症状 | `AdminFoodService.nextCode()` 用 MySQL 特有 `ORDER BY CAST(SUBSTRING(code,2) AS UNSIGNED) DESC LIMIT 1` 生成食物编号，PostgreSQL/H2 下失效 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/AdminFoodService.java:176` |
| 根因 | 编号生成依赖 MySQL 函数式排序 |
| 影响 | 数据库迁移即失效；UT/IT 无法覆盖此路径 |
| 修复建议 | 编号生成抽 `CodeSequenceService`，用表级/分布式自增替代函数式排序；若必须，mapper XML 用 `databaseId` 区分方言 |

### IMPL-语言惯用法-003 — 角色静默降级吞并真实角色

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 语言惯用法 |
| 严重度 | 中 |
| 症状 | 认证过滤器把非 ADMIN 一律置为 ROLE_USER，库里 role 为非法值（如误写 "superadmin2"）也被静默降级，掩盖脏数据 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/security/JwtAuthenticationFilter.java:122-125`（`String role = StrUtil.blankToDefault(...); if (!ROLE_ADMIN.equals(role)) role = ROLE_USER;`） |
| 根因 | 防御式默认值把「未知角色」与「普通用户」混为一谈 |
| 影响 | 库中角色误写不被发现，越权风险被掩盖；未来新增角色（如运营）被强制降级 |
| 修复建议 | 白名单校验：ADMIN 保持，其余按库值映射，非法值显式 WARN 并拒绝或走默认，勿静默改写 |

### IMPL-语言惯用法-004 — 时间字段手工 set 与 MetaObjectHandler 自动填充分裂

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 语言惯用法 |
| 严重度 | 中 |
| 症状 | 多处业务手工 `setUpdateTime(LocalDateTime.now())`、`setSubmitTime(...)`、`setArchivedAt(...)`，与 `MybatisPlusConfig` 的 MetaObjectHandler（strictInsertFill/strictUpdateFill）职责重复，形成「有的靠自动填、有的靠手动」不一致 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/BodyProfileService.java:146`（setUpdateTime）、`AdminFoodService.java:121,143`（setSubmitTime）等 |
| 根因 | `strictUpdateFill` 仅空字段生效，查询出的旧值不覆盖，开发者被迫手动刷新 |
| 影响 | 时间语义不统一（精度与是否覆盖各异），审计字段可能缺失或重复写 |
| 修复建议 | 更新统一经 `updateWrapper.set(updateTime, ...)` 或在 Handler 内对非空 updateTime 也强制刷新，消除手动 set |

### IMPL-语言惯用法-005 — 缓存值体积校验以 JSON 序列化近似

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 语言惯用法 |
| 严重度 | 低 |
| 症状 | `CacheValueGuard.check()` 每次缓存读写路径对 value 做整对象 JSON 序列化估算体积，拿「序列化字节数」当体积上限 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/common/cache/CacheValueGuard.java:28`（`JSONUtil.toJsonStr(value).getBytes().length`） |
| 根因 | 无「内存体量估算」现成手段，以串化近似 |
| 影响 | 大对象缓存命中时额外 CPU/GC 开销；串化字节与 JetCache `valueEncoder=java` 实际编码不一致，阈值失真 |
| 修复建议 | 改字段级估算，或仅对已知大对象类型校验；阈值与 valueEncoder 实际字节对齐 |

### IMPL-魔法数字-001 — 业务阈值/容量魔法数字内联散落

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 魔法数字 |
| 严重度 | 中 |
| 症状 | 调度池/外呼执行器/批大小等容量数字以裸字面量散落在配置类与任务类（未抽为具名常量或 `@ConfigurationProperties`），且阈值常量互不关联（如多处 `0.3`/`4`/`200` 语义各异但同值） |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/config/ScheduleConfig.java:47-49`（`setCorePoolSize(2)/setMaxPoolSize(4)/setQueueCapacity(200)` 魔法数字）、`task/GuestCleanupTask.java:23`（`BATCH_SIZE = 200`）、`ReminderService.java` 截断 255 等裸值散落；对照 `WeightService.java:47-58`（已具名常量，但 PLATEAU_DELTA/RESTORE_DELTA 均为 0.3 语义不同仅靠命名区分） |
| 根因 | 阈值直接内联或就近分散，未集中到 `CommonConstant`/`@ConfigurationProperties` |
| 影响 | 跨模块重复定义（多个 0.3 语义不同却同名），调整阈值易漏改 |
| 修复建议 | 抽到 `CommonConstant` 或 `@ConfigurationProperties`，阈值集中可配 |

### IMPL-异常-001 — 空 catch 吞异常

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 异常 |
| 严重度 | 低 |
| 症状 | `resolveTrendStart()` 解析 `range` 时 `catch (NumberFormatException ignored)` 空 catch，非法 range 静默回默认窗口，无日志 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/WeightService.java:188-190` |
| 根因 | 降级容错有意吞异常，但此处连 debug 级别说明都缺 |
| 影响 | 调用方无法感知 range 非法，排查排障无痕迹 |
| 修复建议 | 空 catch 至少补 debug 说明；`resolveTrendStart` 补 WARN |

### IMPL-空值-001 — null 返回表示空态（null 传播）

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 空值 |
| 严重度 | 低 |
| 症状 | `FoodCategoryEnum.of(code)` 等用 `return null` 表示「未匹配」，调用方须处处判空；「null=不存在」与「null=数据异常」语义混淆 |
| 证据 | `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/common/enums/FoodCategoryEnum.java:91-100` |
| 根因 | 以 null 为空态惯例，未用 Optional |
| 影响 | 漏判即 NPE |
| 修复建议 | 枚举 `of` 用 `Optional.ofNullable` 包装，或空态统一 VO 标记 |

### IMPL-硬编码-001 — 管理后台色值硬编码绕过 CHART_PALETTE

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 硬编码 |
| 严重度 | 中 |
| 症状 | 品牌绿 `#00AC7C`、琥珀 `#FFB020` 在 dashboard 的 `areaGradient('#00AC7C')`/`areaGradient('#FFB020')` 与 KPI 图标渐变处反复硬编码，而 `utils/echarts-theme.ts` 已定义 `CHART_PALETTE`（`['#00AC7C', '#FFB020', ...]`）却未在 series 色处引用 |
| 证据 | `apps/zhenxinjian-front/src/view/dashboard/index.vue:76,85,293-305` vs `apps/zhenxinjian-front/src/utils/echarts-theme.ts:9`（`CHART_PALETTE`） |
| 根因 | 色板、CSS 设计令牌、组件内联渐变三套体系未统一，新页就近写死具体色值 |
| 影响 | 品牌色调整须跨文件查找；`CHART_PALETTE` 单处维护的努力被绕过，色值漂移风险 |
| 修复建议 | series 色统一从 `CHART_PALETTE[0]/[1]` 取；渐变复用 CSS 变量或集中 theme 导出 |

### IMPL-硬编码-002 — 小程序品牌色 scss/ts/json 三处硬编码

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 硬编码 |
| 严重度 | 中 |
| 症状 | 品牌绿 `#00AC7C` 在 TS 组件常量、SCSS token、pages.json 三处各维护一份，无共享 TS 色板；`switch`/`checkbox` 模板内 `color="#00AC7C"` 亦硬编码 |
| 证据 | `apps/zhenxinjian-uniapp/src/components/EnergyRing.vue:28-29`（`LEAF='#00AC7C'`）、`config/constants.ts:87`（色值）、`pages.json`（json 内 `#00AC7C`）、`pages/reminder/index.vue:238`（`color="#00AC7C"`） |
| 根因 | SCSS token 与 TS 无共享常量，JS 侧无法用 SCSS 变量 |
| 影响 | 品牌色调整须同步多处，漏改即色差 |
| 修复建议 | 建单一 TS 色板（`BRAND_COLORS`），组件/模板 `:color` 引用；pages.json 注释标注与 uni.scss 对齐 |

### IMPL-参数-001 — picker 事件用 `any` 丢类型保护

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 参数 |
| 严重度 | 低 |
| 症状 | `handleEnergyUnitChange(e: any)`、`handleCategoryChange(e: any)` 未用 uni 具体类型签名，`e.detail.value` 隐式 any，误用不报错 |
| 证据 | `apps/zhenxinjian-uniapp/src/pages/food/custom-edit.vue:80,97` |
| 根因 | picker 事件类型未收敛 |
| 影响 | 丢失类型保护 |
| 修复建议 | 改 `e: { detail: { value: number } }` 或抽 `utils/uni-types.ts` 统一 `SwitchChangeEvent`/`PickerChangeEvent` 窄化类型 |

### IMPL-类型-001 — 列表页 query 未受 PageQueryBase 约束

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 类型 |
| 严重度 | 低 |
| 症状 | `usePageQuery<T, Q extends PageQueryBase>` 已定义 `PageQueryBase`，但各列表页 `query` 均以 `reactive({ page, size, ... })` 字面量定义，未绑定接口，筛选字段类型靠逐个 `as number | undefined` 手写补丁 |
| 证据 | `apps/zhenxinjian-front/src/composables/usePageQuery.ts:10-13` vs `apps/zhenxinjian-front/src/view/users/index.vue:22-28`、`view/foods/index.vue:35-44` |
| 根因 | composable 泛型已定义基座，调用侧未显式声明满足契约 |
| 影响 | `page/size` 基础字段缺少编译期一致性保障 |
| 修复建议 | 各页 `interface UserListQuery extends PageQueryBase {...}` 显式传入 `usePageQuery`，淘汰逐个 `as` 断言 |

### IMPL-死代码-001 — store isLoggedIn 导出零消费

| 字段 | 内容 |
| --- | --- |
| 级别 | IMPL |
| 类别 | 死代码 |
| 严重度 | 低 |
| 症状 | `useUserStore` 定义并导出 `isLoggedIn`，但全仓库无消费点（登录态判断实际均直读 `token`） |
| 证据 | `apps/zhenxinjian-front/src/store/user.ts:17,55` |
| 根因 | 登录态判断实际走 `token`，便捷方法残留 |
| 影响 | 死代码膨胀公共 API，误导调用方 |
| 修复建议 | 若无用途删除；若保留改 `computed(() => !!token.value)` getter 并真正接入守卫/MainLayout |

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：IMPL 级首次全项目扫描，检出 15 条（高 1 / 中 8 / 低 6） |
| v1.1 | 2026-10-08 | — | IMPL-语言惯用法-002 置"已消除"（`nextCode` 排序改 `LENGTH(code) DESC, code DESC` 可移植表达式，附顺延测试） |
| v1.2 | 2026-10-09 | — | IMPL-语言惯用法-003 置"已消除"（`JwtAuthenticationFilter` 角色白名单校验，非法值 WARN 后降级 USER） |
| v1.3 | 2026-10-09 | — | IMPL-语言惯用法-004 置"已消除"（`MetaObjectHandler.updateFill` 强制刷新 updateTime，删除业务手动 `setUpdateTime`） |
| v1.4 | 2026-10-09 | — | IMPL-语言惯用法-005 置"已消除"（`CacheValueGuard` 改 JDK 序列化字节估算，与 `valueEncoder=java` 对齐） |
| v1.5 | 2026-10-09 | — | IMPL-魔法数字-001 置"已消除"（`ScheduleConfig` 外呼执行器容量抽具名常量、`ReminderService` 失败原因截断 255 抽 `FAIL_REASON_MAX_LENGTH`） |
| v1.6 | 2026-10-09 | — | IMPL-异常-001 置"已消除"（`WeightAggregateService.resolveTrendStart` 空 catch 补 WARN，非法窗口回退默认 30 天） |
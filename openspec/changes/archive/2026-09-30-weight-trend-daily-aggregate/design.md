# 设计 — 体重随时记录 + 每日末值聚合 + 多窗口趋势曲线

## Context

- `weight_record`（sql/change8_taper_532.sql:35-50）：`user_id / record_date / weight`，索引 `(user_id, record_date, delete_flag)`，**本就无日期唯一约束**；同日多条在表结构层天然支持。
- `WeightService.save`（WeightService.java:85-96）：当前先按 `(userId, recordDate)` 软删再插入 → 每日仅一条活跃；`list`（:118-138）`LIMIT` 上限 100；`isPlateau`（:176-191）直接对窗口内全部原始记录算 max−min。
- 平台下调态持久化于 `user_body.is_adjusted / trigger_weight`，`adjust_log` 仅留痕（taper-532 D3）；数值 −20g/−80kcal 由 `Taper532Service.buildToday` 应用。
- 小程序体重页 `pages/weight/index.vue` 消费 `store/weight.ts`（records 倒序）；uniapp 无内联 SVG，先例 `EnergyRing.vue` / `utils/icons.ts` 用 SVG data-uri 经 `<image>` 渲染。
- 测试：后端 H2 schema 已镜像 weight_record；测试走真实 Mapper。

## Goals / Non-Goals

**Goals：** 同日多条共存；列表全量；每日末值聚合（曲线 + 平台判定统一数据源）；多窗口趋势接口与体重差；小程序多窗口折线。

**Non-Goals：** 不做全自动复盘；不改微调数值（−20/−80）与恢复阈值（≥0.3）；不碰碳循环固化日目标；不做后台体重展示（独立 change）；不为体重记录加来源/备注字段。

## Decisions

### D1 每日末值的定义与取数

**「当日最后一条」= 同 `(user_id, record_date)` 下 `id` 最大者**（自增 id 单调，等价最后插入；不引入时间戳列）。

取数用一条 SQL：子查询 `SELECT MAX(id) FROM weight_record WHERE user_id=? [AND record_date BETWEEN ? AND ?] AND delete_flag=0 GROUP BY record_date`，外层 `SELECT * FROM weight_record WHERE id IN (...)`，再按 `record_date` 升序排序。

- 落点：`WeightRecordMapper.selectDailyLast(Long userId, LocalDate start, LocalDate end)`，用 MyBatis `@Select` 注解写子查询（`@Param` 绑定，禁止 `${}`）；MySQL 8 与 H2 均支持该语法。
- 聚合在数据库完成，Service 不做内存分组（避免「先全量拉取再分组」与 BigKey 式传输）。
- 备选否决：内存分组（数据量大时浪费 IO）；加 `last_flag` 列并在 save 时维护（写入放大 + 并发维护复杂，YAGNI）。

### D2 save：去同日覆盖

删除 WeightService.java:85-88 的软删，直接 insert；校验（25–200、未来日期、一位小数 HALF_UP）保持不变。保存后平台判定的触发参考改用「今日末值」（D4），不再用入参 weight。

### D3 list：去条数上限

`list` 去掉 `Math.min(limit,100)` 与默认 30：页面列表不传 limit 即**不拼接 LIMIT**，返回日期范围（可空）内全部记录，`ORDER BY record_date DESC, id DESC`。保留可选 `limit` 参数（仅当显式传入正整数时拼接，走白名单式整数，沿用现有 `last("LIMIT n")`，n 为已校验的 int）。

- 单用户体重数据量可控（个人数据，非高并发写入），不加上限不构成全表扫描风险；查询始终带 `user_id` 命中索引。

### D4 平台判定与触发参考统一走每日末值

- `isPlateau`：改用 `selectDailyLast(userId, today−6, today)`；按**不同日**点数 <2 返回 false；对每日末值算 max−min<0.3。
- `applyPlatformAdjust`：入参从「本次提交值」改为「今日末值」（save 后调 `selectDailyLast` 取当日点；当日无点理论不发生，兜底用入参）。单次门闩（is_adjusted）、恢复阈值（trigger − 今日末值 ≥0.3）逻辑不变。

### D5 趋势接口与窗口

`GET /weight/trend?range=30`（默认 30；合法值 7/30/60/90/365/all，非法值回退默认 30）：

- 有界窗口 `start = today.minusDays(range-1)`（7→7 天、365→365 天）；`all` 不设 start。end=today。
- 数据 = `selectDailyLast`（升序）。
- 体重差：点数 ≥2 时 `delta = first.weight - last.weight`（正=下降）；同时返回首末日期；点数 <2 时不返回 points/delta。
- 新增 `WeightTrendVO { points:[{date,weight,plateau}], delta, startDate, endDate }`；`plateau` 标记复用当前 isPlateau（前端用于琥珀点）。
- 窗口映射在后端，前端只传 range（口径单一真源，ADR 0004）。

### D6 小程序呈现

- `components/WeightChart.vue`：props 为 `trend: WeightTrendVO`；SVG data-uri 折线（叶绿 #00AC7C、线宽、末点圆点高亮、平台日琥珀点、首尾日期轴、末值标注）；经 `<image>` 渲染，Map 缓存 data-uri。
- 窗口 segment：7/30/60/90/365/all 六键，默认 30；切换调趋势接口。
- 体重差文案「较 M月D日 ±X.Xkg」：delta>0（下降）绿、delta<0（上升）警示色；points<2 时整图与文案不渲染。
- 体重页：录入卡与历史记录之间加「体重趋势」面板；历史列表用无 limit 的 list（展示同日多条）；删除规则（>7 条显示）保留。
- 曲线坐标与 delta 始终 kg；kg/斤 仅作用于录入与历史数值。

## Risks / Trade-offs

- [去 LIMIT 后异常大结果] → 单用户个人数据 + user_id 索引；前端按需传日期范围，风险低。
- [子查询性能] → 命中 `(user_id, record_date)` 索引，MAX(id) 分组数据量小；EXPLAIN 验证留待压测（个人数据无压力）。
- [H2 与 MySQL 方言差异] → 子查询为标准 IN + GROUP BY，两库兼容；Mapper 测试覆盖。
- [旧数据兼容] → 历史每日已仅一条，末值即其本身，零刷数。

## Migration Plan

1. Mapper 加 selectDailyLast（含 H2 测试）
2. Service：save 去覆盖、list 去上限、isPlateau/apply 按日末值、新增 trend
3. Controller：加 /weight/trend、更新描述
4. 小程序：api 类型 + WeightChart + 页面接入 + store
5. 后端 mvn test、前端 H5 Playwright 走查、两端 build
6. 回滚 = git revert（无结构/数据变更）

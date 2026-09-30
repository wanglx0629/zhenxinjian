# 体重随时记录 + 每日末值聚合 + 多窗口趋势曲线 — Tasks

> 标记约定：`[ ]` 未开始 / `[x]` 已完成 / `[~]` 部分完成 / `[-]` 已取消
> 前置阅读：本 change `design.md`（D1–D6）与 `openspec/specs/body/weight/spec.md`

## 后端 — Mapper

- [x] **T-01** `WeightRecordMapper` 新增 `selectDailyLast(@Param("userId") Long, @Param("startDate") LocalDate, @Param("endDate") LocalDate)`：`@Select` 子查询——`SELECT * FROM weight_record WHERE id IN (SELECT MAX(id) FROM weight_record WHERE user_id=#{userId} AND delete_flag=0 [AND record_date >= #{startDate}] [AND record_date <= #{endDate}] GROUP BY record_date)`；`<script>` 动态条件，`#{}` 绑定；返回按 record_date 升序（Service 排序或 SQL ORDER BY）。作者注释

## 后端 — Service / VO

- [x] **T-02** `WeightService.save`：删除同日软删（WeightService.java:85-88），直接 insert 允许多条；校验不变；保存后平台触发参考改用「今日末值」（T-05）
- [x] **T-03** `WeightService.list`：去条数上限——不传 limit 不拼 LIMIT，`ORDER BY record_date DESC, id DESC`；显式传正整数 limit 时才拼；日期范围过滤保留
- [x] **T-04** `WeightService.isPlateau`：改用 `selectDailyLast(userId, today−6, today)`；按不同日点数 <2 返回 false；对每日末值算 max−min<0.3
- [x] **T-05** `applyPlatformAdjust`：触发/恢复参考改为「今日末值」（save 后取 selectDailyLast 当日点，兜底入参值）；单次门闩与恢复 ≥0.3 逻辑不变
- [x] **T-06** 新增 `WeightTrendVO`（points:{date,weight,plateau}[]、delta、startDate、endDate）与 `trend(userId, range)`：range 合法 7/30/60/90/365/all、默认 30、非法回退 30；有界 start=today−(range−1)，all 不设；points 来自 selectDailyLast；点数 ≥2 才算 delta=first−last；<2 不返回 points/delta

## 后端 — Controller

- [x] **T-07** `WeightController`：新增 `GET /weight/trend?range=`；更新 save 描述（同日多条、非覆盖）与 list 描述（不设上限）

## 后端 — 测试

- [x] **T-08** 更新/新增 WeightService + Mapper 测试：同日多条共存且取 MAX(id) 末值；list 无上限全返回；同日多次不干扰平台判定；不足 2 个不同日不判定；trend 各窗口/非法 range/delta 正负/不足两日；下调恢复以今日末值为准
- [x] **T-09** `mvn test`（JAVA_HOME=JDK25）全绿

## 小程序 — API / Store

- [x] **T-10** `api/weight.ts`：新增 `WeightTrendPoint / WeightTrendVO` 类型与 `getWeightTrend(range)`（GET /weight/trend）；listWeights 不传 limit
- [x] **T-11** `store/weight.ts`：新增 trend/range/loading 状态与 fetchTrend；records 全量

## 小程序 — 组件 / 页面

- [x] **T-12** 新增 `components/WeightChart.vue`：SVG data-uri 折线（叶绿、末点高亮、平台日琥珀点、首尾日期轴、末值），经 `<image>` + Map 缓存；props trend；纯 kg
- [x] **T-13** 窗口切换条（7/30/60/90/365/all，默认 30），切换调 fetchTrend；体重差文案「较 M月D日 ±X.Xkg」（下降绿/上升警示）；points<2 不渲染图与文案
- [x] **T-14** `pages/weight/index.vue`：录入卡与历史记录间加「体重趋势」面板；历史列表全量展示同日多条；删除规则保留

## 验收

- [x] **V-A1** 后端 `mvn test` 通过
- [x] **V-A2** 小程序 H5 + Playwright 走查：六窗口切换正确、同日多次仅末值上曲线、列表全展示、delta 正负与口径（最早日末值−最新日末值）、不足两日隐藏
- [x] **V-A3** 两端 build 通过（front/uniapp）
- [x] **V-A4** `openspec validate weight-trend-daily-aggregate` 通过

## 不做

- 全自动复盘；改微调数值/恢复阈值；碳循环固化日目标；后台体重展示（下一 change）；体重来源/备注字段

# 提案 — 体重随时记录（同日多条）+ 每日末值聚合 + 多窗口趋势曲线

## Why

体重链路此前按「每周一空腹称重、同日幂等覆盖、每日至多一条」设计（`WeightService.save` 软删当日记录后插入，`list` limit 上限 100）。现产品口径调整为**随时可称重、同日允许多条**，且需要可视化趋势：

- 用户一天可能多次称重，全部记录要在历史列表完整保留；但趋势曲线每日只应呈现「最后一次」，否则同日抖动会让曲线与平台判定失真。
- 平台判定当前直接对原始记录算 max−min（WeightService.java:188-190），同日多条会污染判定，必须改为按「每日末值」聚合后再判。
- 缺少趋势曲线与多窗口对比；用户无法直观看到一段时间的体重变化。

## What Changes

- **同日不再覆盖**：`save` 删除软删当日记录逻辑，直接插入，每用户每日可多条；「当日最后一条」= 同日 id 最大者。
- **列表不限制条数**：`list` 去掉 `LIMIT` 上限，按日期倒序、同日 id 倒序，完整展示；保留日期范围过滤。
- **新增每日末值聚合查询**：`GROUP BY record_date` 取 `MAX(id)` 回连，专供曲线与平台判定。
- **平台判定改为按日末值**：近 7 天每日末值 max−min<0.3，≥2 个不同日才判定；下调/恢复仍以「今日末值」为参考，单次门闩与恢复阈值不变。
- **新增趋势接口**：`GET /weight/trend?range=7|30|60|90|365|all`，返回窗口内每日末值（时间序）+ 体重差（最早日末值 − 最新日末值）。
- **小程序新增 `WeightChart.vue`**：SVG data-uri 折线（不引入图表库），窗口切换条（近7天/30天/60天/90天/一年/全部，默认30天），体重差文案（下降绿/上升警示），平台日琥珀点，不足 2 个不同日不渲染。
- 不改变：−20g/−80kcal 微调与恢复阈值、碳循环固化日目标、kg/斤 双单位口径（曲线坐标始终 kg）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- **body/weight**：体重记录由「同日覆盖」改为「同日多条共存 + 每日末值聚合」；平台判定改为基于每日末值；新增多窗口趋势与体重差能力。

## Impact

- **后端**（`apps/zhenxinjian-backend`）：
  - `WeightService`：save 去覆盖、list 去条数上限、isPlateau 按日末值、applyPlatformAdjust 以今日末值为参考；新增 trend 方法。
  - `WeightRecordMapper`：新增每日末值聚合查询（GROUP BY + MAX(id) 回连）。
  - `WeightController`：新增 `GET /weight/trend`；更新 save/list 接口描述。
  - 无建表结构变更（`weight_record` 本就无日期唯一约束），无需刷数；H2 测试 schema 无需改。
- **小程序**（`apps/zhenxinjian-uniapp`）：
  - 新增 `src/components/WeightChart.vue`；`src/api/weight.ts` 新增 trend 接口与类型；
  - `src/pages/weight/index.vue` 新增趋势面板、历史列表全量展示、同日多条；`store/weight.ts` 增加趋势状态。
- **不受影响**：管理后台、后端其余服务、碳循环、小程序其他页面。

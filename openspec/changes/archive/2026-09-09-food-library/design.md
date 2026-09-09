# Design — 食物库（内置 200 条 + 搜索筛选 + 自定义食物）

## Context

后端已有登录/游客（`users`）与身体档案（`user_body`）两层，软删 + 生成列活跃唯一 + 枚举字典的样板已在 Change 2/3 固化（参照 `sql/change3_user_body.sql` 与 `BodyProfileService`）。食物数据真源三份同源：`MRD-PRD/foods_200.json（开发导入用）.json`（后端导入唯一真源）、`doscFile/projectFile/04-食物库数据字典.md`（口径规则）、`apps/zhenxinjian-uniapp/src/data/foods.ts`（小程序离线兜底，已全量对拍一致）。目标见 proposal「Why」。

## Goals / Non-Goals

**Goals:**

- `foods` 单表承载内置 + 自定义食物，搜索/筛选一套查询通路
- 200 条内置数据幂等导入，成为后端权威源
- 自定义食物完整 CRUD（软删），校验与归属隔离后端兜底

**Non-Goals:**

- 单位换算（个/碗/勺）——数据真源无权威换算系数，待 PM 补充
- 热门按记录次数统计——`diet_records` 未建，本期静态清单
- 游客自定义食物迁移——随 Change 5 饮食记录迁移统一设计
- 管理端（front）食物库运营维护——一期边界未确认（PRD 待确认问题）

## Decisions

**D1：单表 `foods` + `source`/`user_id` 隔离，而非内置/自定义分表。**
数据字典两案皆允许；单表让搜索/筛选/分页一条 SQL 覆盖全库，自定义食物通过 `source=2 AND user_id=?` 隔离，搜索默认仅返回内置 + 本人自定义。分表需 UNION 且后续运营扩库跨表，收益低。

**D2：营养值 `DECIMAL(5,1)`、能量 `INT`，偏离 `DOUBLE 存克数` 通则。**
食物值是静态权威数据非用户累计值，数据字典 §6 明确建议 `DECIMAL(5,1)` 防浮点漂移；`DOUBLE 存克数` 规则针对的是随记录累计的克数（未来 `diet_records`），二者不冲突。服务层以 `BigDecimal` 承载入库；详情/列表出参保持每 100g 一位小数，试算结果按「每 100g 值 × 克数 ÷ 100」保留两位小数（如 150g 米饭碳水 38.85），热量取整。

**D3：自定义食物名称活跃唯一用生成列。**
```sql
name_active VARCHAR(191) GENERATED ALWAYS AS
  (IF(delete_flag = 0 AND source = 2, CONCAT(user_id, ':', name), NULL)) STORED,
UNIQUE KEY uk_food_name_active (name_active)
```
内置食物（`source=1`）恒 NULL 不占用唯一约束，与 `user_body.user_id_active` 同构。

**D4：导入用 resources JSON + 幂等初始化器。**
`foods_200.json` 拷入 `src/main/resources/`，`FoodLibraryInitializer`（`ApplicationRunner`）启动时执行：`COUNT(source=1) >= 200` 即跳过，否则清空异常半导入态后批量插入 200 条。备选 SQL `INSERT ... SELECT` 静态脚本被否：200 条含中文与小数，JSON 走代码可复用 Jackson 反序列化且断言条数（≠200 拒绝启动并报错）。单体部署无并发启动竞争；多实例场景由 `uk_food_code` 兜底去重（捕获 `DuplicateKeyException` 跳过）。

**D5：搜索为前后通配 `LIKE '%kw%'`，暂不加全文索引。**
库规模 200 条内置 + 少量自定义，`LIKE` 全表扫描成本可忽略；`name`/`alias` 仍建普通索引备用。规模上千再评估全文索引/搜索中间件。命中规则：名称或别名任一包含关键字（别名按顿号分隔多词，库内以整串 LIKE 即可覆盖「白米饭」这类独立别名）。

**D6：热门食物为静态编码清单（食物编号 F-code 常量）。**
`FoodHotConstant` 维护一组高频减脂食物编号（鸡胸肉/米饭/鸡蛋/西兰花/燕麦片等，≤12 条），按编号查库返回；后续可平滑替换为统计口径，接口不变。

**D7：错误码 404xx 段位，自定义食物校验复用 Bean Validation + Service 交叉校验。**
40401 名称重复、40402 宏量非法、40403 能量不守恒、40404 食物不存在/不可操作、40405 无权限操作他人食物（段位分配与 `ExceptionConstant` 现有 401/402/403 对齐）。能量守恒 `|4c+4p+9f − kcal| / max(kcal,1) ≤ 10%` 在 Service 层以 `BigDecimal` 判定。

**D8：小程序数据通路——`api/food.ts` + food store，失败降级 `foods.ts`。**
搜索/分类/详情走后端；请求失败时 store 标记降级态，检索切本地 `FOODS` 数组（同一 `FoodItem` 结构），联网请求恢复后自动切回。自定义食物仅在线可用（无后端即无意义，失败提示重试不做本地假写）。

## Risks / Trade-offs

- [200 条导入与后续运营版本升级] → 初始化器仅补不覆：已有编号跳过，升级走增量迁移脚本，不在初始化器做 UPDATE
- [`LIKE '%kw%'` 无索引] → 当前数据规模下可接受，D5 记录升级路径
- [DECIMAL 偏离通用约定引起误读] → 建表 SQL 注释与本文档 D2 双重留痕
- [游客自定义食物登录后归属] → 本期游客食物挂游客 ID 正常使用；登录迁移随 Change 5 统一（proposal 已记录）
- [foods_200.json 与 foods.ts 双源漂移] → 导入唯一真源为 JSON，验收任务含与 `foods.ts` 抽样对拍；后续改数据须先改真源再同步

## Migration Plan

1. 执行 `sql/change4_foods.sql` 建表（dev 库手工执行，同前三次变更流程）
2. 部署后端，初始化器自动导入 200 条，启动日志确认 `food=200`
3. 回滚：单表独立无外键引用，`DROP TABLE foods` 即可完全回退（仅 dev 环境；生产尚未上线）

## Open Questions

- 热门清单具体条目由实现时按减脂高频场景选取（≤12 条），验收时用户可要求替换——不改变接口与行为契约

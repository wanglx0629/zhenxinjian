# Tasks — 碳循环模式（周期计划 + 图片公式 + 模式切换）

## 1. 数据库迁移

- [x] 1.1 编写 `sql/change6_carb_cycle.sql`：`user_body` 加 `mode` 列（TINYINT 默认 1）；新建 `carb_cycle_plan`（含体重/池/蛋白快照）与 `carb_cycle_day`（日序/日型/三宏/kcal/运动标）两表，规约标配 `delete_flag`+`status`+审计列与索引（`idx_user_status` / `idx_plan` / `idx_user_date`）
- [x] 1.2 本地执行迁移并验证：mode 默认值、两表索引命中（EXPLAIN 按 user_id+date）、快照列可空约束

## 2. 后端：枚举与数据层

- [x] 2.1 新建字典枚举（`common/enums/`，code+desc+of 规约）：`DietModeEnum`（1=532/2=碳循环）、`CycleDayTypeEnum`（1高/2中/3低）、`CyclePlanStatusEnum`（1进行中/2已完成/3已终止）
- [x] 2.2 新建 PO：`CarbCyclePlan`、`CarbCycleDay`（`@TableLogic` 逻辑删除，`@TableField` 快照列显式映射）；DTO（创建周期入参 + Bean Validation：天数 7–14、cfc 0.8/1.0、运动日 ≤ 天数）与 VO（周期 + 逐日计划 + 今日定位 + 空态）
- [x] 2.3 `BodyProfile` PO/DTO/VO 加 `mode` 字段；新建 `CarbCyclePlanMapper` / `CarbCycleDayMapper`（BaseMapper + 按 user_id+day_date 查当日行）

## 3. 后端：计算引擎（CycleCalcService）

- [x] 3.1 三大池计算：碳水池 = 目标体重×2.5×N、脂肪池 = 目标体重×cfc×N、每日蛋白 = 当前体重×1.5（BigDecimal，注意目标/当前体重不可混淆）
- [x] 3.2 日型分配：高（碳50%/脂15%/w2.0）、中（碳35%/脂35%/w2.2）、低（碳15%/脂50%/w2.0）；N≠7 按权重和归一；逐日 setScale(1, HALF_UP)，热量 4/4/9 取整
- [x] 3.3 总量守恒：逐日合计与池微差归入周期末日，合计 − 池 ≤ ±1g
- [x] 3.4 日型排布：默认模板循环 + 运动日抢占（H 位用尽降级 M 位，非运动日保持相对顺序）；运动日越界/超量 40604
- [x] 3.5 单测：金标对拍（57/55/0.8/7 → 池 962.5/308/85.5，H 240.6/23.1、M 153.1/49.0、L 72.2/77.0 ±0.1g）、14 天与 cfc=1.0 变体、守恒 ±1g、运动日抢占与降级、N≠7 模板循环

## 4. 后端：周期服务（CyclePlanService）

- [x] 4.1 创建周期：校验已建档（40601）→ 终止进行中旧周期 → 计算并落 plan+days（体重/池快照）→ 返回完整计划
- [x] 4.2 查询当前周期：plan + days + 今日 dayIndex 定位；无进行中返回空态；历史周期按 id 查（本人校验 40605）
- [x] 4.3 终止周期：进行中 → 已终止；已完成/已终止幂等不报错
- [x] 4.4 模式切换：`PUT /api/body/mode` 持久化 mode；切出碳循环自动终止进行中周期（G4）；mode 非法 40602
- [x] 4.5 单测：创建顶替旧周期、终止幂等、切换双向（532↔碳循环）、越权、未建档拦截

## 5. 后端：summary 分发与接口

- [x] 5.1 `DietRecordService.buildSummary` 按 mode 分发：532 现状不变；碳循环取 `carb_cycle_day` 当日行为目标；未命中 `noProfile=true`；VO `mode` 返回实际值
- [x] 5.2 `ExceptionConstant` 启用 406xx 段位（40601 未建档 / 40602 模式或参数非法 / 40603 周期不存在 / 40604 运动日越界或过多 / 40605 越权或不可操作）
- [x] 5.3 `CyclePlanController`：`POST/GET/terminate` + `PUT /api/body/mode`，返回 `Result`，注释含「作者: wanglx」
- [x] 5.4 接口级验证：创建→计划→summary（高/中/低碳日目标各异）→切换→summary 回 532 全链路；空态、越权、非法入参错误码命中

## 6. 后端：游客迁移补齐

- [x] 6.1 新建 `CarbCyclePlanMigrator implements GuestDataMigrator`：两表幂等 UPDATE 归属；冲突时游客进行中周期置为已终止；purge 逻辑删除
- [x] 6.2 迁移用例验证：迁移幂等、冲突降级、purge 全量逻辑删除、正式用户周期不受影响

## 7. 小程序：数据通路

- [x] 7.1 新增 `src/api/cycle.ts`：创建/当前计划/历史详情/终止/切换模式 封装 + 类型（CyclePlanVO/CycleDayVO）
- [x] 7.2 新增 cycle store：当前周期、逐日计划、今日定位、cfc 上次选择持久化（本地 storage）
- [x] 7.3 `constants.ts` 新增：日型字典（高/中/低 + 色值）、模式字典、运动日星期选项

## 8. 小程序：P05 模式选择页

- [x] 8.1 新建 `pages/mode/select.vue`：双模式卡片 + 当前标记；选碳循环无周期 → 切模式后引导 P06；有周期直达 P07；`pages.json` 注册

## 9. 小程序：P06/P07 碳循环页面

- [x] 9.1 新建 `pages/cycle/setting.vue`（P06）：天数步进 7–14、cfc 段选（回填上次）、运动日多选（≤ 天数，随天数变化裁剪）；提交创建 → 跳 P07
- [x] 9.2 新建 `pages/cycle/plan.vue`（P07）：池总量头卡 + 逐日日型卡片（色块/三宏/kcal/运动标）+ 今日高亮；空态引导 P06；`pages.json` 注册

## 10. 小程序：切换确认与记录页联动

- [x] 10.1 P16 切换确认弹窗组件：碳循环切出时二次确认「终止周期并清空进度」；确认调 `PUT /api/body/mode`
- [x] 10.2 `pages/record/index.vue`：按 summary `mode` 展示目标口径标签（532「今日目标」/ 碳循环「今日高/中/低碳日」）；碳循环空态引导创建周期

## 11. 联调与验收

- [x] 11.1 全流程走查：建档→选碳循环→建周期→P07 计划→记录页目标按日型→切换 532→周期终止（对照原型 P05/P06/P07/P16）（用户决定暂不走查，直接归档）
- [x] 11.2 金标验算：57/55/0.8/7 → 高 240.6/23.1、中 153.1/49.0、低 72.2/77.0、蛋白 85.5（±0.1g），周期合计守恒 ±1g；14 天与 cfc=1.0 变体抽查
- [x] 11.3 边界验证：无周期空态、运动日抢占与降级、周期内改体重不影响当期、越权拦截、游客迁移与冲突降级
- [x] 11.4 后端 `mvn test` 全绿；小程序 `npm run build:mp-weixin` 通过
- [x] 11.5 `openspec validate carb-cycle --strict` 通过；规格与实现一致性自查后归档

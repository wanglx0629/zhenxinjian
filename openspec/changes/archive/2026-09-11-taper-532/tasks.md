# Tasks — 532 碳水渐降推进（P08/P09 + 经期管理）

## 1. 数据库

- [ ] 1.1 编写 `sql/change8_taper_532.sql`：新建 `user_menstrual`（enabled/period_start_date/cycle_len/period_days，生成列活跃唯一）、`weight_record`（user_id + record_date + weight）、`adjust_log`（action + trigger_weight）；`user_body` 加 `is_adjusted`/`trigger_weight`；三表均带 `delete_flag`/`status`/审计列，符合开发规范 §4
- [ ] 1.2 本地执行迁移并核对表结构（生成列唯一索引、utf8mb4、注释齐全）

## 2. 经期管理

- [ ] 2.1 `ExceptionConstant`/`CommonConstant` 启用 40801 `MENSTRUAL_SETTING_INVALID`；新枚举 `MenstrualPhaseEnum`（menstrual/follicular/ovulation/luteal，含上浮 carb/kcal）
- [ ] 2.2 新建 PO/Mapper：`UserMenstrual`（`@TableLogic`）；对应 Mapper 接口
- [ ] 2.3 `MenstrualCalcService`：`resolvePhase`（性别/开启/起始日/L/D/today → 阶段 + 上浮）；口径与 `calculator.ts.menstrualPhase` 对拍，后端唯一真源
- [ ] 2.4 `MenstrualService` + `MenstrualController`：`GET/PUT /api/menstrual`（男性返回不适用；越界 40801；开启须有效起始日）；统一 `Result` + `UserContext`

## 3. 体重记录与平台调碳

- [ ] 3.1 新枚举 `AdjustActionEnum`（1下调 2恢复）；`ExceptionConstant`/`CommonConstant` 启用 40802 `WEIGHT_INVALID`
- [ ] 3.2 新建 PO/Mapper：`WeightRecord`、`AdjustLog`；对应 Mapper 接口
- [ ] 3.3 `WeightService`：`save`（同日幂等覆盖，25–200 校验，未来日期拒绝）、`list`（日期范围/最近 N）、`remove`（软删）；平台判定 `isPlateau`（近 7 天 ≥2 条 波动 <0.3）
- [ ] 3.4 平台下调/恢复：体重增改后判定下调（未下调态且平台 → `is_adjusted=1` + `trigger_weight` + 写下调日志）与恢复（下调态且较触发参考下降 ≥0.3 → 清除 + 写恢复日志）；单次门闩防连续下调
- [ ] 3.5 `WeightController`：`GET/PUT/DELETE /api/weight`；统一 `Result` + `UserContext`

## 4. 532 推进与目标分发

- [ ] 4.1 `Taper532Service`：四阶段计划卡（静态结构 + 阶段 2 经期说明注入）；`todayTarget`（基线 + 平台下调 + 经期上浮，蛋白脂肪不变，克数 1 位小数/热量取整）
- [ ] 4.2 `Taper532Controller`：`GET /api/taper-532/plan`（计划卡 + 今日目标）；统一 `Result`
- [ ] 4.3 `DietRecordService.summary` 改造：532 模式目标改用 `todayTarget`；碳循环模式叠加经期上浮；`MenstrualCalcService` 复用
- [ ] 4.4 `Taper532Migrator implements GuestDataMigrator`：migrate（经期设置冲突降级保留正式 + 体重/日志改归属，幂等）；purge（三表逻辑删除）；接入既有编排

## 5. 小程序 P08/P09 与入口

- [ ] 5.1 `api/weight.ts`、`api/menstrual.ts`、`api/taper.ts`；`pages.json` 注册三页
- [ ] 5.2 `pages/taper/plan.vue`（P08 四阶段计划卡 + 今日目标）、`pages/weight/index.vue`（P09 体重记录 + 平台提示 + 调碳日志）、`pages/menstrual/index.vue`（经期设置，男性隐藏）
- [ ] 5.3 `pages/mine/index.vue` 功能列表加「体重记录」「月经周期」入口（按性别显隐，onShow 同步）
- [ ] 5.4 首页/记录页按后端 summary 推进口径展示目标，不再本地 macro532 叠加

## 6. 联调与验收

- [ ] 6.1 经期闭环：保存 L/D 越界 40801、未来起始日拒绝、男性不适用、阶段判定与上浮对拍（速查 §9 基准 + 48 项对拍口径）
- [ ] 6.2 体重闭环：同日覆盖、越界/未来拒绝、软删、近 7 天平台判定（≥2 条 <0.3）
- [ ] 6.3 调碳闭环：触发下调 −20/−80（单次门闩）、恢复解除 +0.3、调碳日志留痕
- [ ] 6.4 532 推进目标验算：基线 190.1/1521 → 经期 +15/+60 = 205.1/1581；下调后 170.1/1441；下调+经期 185.1/1501
- [ ] 6.5 目标分发：532 推进目标 / 碳循环日型叠加经期；游客迁移无冲突改归属、冲突保留正式设置
- [ ] 6.6 `mvn compile` 与 `npm run build:mp-weixin` 通过；`openspec validate taper-532 --strict` 通过；规格与实现一致性自查后归档
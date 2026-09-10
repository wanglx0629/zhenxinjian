# Tasks — 饮食记录（增删改 + 当日累计 + 三色进度 + 微调建议）

## 1. 数据库迁移

- [x] 1.1 编写 `sql/change5_diet_records.sql`：`diet_records`（user_id / record_date / meal_type / source / food_id / food_name 快照 / 每 100g 快照四列 / amount_g / 实际摄入四列 / remark + `delete_flag` + `status` + 审计列 + `idx_user_date(user_id, record_date, delete_flag)` 索引；克数 DOUBLE、快照 DECIMAL(5,1) 与 kcal INT，注释留痕 D2 口径）
- [x] 1.2 本地执行迁移并验证：逻辑删除列默认值、索引生效（EXPLAIN 按 user_id+date 命中）、快照列可空约束符合 D1

## 2. 后端：枚举与数据层

- [x] 2.1 新建字典枚举（`common/enums/`，code+desc+of 规约）：`MealTypeEnum`（1早/2午/3晚/4加餐）、`DietRecordSourceEnum`（1内置食物/2自定义食物/3手动输入）
- [x] 2.2 新建 PO：`DietRecord`（`@TableLogic` 逻辑删除）；DTO（新增/编辑入参 + Bean Validation：份量 >0 ≤5000、名称 ≤50 字、备注 ≤100 字）与 VO（记录项 + 餐别分组 + 小计/合计 + summary 含目标与达成率 + mode 恒 532）
- [x] 2.3 新建 `DietRecordMapper`（MyBatis-Plus BaseMapper）+ 餐别分组 SUM 聚合查询

## 3. 后端：记录服务（DietRecordService）

- [x] 3.1 新增记录：食物来源（查食物→归属校验：内置或本人自定义→BigDecimal 按克数换算→落快照+摄入）；手动输入（区间 + 能量守恒 ±10% 复核）；份量/名称非法走 405xx
- [x] 3.2 编辑记录：按 id + user_id 双条件；食物来源仅可改餐别/份量/备注（按快照重算摄入，不回查食物表）；手动输入可改名称与三宏热量（守恒复核）
- [x] 3.3 删除记录：逻辑删除（id + user_id 双条件），越权/不存在统一 40504
- [x] 3.4 按日查询：餐别四组分组返回（含小计与当日合计），空日返回空分组；未来日期 40505
- [x] 3.5 单测：换算精度（150g 鸡胸肉 → 36.9/2.9/0.9/177 ±0.1g/±1kcal）、守恒 ±10% 边界、越权拦截、未来日期拦截、快照不随食物变更

## 4. 后端：当日累计（summary）

- [x] 4.1 `GET /api/diet/summary?date=`：已摄入合计 + `user_body` 活跃档案目标快照 + 各项达成率 + mode=532；未建档返回空态标记（不报错）
- [x] 4.2 单测：有档案正常返回、未建档空态、删除记录后合计减少、历史日期可查

## 5. 后端：接口与常量

- [x] 5.1 `ExceptionConstant` 新增饮食记录文案；启用 405xx 段位（40501 份量非法 / 40502 宏量非法 / 40503 能量不守恒 / 40504 记录不存在或不可操作 / 40505 未来日期 / 40506 食物不存在或已删除）
- [x] 5.2 `DietController`：`POST/PUT/DELETE /api/diet/records`、`GET /api/diet/records?date=`、`GET /api/diet/summary?date=`，返回 `Result`，注释含「作者: wanglx」
- [x] 5.3 接口级验证：三类来源新增/编辑/删除/按日查询/summary 全链路符合 spec；越权、未来日期、守恒失败各场景错误码命中

## 6. 后端：游客迁移补齐

- [x] 6.1 新建 `DietRecordMigrator implements GuestDataMigrator`：`migrate` 幂等 UPDATE 归属、`purge` 逻辑删除游客记录；注册 Bean 由登录/清理任务自动装配
- [x] 6.2 迁移用例验证：迁移后归属变更且幂等（重复执行无影响）、purge 全量逻辑删除、正式用户记录不受影响

## 7. 小程序：数据通路

- [x] 7.1 新增 `src/api/diet.ts`：记录 CRUD/按日查询/summary 封装 + 类型定义（RecordItem/MealGroup/Summary）
- [x] 7.2 新增 diet store：当日记录、餐别分组、summary、当前查看日期；增删改后重拉保证实时
- [x] 7.3 `constants.ts` 新增：餐别列表与时段默认规则（05–10 早 / 10–15 午 / 15–20:30 晚 / 其余加餐）、三色阈值（80/100）、微调建议文案（脂肪/碳水/蛋白/总热量四条 + 免责注）

## 8. 小程序：P11 添加饮食页

- [x] 8.1 新建 `pages/record/add.vue`：餐别段选（按时段默认选中可改）+ 食物模式（路由带入 foodId + 克数，克数可改，实时换算预览）/ 手动模式（名称 + 三宏 + 热量，前端区间与 ±10% 校验）
- [x] 8.2 提交调后端，成功返回记录页并刷新；失败提示可重试；`pages.json` 注册路由

## 9. 小程序：P12 当日记录页

- [x] 9.1 重写 `pages/record/index.vue`：日期导航（右箭超今日禁用）+ 餐别分组列表（空餐别显空态）+ 编辑（跳 add 页回显）/ 删除（二次确认）
- [x] 9.2 进度区：三宏 + 总热量四条进度条（80–100% 绿 / <80% 黄 / >100% 红 + 已超标 XXg/XXkcal）；未建档空态隐藏进度区并引导录档案
- [x] 9.3 超标建议卡：按超标项动态拼接建议（口径同 spec），附「非医疗建议」注；无超标不展示

## 10. 小程序：食物库联动

- [x] 10.1 P10 食物详情试算区新增「加入记录」按钮：携带 foodId + 当前克数跳转 P11
- [x] 10.2 自定义食物详情同样可加入记录；手动输入入口在 P12 与 P11 均可达

## 11. 联调与验收

- [x] 11.1 全流程走查：选食物→加记录→当日累计更新→编辑/删除→三色进度与超标建议正确（对照原型 P11/P12）
- [x] 11.2 金标验算：目标脂肪 34g 摄入 70g → 红色「已超标 36g」+ 脂肪建议；双超标并列建议；达成 90% 绿色；65% 黄色
- [x] 11.3 边界验证：未建档空态、历史日期可查、未来日期拦截、食物软删后历史记录完整、游客登录迁移归属
- [x] 11.4 后端 `mvn test` 全绿（69/69）；小程序 `npm run build:mp-weixin` 通过
- [x] 11.5 `openspec validate diet-record --strict` 通过；规格与实现一致性自查后归档

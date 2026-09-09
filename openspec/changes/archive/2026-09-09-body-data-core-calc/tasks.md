# Tasks — 身体数据录入与核心计算

## 1. 数据库迁移

- [x] 1.1 编写 `sql/change3_user_body.sql`：`user_body`（性别/年龄/身高/体重/目标体重/活动系数/缺口/cfc 默认 0.8/BMR/TDEE/基准热量/目标三宏快照 + `delete_flag` + `status` + 审计列 + `user_id_active` 生成列活跃唯一索引）与 `user_body_history`（同构快照 + 归档时刻 + `delete_flag`/`status`）
- [x] 1.2 本地执行迁移并验证：活跃唯一约束生效（同用户插第二条活跃记录报错）、逻辑删除列默认值正确

## 2. 后端：枚举与数据层

- [x] 2.1 新建字典枚举（`common/enums/`，code+desc+of 规约）：`GenderEnum`（1男/2女）、`ActivityLevelEnum`（1.2/1.375/1.55/1.725）、`DeficitOptionEnum`（200/300/400/500）
- [x] 2.2 新建 PO：`UserBody`（`@TableLogic` 逻辑删除、`@Version` 如需）与 `UserBodyHistory`，DTO（保存入参 + Bean Validation 区间注解）与 VO（档案 + 结果快照 + 风险标记 + 免责声明）
- [x] 2.3 新建 `UserBodyMapper` / `UserBodyHistoryMapper`（MyBatis-Plus BaseMapper）

## 3. 后端：计算服务（BodyCalcService）

- [x] 3.1 实现 BMR（Mifflin-St Jeor 男女两式）/TDEE（4 档系数）/基准热量（TDEE − X，默认 200）/532 宏量（50/30/20，4/4/9）；热量取整 kcal、克数 1 位小数 double
- [x] 3.2 实现低热量风险判定（女 <1200 / 男 <1500 返回风险标记，不阻断）
- [x] 3.3 单测移植对拍验算基准：女 30/162/55/1.375/200 → BMR 1252、TDEE 1721、基准 1521、碳 190.1/蛋 114.1/脂 33.8（±1kcal/±0.1g）；补男性公式差值 166、缺口联动、风险提示用例

## 4. 后端：档案服务与接口

- [x] 4.1 `ExceptionConstant` 新增身体数据文案；`CommonConstant` 启用 403xx 段位（40301 目标体重超当前+0.1、40302 缺口档位非法等）
- [x] 4.2 `BodyProfileService.save`：Service 层交叉校验（目标体重 ≤ 当前 +0.1、缺口档位枚举）→ 同事务「归档旧值入 history + 覆盖当前档案 + 重算快照」；`DuplicateKeyException` 转覆盖重试不抛 500
- [x] 4.3 `BodyProfileService.getCurrent`：返回档案+快照+风险标记+免责声明；未录入返回空态（不抛错）
- [x] 4.4 `BodyController`：`PUT /api/body/profile`、`GET /api/body/profile`，返回 `Result`，注释含「作者: wanglx」
- [x] 4.5 接口级验证：越界/非法档位/并发保存/未录入空态各场景返回符合 spec（实测：金标 1252/1721/1521/190.1/114.1/33.8 命中；age=11 拦截；40302/40301 命中；空态 recorded=false；归档 2 条+活跃唯一+version 递增；修复 strictUpdateFill 不刷新 updateTime 问题）

## 5. 后端：游客迁移补齐

- [x] 5.1 `UserRecordMigrator` 增加 `user_body`、`user_body_history` 归属迁移（UPDATE user_id，幂等）；正式用户已有活跃档案时游客活跃档案降级入 history（新建 `BodyDataMigrator implements GuestDataMigrator`，登录/清理任务自动装配调用）
- [x] 5.2 迁移用例验证：`BodyDataMigratorTest` 4 场景通过（无冲突改归属/冲突降级入 history 且不覆盖正式用户当前值/游客档案缺失幂等/purge 双表逻辑删除）；真实微信 code 本地不可签发，登录链路装配由 `SpringUtils.getBeansOfType(GuestDataMigrator)` 保证

## 6. 小程序：数据通路

- [x] 6.1 新增 `src/api/body.ts`：保存/查询封装 + 类型定义（Profile/CalcResult/RiskFlag）
- [x] 6.2 新增 body store（或扩展 user store）：当前档案、计算结果、空态标识；提交后以后端返回覆盖本地预览值

## 7. 小程序：P03 身体数据录入页

- [x] 7.1 新建 `pages/body/profile.vue`：性别/年龄/身高/体重/目标体重/活动系数 4 档/缺口 4 档（默认 200），复用 `constants.ts` RANGES/ACTIVITY/DEFICIT_OPTIONS 做提交前校验
- [x] 7.2 录入页实时预览（`calculator.ts`），标注「预览以服务端结果为准」；提交调后端，失败提示可重试
- [x] 7.3 已录入用户进入时回显当前档案

## 8. 小程序：P04 代谢结果页

- [x] 8.1 新建 `pages/body/result.vue`：展示 BMR/TDEE/基准热量/目标三宏（UI 整数克）+ 低热量风险提示（按风险标记）
- [x] 8.2 页面内置免责声明文案（不可移除）；预留「去选择模式」入口（P05 后续 change）
- [x] 8.3 `pages.json` 注册 P03/P04 路由；保存成功跳转结果页，未录入查询空态引导至录入页

## 9. 联调与验收

- [x] 9.1 后端 `mvn test` 通过（11 项：BodyCalcServiceTest 7 + BodyDataMigratorTest 4，含对拍验算基准单测）；小程序 `npm run build:mp-weixin` 编译无错
- [x] 9.2 按 spec 场景走查：录入→结果（金标 1252/1721/1521/190.1/114.1/33.8 与验算基准一致）→ 修改留痕（history 2 条+version 递增+updateTime 刷新）→ 越界拒绝（age=11 / deficit=250→40302 / 目标体重>当前→40301）→ 游客录入→登录迁移（单测 4 场景：改归属/冲突降级/幂等/purge）→ 免责声明可见（每次响应均带 disclaimer）
- [x] 9.3 更新验收记录，准备 `openspec verify`

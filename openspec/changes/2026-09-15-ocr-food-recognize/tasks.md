# OCR 食物识别（GLM 视觉模型）— Tasks

> 执行顺序即编号顺序；每步完成后勾选 `[x]`。规约：错误文案进 `ExceptionConstant`、注释 `作者: wanglx`、接口返回 `Result`。

## 1. 数据库

- [x] 1.1 新建 `sql/change12_ocr_config.sql`：ocr.model/enabled/daily-limit/timeout-seconds/cache-ttl-hours 5 条种子（INSERT IGNORE）+ schema_migrations 补账 version=12（api-key 不进 git）
- [x] 1.2 本地库执行 change12 SQL 并验证种子/补账（mysql CLI，参照 change11 流程）

## 2. 后端

- [x] 2.1 `ProjectConfigKeyConstant` 新增 ocr.* 键常量与前缀；`CommonConstant`/`ExceptionConstant` 新增 AI 四组错误码与文案（40910–40913）
- [x] 2.2 pom 引入 `ai.z.openapi:zai-sdk:0.3.5`；全量 `mvn test` 验证依赖无冲突
- [x] 2.3 `common/ai/AiChatClient.java`（接口 + AiImage record）+ `common/ai/ZhipuAiChatClient.java`（每次调用现读 key/model/timeout 懒构建 SDK 客户端）
- [x] 2.4 `domain/vo/FoodRecognizeVO.java`（name/carb/protein/fat/kcal，每 100g 基准）
- [x] 2.5 `service/impl/AiRecognizeService.java`：开关 fail-closed → 扩展名/大小校验 → 同图 SHA-256 指纹缓存命中直返（RedisUtils，TTL 走配置）→ Redis 每日限流 → 调模型（围栏剥离 + JSON 解析 + 失败重试 1 次）→ kcal 4/4/9 守恒重算覆盖 → 脏值丢弃/10 项截断 → 成功后写指纹缓存 + INCR 计次
- [x] 2.6 `FoodRecognizeController`：`POST /food/recognize`（multipart file，登录含游客）；Swagger 注解
- [x] 2.7 单测 `AiRecognizeServiceTest`（开关/校验/指纹命中不计次/限流/key 缺失/解析容错/重试/守恒重算/脏值）+ `FoodRecognizeControllerTest`

## 3. 小程序（uniapp）

- [x] 3.1 `src/api/ai.ts`：`recognizeFood(filePath)`（uni.uploadFile 带 token）
- [x] 3.2 `src/pages/record/recognize.vue`：选图 → 上传 loading → 候选卡片（名称/每 100g 四值/克数输入默认 100）→ 勾选加入今日记录（走现有 source=3 手动保存链路）→ 返回刷新；teal+橙 V1.1 风格
- [x] 3.3 `pages/record/add.vue` 加「拍照识别」入口；`pages.json` 注册新页

## 4. 验证

- [x] 4.1 全量 `mvn test` 通过
- [ ] 4.2 本地启动：admin API 录入真实 key（SECRET 加密落库）→ E2E：上传食物图识别成功且 kcal 守恒；同图二次提交命中缓存（无模型调用、剩余次数不变）；改 `ocr.enabled=false` 拒识；改回恢复；超限触发限流文案（临时调小 daily-limit 验证后恢复）
  - ⏸️ 2026-09-15 暂缓：需本地 MySQL/Redis + 真实智谱 key，待环境就绪后补验
- [ ] 4.3 小程序端联调：拍照/选图 → 识别 → 勾选 → 保存记录全链路；识别名称含敏感词时保存被拦截（复用过滤体系）
  - ⏸️ 2026-09-15 暂缓：待微信开发者工具联调环境，代码已通过 vue-tsc 类型检查

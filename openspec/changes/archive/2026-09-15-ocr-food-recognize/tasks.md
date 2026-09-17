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
- [x] 4.2 本地启动：admin API 录入真实 key（SECRET 加密落库）→ E2E：上传食物图识别成功且 kcal 守恒；同图二次提交命中缓存（无模型调用、剩余次数不变）；改 `ocr.enabled=false` 拒识；改回恢复；超限触发限流文案（临时调小 daily-limit 验证后恢复）
  - 2026-09-16 已补验（真实 key）：识别+kcal 守恒、同图指纹缓存命中不计次见 change13 E2E；本次补 `ocr.enabled=false`→40912「拍照识别功能暂未开放」、恢复 true→识别正常；`daily-limit=0`→40910「今日识别次数已用完」，恢复 20；配置均经 `/admin/configs` 热改即时生效，验后恢复并清理测试键。
- [x] 4.3 小程序端联调：拍照/选图 → 识别 → 勾选 → 保存记录全链路；识别名称含敏感词时保存被拦截（复用过滤体系）
  - 2026-09-16 已联调（H5 等价环境，dev server :5174 + Playwright）：本机未安装微信开发者工具（注册表/开始菜单/常见目录均无），故以 uni-app H5 端走同一套页面与 `uni.uploadFile`/`uni.request` 封装验证——游客登录 → 识别页选图上传 F011 → 返回候选「薏米 72.2碳/13.7蛋/5.4脂/392.2kcal」（100g 守恒、摄入预览按克数缩放）→ 勾选「加入今日记录」→ 跳转记录 tab，午餐出现该条且当日合计正确，控制台无 error；
  - 敏感词：临时将「薏米」加入 `sensitive.filter.extra-words`（词库 333→334 热重建），同链路保存时被后端拦截「内容包含违规词汇，请修改后重试」、停留识别页不跳转；验后恢复词库 333。测试游客与记录已软删、测试键已清。
  - 备注：页面/类型此前已过 vue-tsc；mp-weixin 真机/开发者工具的相机与上传授权弹窗建议发版前再走一遍（本机无该工具，未能覆盖端原生差异）。

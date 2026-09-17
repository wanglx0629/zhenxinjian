# OCR 食物识别（GLM 视觉模型）— Design

| 项目 | 内容 |
| ---- | ---- |
| Change ID | 2026-09-15-ocr-food-recognize |
| 依赖 | change11 `project_config` + ConfigService（SECRET 加密/缓存失效）已上线；StorageService 扩展名白名单可复用 |
| 供应商 | 智谱 BigModel，模型 **GLM-4.6V-Flash**（API 名 `glm-4.6v-flash`，免费档），SDK `ai.z.openapi:zai-sdk:0.3.5`，端点 `https://open.bigmodel.cn/api/paas/v4/` |

## 1. 数据库设计

### 1.1 `sql/change12_ocr_config.sql`（version=12）

```sql
USE zhenxinjian;

-- OCR 运行时参数种子（api-key 为 SECRET 已在 change11 预置，真实值不进 git，经 admin 后台录入）
INSERT IGNORE INTO project_config (config_key, config_value, value_type, remark, create_by)
VALUES
('ocr.model',           'glm-4.6v-flash', 1, '食物识别视觉模型名（智谱），可随时换', 'system'),
('ocr.enabled',         'true',           3, '食物识别总开关', 'system'),
('ocr.daily-limit',     '20',             2, '每用户每日识别次数上限', 'system'),
('ocr.timeout-seconds', '30',             2, '识别调用超时（秒）', 'system'),
('ocr.cache-ttl-hours', '24',             2, '同图识别结果缓存时长（小时）', 'system');

INSERT IGNORE INTO schema_migrations(version, script) VALUES (12, 'change12_ocr_config.sql');
```

- 不建新表（复用 project_config）；data.sql / h2-schema.sql 无需变更（无新表、种子不进基线）
- `ocr.api-key`：change11 种子已存在；本地库经 admin API `PUT /admin/configs/{id}` 传明文自动 AES 加密落库

## 2. 后端设计

### 2.1 常量与错误文案

- `ProjectConfigKeyConstant` 新增：`OCR_MODEL` / `OCR_ENABLED` / `OCR_DAILY_LIMIT` / `OCR_TIMEOUT_SECONDS` / `OCR_CACHE_TTL_HOURS`（及前缀 `OCR_PREFIX = "ocr"`）
- `CommonConstant`：`AI_DAILY_LIMIT_CODE = 40910`、`AI_RECOGNIZE_FAIL_CODE = 40911`、`AI_SERVICE_DISABLED_CODE = 40912`、`AI_CONFIG_MISSING_CODE = 40913`
- `ExceptionConstant`：`AI_DAILY_LIMIT`（"今日识别次数已用完，明天再来吧"）、`AI_RECOGNIZE_FAIL`（"识别失败，请换张清晰的食物图片重试"）、`AI_SERVICE_DISABLED`（"拍照识别功能暂未开放"）、`AI_CONFIG_MISSING`（"识别服务未配置，请联系管理员"）

### 2.2 AI 客户端薄封装（可测性关键）

- `common/ai/AiChatClient.java`（接口）：`String chat(String model, String systemPrompt, List<AiImage> images, int timeoutSeconds)`；`AiImage(String base64, String mimeType)` record
- `common/ai/ZhipuAiChatClient.java`（`@Component`，实现）：内部 `ZhipuAiClient.builder().ofZHIPU().apiKey(configService.getValue(OCR_API_KEY)).build()` **每次调用懒构建**（key 换了即时生效，免监听配置变更）；SDK `client.chat().createChatCompletion(...)` 多模态消息（image_url = `data:{mime};base64,{b64}`）；超时由 SDK request 配置
- 设计理由：SDK 客户端为具体类不利于单测，抽薄接口后 `AiRecognizeService` 单测可纯 mock

### 2.3 AiRecognizeService（`service/impl/AiRecognizeService.java`）

流程 `recognize(Long userId, MultipartFile file)`：

1. **开关**：`configService.getBoolean(OCR_ENABLED, true)` 为 false → 抛 `AI_SERVICE_DISABLED`（fail-closed：该功能关闭即拒绝，区别于敏感词的 fail-open）
2. **参数校验**：扩展名 ∈ jpg/jpeg/png/webp，大小 ≤10MB（口径与 `zhenxinjian.storage` 一致，硬编码常量）
3. **同图指纹缓存**：`DigestUtil.sha256Hex(file.getBytes())` 为指纹，缓存键 `zhenxinjian:ocr:img:{sha256}`（RedisUtils 手动读写，值 = 结果 JSON 串；不用 JetCache 注解——其 expire 为编译期常量，无法从配置表动态读 TTL）；**命中直接返回缓存结果**——不调模型、不计次、不做限流检查（缓存与用户无关，同一张图全站共享同一份结果；识别结果为公开营养数据，无隐私风险）；TTL = `configService.getInt(OCR_CACHE_TTL_HOURS, 24)` 小时。指纹计算对 10MB 图片开销毫秒级，可忽略
   - **字节级语义边界**：指纹只保证拦住「字节完全相同」的精确重放（脚本刷接口的主要形态）；截图/重压缩/重拍产生的字节变化不命中，由每日限流兜底。不引入感知哈希（pHash）——其误判会把 A 图结果错配给 B 图，对 20 次/天限流的功能不值当
4. **限流**：Redis key `zhenxinjian:ocr:daily:{userId}:{yyyyMMdd}`；`INCR` 首次置 TTL 到当日 24:00；值 > `configService.getInt(OCR_DAILY_LIMIT, 20)` → 抛 `AI_DAILY_LIMIT`（用 RedisUtils 现有能力）
5. **key 检查**：`configService.getValue("ocr.api-key")` 空白 → 抛 `AI_CONFIG_MISSING`
6. **调模型**：`AiChatClient.chat(model, SYSTEM_PROMPT, [image], timeout)`；`model = configService.getValue(OCR_MODEL)` 缺省 `glm-4.6v-flash`
7. **解析**：剥 ```` ```json ````/```` ``` ```` 围栏 → `JSONUtil` 解析 `{"items":[{name,carb,protein,fat,kcal}]}`；失败**重试 1 次**（Prompt 补一句"只输出 JSON"），仍失败 → 抛 `AI_RECOGNIZE_FAIL`
8. **守恒重算**：每项 `kcal = carb*4 + protein*4 + fat*9`（BigDecimal，HALF_UP 1 位小数），**覆盖模型 kcal**；宏量负数/超界（>100g/100g）置 0 丢弃脏值；`name` trim，空白项丢弃；上限 10 项
9. **写缓存 + 计费计数**：结果写入指纹缓存（步骤 3 键），Redis INCR 限流计数（仅模型调用成功后；校验失败、缓存命中均不计次）

**Prompt（SYSTEM，中文）**：角色"食物营养识别助手"；**仅识别图中可食用的食物**（忽略人物/餐具/包装/文字/背景等非食物内容），识别图中**所有**食物（一图多食逐一列出）；输出严格 JSON（不输出任何其他文字/Markdown 围栏）：
`{"items":[{"name":"食物名(中文,≤20字)","carb":每100g碳水g,"protein":每100g蛋白质g,"fat":每100g脂肪g}]}`；数量 1–10 项；拿不准时给出最可能项；**图中无任何食物时输出 `{"items":[]}`**。
- 解析后 `items` 为空 → 返回空列表（HTTP 200），前端空态提示「未识别到食物」，不算错误不计次

### 2.4 Controller（`FoodRecognizeController` → 挂 FoodController 同域，`/food/recognize`）

- `POST /food/recognize`：`@RequestPart("file") MultipartFile`，登录态（含游客）即可，`UserContext.getUserId()`
- 返回 `Result<List<FoodRecognizeVO>>`；`FoodRecognizeVO{id?无, name, carb, protein, fat, kcal}`（每 100g 基准）
- Swagger 注解齐备；无新 DTO（multipart 单文件）

### 2.5 配置即时生效链路

- 读：ConfigService JetCache BOTH（本地 5min/远程 1h）+ cacheNullValue
- 写：admin `PUT /admin/configs/{id}` → `evictCache`（本变更键不含 sensitive 前缀，不触发词库重建，无副作用）
- ZhipuAiChatClient 每次调用现读 key/model/timeout → 后台改完下一次请求即生效

## 3. 小程序设计（uniapp）

- `src/api/ai.ts`：`recognizeFood(filePath)` 用 `uni.uploadFile({url: '/food/recognize', name: 'file'})`（携带 token，对齐 wechatLogin 上传模式）
- `src/pages/record/recognize.vue`：选图（`uni.chooseImage` 相册/相机）→ 上传 loading → 候选列表卡片（名称 + 每 100g 四值 + 克数输入默认 100）→ 勾选「加入今日记录」→ 逐条调用现有 `addRecord`（source=3 手动链路：敏感词 check、守恒校验、餐别默认当前选餐次）→ 成功后 `uni.navigateBack` 刷新当日列表
- `pages/record/add.vue`：顶部加「拍照识别」入口按钮跳转新页；`pages.json` 注册 `record/recognize`
- 样式对齐 V1.1 teal+橙 token（复用 EmptyState / 卡片阴影规范）

## 4. 测试设计

- `AiRecognizeServiceTest`（mock AiChatClient + ConfigService + RedisUtils）：开关关闭拒识 / 扩展名与大小校验 / 限流 INCR 与超限拒 / key 缺失报配置错 / 围栏剥离与 JSON 解析 / 解析失败重试一次后抛错 / kcal 守恒重算覆盖模型值 / 脏值丢弃与 10 项截断
- `FoodRecognizeControllerTest`：@WebMvcTest multipart 200 / 未登录语义（addFilters=false 模式下仅验证 200 与 code）
- `ZhipuAiChatClient` 不单测（薄封装，E2E 覆盖）
- E2E（本地真实 key）：admin API 录入 key → 上传一张食物图 → 断言候选与 kcal 守恒 → 改 enabled=false 断言拒识 → 恢复

## 5. 实施顺序与风险

1. change12 SQL → 2. 常量/错误码 → 3. AiChatClient + Zhipu 实现（SDK 依赖入 pom）→ 4. AiRecognizeService + Controller → 5. 单测 → 6. 小程序 api/页面/入口 → 7. E2E 联调

| 风险 | 缓解 |
| ---- | ---- |
| 免费模型限速/服务不稳 | timeout 可配；解析失败重试 1 次；统一失败文案引导重试 |
| 模型输出非 JSON / 字段漂移 | 严格 Prompt + 围栏剥离 + 重试 + 脏值丢弃；守恒重算以宏量为准 |
| 免费额度被刷 | 每用户每日限流（Redis 原子 INCR），阈值后台可调 |
| API Key 泄露 | SECRET 加密落库 + `******` 脱敏下发；不进 git；聊天已暴露建议轮换（用户侧操作） |
| SDK 版本与 Spring Boot 3.5 依赖冲突 | zai-sdk 为独立 HTTP 客户端，冲突概率低；引入后跑全量 mvn test 验证 |

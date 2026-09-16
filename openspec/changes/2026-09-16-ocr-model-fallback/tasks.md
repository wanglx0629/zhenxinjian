# OCR 模型故障转移与熔断降级 — Tasks

> 执行顺序即编号顺序；每步完成后勾选 `[x]`。规约：错误文案进 `ExceptionConstant`、注释 `作者: wanglx`、接口返回 `Result`；遵循 TDD（先写/改单测再实现）。

## 1. 数据库

- [x] 1.1 新建 `sql/change13_ocr_fallback.sql`：`ocr.fallback-models`（value_type=4，值 `["GLM-4.1V-Thinking-Flash","GLM-4V-Flash"]`，按 2026-09-16 用户指定预置两个备用模型；空数组语义仍由代码缺省保留）、`ocr.circuit-fail-threshold`（=2，值 `3`）、`ocr.circuit-open-seconds`（=2，值 `300`）三条 INSERT IGNORE 种子 + schema_migrations 补账 version=13
- [x] 1.2 本地库执行 change13 SQL 并验证种子/补账（mysql CLI，参照 change12 流程）
  - 2026-09-16 已执行：3 条 ocr.* 种子 + schema_migrations v13 在库；修正了首次以错误字符集导入导致的 3 行 remark 乱码（utf8mb4 连接重跑 UPDATE）

## 2. 后端

- [x] 2.1 `ProjectConfigKeyConstant` 新增 `OCR_FALLBACK_MODELS` / `OCR_CIRCUIT_FAIL_THRESHOLD` / `OCR_CIRCUIT_OPEN_SECONDS` 三键
- [x] 2.2 新增 `common/ai/AiCallException`（携带 `httpStatus`、`transferable`）；`ZhipuAiChatClient` 改为透出状态码并分类：429/5xx/404/408/网络超时/结构异常=可转移，401/403/400/422 及其余 4xx=不可转移（保留 WARN 日志）
- [x] 2.3 `CommonConstant`/`ExceptionConstant` 新增「识别繁忙，请稍后再试」错误码与文案（40914）
- [x] 2.4 `AiRecognizeService`：读取主模型 + fallback（getStringList，去空去重保序）、阈值/时长（getInt 带默认与下限保护）；新增候选链编排（跳过熔断模型 → 调用 → 成功清熔断 / 可转移失败计数并按阈值置熔断 / 不可转移立即失败 → 全耗尽抛繁忙）；同模型仅 JSON 解析失败加强提示词重试一次，服务端错误不做同模型重试
- [x] 2.5 Redis key（已为 `RedisUtils` 补通用 `delete` 方法）：`zhenxinjian:ocr:modelfail:{model}`（计数，TTL=熔断窗口）、`zhenxinjian:ocr:circuit:{model}`（熔断标记，TTL=窗口秒）；复用 `RedisUtils` get/setEx/incrementWithTtl/delete（按需补删除方法）

## 3. 测试

- [x] 3.1 扩展 `AiRecognizeServiceTest`（新增 8 个场景，共 20 个测试）：主 429→备用成功、主熔断中直接跳过、成功清除计数/熔断、401 立即失败不切换不熔断、全熔断无调用快速失败、连续失败达阈值置熔断、fallback 空时与现状一致
- [x] 3.2 全量 `mvn test` 通过（382 测试，0 失败 0 错误，JDK 25）

## 4. 验证

- [x] 4.1 本地 E2E（错峰或临时配置一个稳定备用模型）：主模型 429 时自动切备用返回候选；后台改 `ocr.fallback-models` 即时生效；识别成功不扣减异常计数
  - 2026-09-16 真实 key 本地 E2E（MySQL/Redis 均在本机，dev profile，JDK 25）：
    - 主模型 glm-4.6v-flash 连续 3 次可转移失败（1 次 SDK 结构异常 httpStatus=0、2 次真实 HTTP 429）后自动切备用 GLM-4.1V-Thinking-Flash/GLM-4V-Flash 返回候选（大米/白米饭/糙米），kcal 按 4/4/9 守恒；
    - 熔断计数 1→2→3，达阈值写熔断标记（TTL 300s），之后请求日志「熔断中，本次跳过」直切备用；
    - 三模型全熔断 → code=40914「识别繁忙，请稍后再试」，且当日计数不增加（繁忙不计次）；
    - 备用模型成功（含空结果）后其预置失败计数被 clearCircuit 清除；
    - 同图重复上传命中指纹缓存（无模型调用、不重复计次）；
    - 经 `/admin/configs/10` 把 fallback 改为 `[]` 即时生效（主熔断时立即 40914），改回两备用后立即恢复出候选；
    - 测试后已清理全部 `zhenxinjian:ocr:*` 测试键与临时登录 token，配置恢复种子值。

# OCR 模型故障转移与熔断降级 — Proposal

| 项目 | 内容 |
| ---- | ---- |
| Change ID | 2026-09-16-ocr-model-fallback |
| 类型 | feature（change12 拍照识别的可用性增强） |
| 涉及端 | `apps/zhenxinjian-backend`（仅后端；配置复用通用系统配置页，无前端改动） |
| 依赖变更 | change12（`ocr.model` 单模型识别链路） |

## 1. 背景

- change12 上线的拍照识别只配置单个模型 `ocr.model`。实测免费档视觉模型（glm-4.6v-flash）高峰期频繁返回 HTTP 429「该模型当前访问量过大」，导致识别直接失败，用户体验受损。
- 失败处理现状是「同一模型」用强调 JSON 的提示词重试 1 次，对限流/模型下线无效；且 client 把所有上游错误吞成 `IllegalStateException`，丢失 HTTP 状态码，无法区分「换个模型能救」与「换模型也没用」。
- 智谱同账号下还有其它视觉模型可作备用；同一 api-key 下模型名只是请求参数，故障转移无需额外凭据。

## 2. 目标

- 配置主模型（`ocr.model`，不变）+ 有序备用模型列表（新增 `ocr.fallback-models`，JSON 数组，默认空=功能关闭、行为与现状一致）。
- 主模型出现「可转移错误」（429 限流 / 5xx / 404 / 网络超时）时，按顺序自动切换到备用模型；出现「不可转移错误」（401/403 密钥问题、400/422 参数或图片问题）时立即失败，不切换、不熔断，避免掩盖配置错误。
- Redis 熔断降级锁定：某模型连续失败达到阈值后写入短期熔断标记，熔断窗口内直接跳过该模型（不再每次先撞主模型），到期自动放行一次试探；任一模型成功立即清除其失败计数与熔断标记。
- 全部候选模型都不可用时快速失败，返回统一「识别繁忙，稍后再试」文案，不打满上游、不扣用户当日次数。

## 3. 非目标

- 不做跨供应商路由（仅智谱同账号不同模型）、不做按成功率/延迟的智能负载均衡
- 不做 admin 专用模型管理页（复用 change11 通用系统配置页维护 ocr.* 键）
- 不改识别结果口径（仍是每 100g 宏量 + 后端 4/4/9 重算 kcal）、不改指纹缓存/每日限流规则
- 不改小程序端（故障转移对调用方透明，返回结构不变）

## 4. 范围

- SQL：`sql/change13_ocr_fallback.sql`（`ocr.fallback-models` / `ocr.circuit-fail-threshold` / `ocr.circuit-open-seconds` 3 条种子 + schema_migrations 补账 version=13）
- 后端：
  - `common/ai/AiCallException`（新增，携带 HTTP 状态码与「是否可转移」标记）
  - `common/ai/ZhipuAiChatClient`（透出状态码并分类，不再统一吞成 IllegalStateException）
  - `common/constant/ProjectConfigKeyConstant`（新增 3 个配置键）
  - `service/impl/AiRecognizeService`（候选链构建、熔断判定/计数/清除、逐模型调用编排；同模型仅在 JSON 解析失败时加强提示词重试一次）
  - `common/constant/CommonConstant` / `ExceptionConstant`（识别繁忙错误码与文案）
- 测试：扩展 `AiRecognizeServiceTest`（故障转移/熔断/不可转移/全熔断/默认关闭）

## 5. 验收

- `tasks.md` 全部勾完；`mvn test` 全绿
- 主模型返回 429 时自动切到备用模型并成功返回；401 时立即失败且不尝试备用模型
- 主模型连续失败达阈值后，后续请求直接走备用模型（日志无主模型调用）；熔断 TTL 到期后自动恢复探测
- 备用模型成功后，主模型失败计数/熔断被清除（或自然到期），不影响后续回切
- `ocr.fallback-models` 为空数组时，识别行为与 change12 现状完全一致
- 所有候选均不可用时返回统一繁忙文案，且不递增当日识别计数

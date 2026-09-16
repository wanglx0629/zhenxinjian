# OCR 模型故障转移与熔断降级 — Design

## 1. 现状与问题

`AiRecognizeService.recognize` 读取单值 `ocr.model`，经 `invokeWithRetry` 调 `AiChatClient.chat(model, ...)`；失败时用同一模型 + 强调 JSON 的提示词重试 1 次。`ZhipuAiChatClient` 对所有上游失败（`response.isSuccess()==false` 或抛异常）统一抛 `IllegalStateException("zhipu chat failed: " + msg)`，丢失 HTTP 状态码，调用方无法判断错误类型。

实测免费模型高峰返回 HTTP 429，同模型重试无效，直接导致识别失败。

## 2. 总体结构

```
recognize()  开关/校验/指纹缓存/每日限流/key 检查（不变）
   └─ invokeWithFallback(candidates, image, timeout)
        for model in 候选链(主 + 备，去重去空):
           if 熔断标记存在 → 跳过
           try:
               out = chat(model)                 # 服务端错误直接抛 AiCallException
               items = parseAndNormalize(out)    # JSON 解析失败 → 同模型加强提示词重试 1 次
               clearCircuit(model)               # 成功：清失败计数 + 熔断标记
               return items
           catch AiCallException e:
               if !e.transferable → 立即抛业务异常（不切换、不熔断）
               recordFailure(model)              # INCR 失败计数；达阈值置熔断标记(TTL)
               continue
        全部候选耗尽/熔断 → 抛「识别繁忙」业务异常
```

候选链在一次请求内固定（读取时刻的配置快照），熔断状态在 Redis 跨请求共享。

## 3. 错误分类（AiCallException）

新增 `common/ai/AiCallException extends RuntimeException`，字段：`int httpStatus`、`boolean transferable`。

`ZhipuAiChatClient.chat`：
- SDK `invokeModelApi` 返回非成功：以 `response.getCode()`（HTTP 状态，如 429/401/500）为准；`getError()` 业务码仅作日志。分类：
  - 可转移：429、5xx、404、408
  - 不可转移：401、403、400、422 及其余 4xx
- 网络/IO/超时异常（SDK 抛出的 RuntimeException）：视为可转移（可能是临时抖动）
- 响应体缺失/无 choices 等结构性异常：可转移
- 成功但内容无法解析（JSON）：这是「可重试但不必换模型」的情形，由 service 层用同模型加强提示词重试 1 次处理（沿用现状），不计入熔断失败

## 4. 熔断状态（Redis，复用 RedisUtils）

- 失败计数 key：`zhenxinjian:ocr:modelfail:{model}`，`incrementWithTtl` 固定窗口计数，TTL 取熔断窗口秒数（计数与熔断同寿，窗口内连续失败才触发）
- 熔断标记 key：`zhenxinjian:ocr:circuit:{model}`，计数达到 `ocr.circuit-fail-threshold` 时 `setEx(..., ocr.circuit-open-seconds)`
- 判定：`redisUtils.get(circuitKey) != null` 即熔断中，跳过
- 恢复：① 标记 TTL 到期自动放行（下一次调用即试探，成功则 `clearCircuit`，失败重新计数）；② 某模型调用成功立即删除其计数与熔断 key
- 所有 key 均带 TTL，无 BigKey/无永久会话，符合 Redis 规约；模型名作为 key 组成前做字符约束（配置来源可信，仅 trim，不接受空白模型名）

## 5. 配置（project_config，change13）

| key | value_type | 默认 | 说明 |
| --- | --- | --- | --- |
| `ocr.fallback-models` | 4(JSON) | `[]` | 有序备用视觉模型名；空数组=关闭故障转移 |
| `ocr.circuit-fail-threshold` | 2(数字) | `3` | 连续失败几次触发熔断 |
| `ocr.circuit-open-seconds` | 2(数字) | `300` | 熔断锁定时长（秒），到期自动试探恢复 |

经 `ConfigService.getStringList` 读取（非法 JSON fail-open 为空列表）；阈值/时长用 `getInt` 带默认值并做下限保护（阈值 ≥1，时长 ≥1s）。主模型 `ocr.model` 保持不变。

## 6. 同模型重试口径调整

- 现状：任何异常都同模型重试 1 次（含 429，浪费配额）。
- 调整：**仅 JSON 解析失败**（`parseAndNormalize` 抛 IllegalArgumentException）时同模型加强提示词重试 1 次；`AiCallException`（服务端错误/网络）不做同模型重试，直接进入转移/熔断逻辑。

## 7. 对现有行为的兼容

- `ocr.fallback-models=[]`（默认）：候选链只有主模型；主模型可转移错误 → 记录失败/可能熔断 → 候选耗尽返回「识别繁忙」。对用户而言错误文案与现状「识别失败，请换张清晰的食物图片重试」的区别需明确：新增统一繁忙文案仅在「可转移错误耗尽候选」时使用；不可转移错误沿用现有识别失败文案。
- 非食物图（解析出空 items）仍视为成功：返回空、不写指纹、不计次（不变），但会 `clearCircuit`（一次成功调用）。
- 指纹缓存、每日限流（仅成功计次）、kcal 守恒重算、脏值丢弃/10 项截断均不变。

## 8. 测试策略

扩展 `AiRecognizeServiceTest`（mock `AiChatClient` + `ConfigService` + `RedisUtils`）：
1. 主模型 429 → 备用模型成功：返回备用结果，主模型计数 +1
2. 主模型熔断中：直接调备用、不调主模型
3. 备用成功：主/备熔断与计数被清理（验证 clear 调用）
4. 主模型 401：立即失败，不调备用，不写熔断
5. 全部模型熔断：无任何模型调用，快速返回繁忙文案
6. 阈值触发：连续失败 N 次后出现熔断 key
7. fallback 为空：行为等同单模型（一次调用 + 解析失败重试）

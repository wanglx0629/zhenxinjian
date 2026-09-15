## Purpose

拍照识物能力：用户上传食物照片，系统经视觉模型识别图中食物并给出候选列表（名称 + 每 100g 宏量与热量），供用户确认后走现有饮食记录链路。识别的全部运行时参数（key/模型/开关/限次/超时）由 `project_config` 运营配置，后台修改即时生效。

## ADDED Requirements

### Requirement: 食物图片识别

系统 SHALL 提供 `POST /food/recognize` 接口：登录用户（含游客）上传食物图片（jpg/jpeg/png/webp，≤10MB），后端调用配置表指定的视觉模型识别图中食物，返回候选食物列表（1–10 项，含名称与每 100g 碳水/蛋白质/脂肪/热量）。热量 MUST 由后端按 4/4/9 由宏量重算，不采信模型输出；宏量脏值（负数或超界）MUST 丢弃。接口 MUST 不持久化图片与识别历史（即用即弃）。

#### Scenario: 上传食物图片返回候选

- **WHEN** 用户上传一张包含食物的照片
- **THEN** 返回 ≥1 项候选，每项含中文食物名与每 100g 宏量；kcal 恒等于 carb×4+protein×4+fat×9（±0.1）

#### Scenario: 非法文件被拒

- **WHEN** 用户上传 .gif 或 12MB 的文件
- **THEN** 返回参数校验失败文案，不产生模型调用与限流计数

#### Scenario: 一图多食返回多项候选

- **WHEN** 用户上传包含多个食物的照片（如一桌菜）
- **THEN** 返回多个候选项（≤10 项），每项独立含名称与每 100g 宏量，供用户在页面分别勾选与修改克数后入库

#### Scenario: 非食物图返回空候选

- **WHEN** 用户上传不含食物的照片（如风景/人物/文件）
- **THEN** 返回空候选列表（业务成功），前端空态提示，不报识别错误；本次不写入指纹缓存，当日剩余次数不变

### Requirement: 识别开关与配置热切换

系统 SHALL 从 `project_config` 读取识别参数：`ocr.api-key`（SECRET 加密）、`ocr.model`、`ocr.enabled`、`ocr.daily-limit`、`ocr.timeout-seconds`。`ocr.enabled=false` 时接口 MUST 返回统一关闭文案（fail-closed）；api-key 缺失 MUST 返回配置缺失文案；后台修改任一键值后下一次请求 MUST 即时生效（无需重启）。

#### Scenario: 关闭开关后拒识

- **WHEN** admin 将 `ocr.enabled` 改为 false 后用户调用识别
- **THEN** 返回「拍照识别功能暂未开放」业务错误，不产生模型调用与限流计数

#### Scenario: 后台换 Key 即时生效

- **WHEN** admin 在系统配置页更新 `ocr.api-key` 为新密钥后用户识别
- **THEN** 下一次识别使用新密钥正常返回（SECRET 后台始终脱敏下发，传 ****** 表示不修改）

### Requirement: 识别每日限流

系统 SHALL 按用户维度做每日识别次数限流：Redis 原子计数（键含 userId 与日期，TTL 至当日结束），超过 `ocr.daily-limit` 的请求 MUST 返回限流文案且不产生模型调用；计数 MUST 仅在模型调用成功后递增（参数校验失败、模型失败不计次）；次日自动恢复。

#### Scenario: 超出每日限次

- **WHEN** 用户当日已成功识别 `ocr.daily-limit` 次后再次调用
- **THEN** 返回「今日识别次数已用完」业务错误；次日 0 点后可再次识别

#### Scenario: 失败不计次

- **WHEN** 用户上传合法图片但模型调用失败（超时/解析失败）
- **THEN** 当日剩余次数不变

### Requirement: 同图结果缓存复用

系统 SHALL 对上传图片字节计算 SHA-256 指纹并缓存识别结果（缓存键与用户无关，全站共享；TTL 由 `ocr.cache-ttl-hours` 配置，默认 24 小时）。指纹命中的重复提交 MUST 直接返回缓存结果：不调用模型、不递增限流计数、不做限流检查。缓存 MUST 在模型调用成功后写入。

#### Scenario: 同一图片重复提交命中缓存

- **WHEN** 用户（或任何用户）在 TTL 内再次提交字节完全相同的图片
- **THEN** 返回与首次一致的识别结果，当日剩余次数不变，后端无模型调用

#### Scenario: 不同图片不命中

- **WHEN** 用户提交与缓存中指纹不同的图片
- **THEN** 正常走限流检查与模型调用流程

### Requirement: 识别结果进入记录链路

系统 SHALL 保证候选食物进入饮食记录时复用现有手动录入链路（source=3）：名称命中敏感词 MUST 被现有 SensitiveWordFilter 拦截，宏量守恒与区间校验 MUST 与手动录入口径一致。

#### Scenario: 候选名称含敏感词被拦

- **WHEN** 用户将识别候选保存为记录且名称包含敏感词
- **THEN** 保存返回「内容包含违规词汇，请修改后重试」，不入库

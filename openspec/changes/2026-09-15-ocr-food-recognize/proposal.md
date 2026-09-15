# OCR 食物识别（GLM 视觉模型）— Proposal

| 项目 | 内容 |
| ---- | ---- |
| Change ID | 2026-09-15-ocr-food-recognize |
| 类型 | feature（二期启动：AI 识物） |
| 涉及端 | `apps/zhenxinjian-backend` + `apps/zhenxinjian-uniapp`（小程序端） |
| 关联任务 | 任务#4 OCR 食物识别 + 企业 LLM 兜底 |
| 供应商选型 | 智谱 BigModel（用户已确认）：**GLM-4.6V-Flash**（免费档视觉理解模型），Java SDK `ai.z.openapi:zai-sdk` |
| Key 存放 | `project_config` SECRET（用户已确认）：**所有运行时参数全部走配置表**，admin 后台可随时换，无需发版 |

## 1. 背景

- MVP 一期明确「拍照识别 / AI 识物」为二期范围（PRD 范围外清单）；本项目所有者现确认启动二期该功能
- 用户在手写食物名时成本高、宏量靠猜——拍照识别直接给出候选食物 + 每 100g 宏量，降低记录门槛
- change11 已预置 `ocr.api-key`（SECRET）配置种子，SECRET 加密/脱敏体系已上线并完成 E2E 验证
- 用户已提供智谱 API Key（聊天中交付，**不写入任何 git 跟踪文件**；本地经 admin 配置接口加密落库，建议开发完成后于智谱控制台轮换）

## 2. 目标

- 新增 `POST /food/recognize`：登录用户上传食物图片（multipart，jpg/jpeg/png/webp ≤10MB），后端调智谱 GLM-4.6V-Flash 视觉理解，返回候选食物列表（名称 + 每 100g 碳/蛋/脂/热量）
- 识别调用全部参数走 `project_config`：`ocr.api-key`（SECRET）、`ocr.model`、`ocr.enabled`、`ocr.daily-limit`、`ocr.timeout-seconds`、`ocr.cache-ttl-hours`——admin 改配置即时生效（写路径已失效缓存）
- 每用户每日识别次数限流（Redis 计数），防免费额度被刷；**同图指纹缓存**：图片字节 SHA-256 指纹为 key 缓存识别结果，同一张图重复提交（无论谁、多少次）直接回缓存，不调模型不计次，彻底堵住「拿一张照片刷接口」
- 模型输出 kcal 一律由宏量按 4/4/9 后端重算覆盖（不信模型算术）；候选名称进入保存链路时复用现有敏感词过滤与守恒校验
- 小程序「添加饮食」页新增「拍照识别」入口 + 识别结果确认页（勾选候选 → 带入保存）

## 3. 非目标

- 不做识别历史留存、图片持久化存储（图片即用即弃，不落 MinIO/OSS）
- 不做条形码 / 包装标签 OCR 精修、多轮对话式修正
- 不做后台单独管理页（系统配置页已通用覆盖 ocr.* 键）
- 不做流式输出 / 多模型路由（单模型可配）

## 4. 范围

- SQL：`sql/change12_ocr_config.sql`（`ocr.model`/`ocr.enabled`/`ocr.daily-limit`/`ocr.timeout-seconds`/`ocr.cache-ttl-hours` 5 条种子 + 补账 version=12；api-key 不落 git，运维经后台录入）
- 后端：`AiChatClient` 薄接口 + 智谱 SDK 实现、`AiRecognizeService`（配置读取/限流/Prompt/解析/守恒重算）、`FoodRecognizeController`、`FoodRecognizeVO`、错误码与文案
- 小程序：`api/ai.ts`、`pages/record/recognize.vue` 识别结果页、`add.vue` 入口按钮、`pages.json` 注册
- 测试：AiRecognizeService 单测（开关/限流/解析容错/守恒重算）+ Controller 测试 + 真实 key 本地 E2E

## 5. 验收

- `tasks.md` 全部勾完；`mvn test` 全绿
- admin 后台改 `ocr.enabled=false` 后识别接口返回统一关闭文案；改 `ocr.daily-limit` 后限流阈值即时生效
- 上传食物图片返回 ≥1 个候选，`kcal` 恒等于 `carb*4+protein*4+fat*9`（±0.1）
- 同一用户超过当日限次后返回限流文案；次日恢复
- 同一张图片重复提交命中缓存：响应与首次一致，当日剩余次数不变，后端无模型调用（日志可证）
- 小程序拍照/选图 → 识别 → 勾选 → 保存记录全链路通畅，保存数据与手动记录一致（敏感词/守恒校验生效）

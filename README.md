# 臻心减（zhenxinjian）项目文档

> 产品载体：微信小程序（基于 UniApp，一套代码可发布 H5 / 微信小程序）
> 脚手架来源：[create-luote](https://create.luote996.cn/guide/getting-started.html) 三端工程
> 文档版本：V1.1　整理日期：2026-09-04
> 依据资料：《臻心减-V1.1-最终版PRD-开发交付版》《臻心减_PRD_V1.1_含食物库》《V1.1 高保真原型》《后端开发落地核对清单》

## 目录

- [第一部分 · 项目介绍](#第一部分--项目介绍)
- [第二部分 · 技术栈说明](#第二部分--技术栈说明)
- [第三部分 · 开发规范](#第三部分--开发规范)

---

# 第一部分 · 项目介绍

## 1. 产品定位

帮助健身新人小白轻松开启生活化减脂第一步，做**私人陪伴式互联网健身搭子**。

- 面向缺乏营养学知识、但有热量 / 宏量营养素管理需求的普通用户。
- 追求**简单、准确、有陪伴感**：把专业的 BMR/TDEE/宏量计算、碳循环 / 532 减脂模式、食物库与饮食记录封装成「零学习成本」的工具。
- 所有健康计算结果内置免责声明：**仅作生活化减脂参考，非医疗建议**。

## 2. 目标用户

健身减脂新人小白：对热量 / 宏量计算有需求但缺乏专业知识，希望用简单工具完成「算得准、记得快、有提醒、能坚持」。

## 3. 团队与节奏

- 团队配置：1 名产品经理 + 1 名开发。
- 项目节奏：无硬性时间要求，以打磨产品体验为核心，敏捷迭代，每完成一个 Phase 即可内部测试。

## 4. 一期（MVP）核心范围

本期聚焦四大核心能力：**计算 + 食物库 + 记录 + 提醒**。

| 能力 | 说明 |
|------|------|
| 核心计算 | BMR / TDEE / 宏量目标；532 碳水渐降模式、碳循环模式 |
| 食物库 | 内置约 200+ 常见食物（9 大分类），支持搜索、筛选、自定义添加 |
| 饮食记录 | 食物库选择或手动输入，自动算宏量、实时累计、超标高亮 |
| 提醒 | 三餐饮食提醒 + 微信订阅消息；超标预警与微调建议 |

**一期明确不做：** 食物拍照识别 / AI 识物 / 条形码扫描；运动课程、社区社交、榜单打卡；付费会员、广告、营销体系；自动体重曲线、全自动数据复盘、详细微量元素。

## 5. 全局核心规则（全功能通用，优先落地）

1. 所有用户数据（个人资料、减脂模式、周期、饮食、体重）**永久云端留存，不自动清空**。
2. 用户切换减脂模式：**自动终止当前周期、清空当期进度、重启全新周期**。
3. 所有健康计算结果内置免责声明，仅作生活化减脂参考，非医疗建议。
4. 所有用户输入数值做人体合理区间校验，**后端二次兜底**，拒绝异常数据计算。
5. 用户隐私数据加密存储、禁止明文传输。
6. 游客体验：新用户首次进入默认授予 **3 天完整功能游客体验**，到期需微信授权登录；游客数据临时留存，登录后自动迁移合并；过期未登录临时数据保留 7 天后清空。
7. **减脂缺口基准**：532 与碳循环共用「基准热量 = TDEE − X」，X 由用户自选（温和 200 / 标准 300 / 积极 400，默认 200）。

## 6. 功能模块清单

### 6.1 基础账号能力（微信登录）
- 微信一键授权登录，无需手机号，`openid` 唯一绑定用户账号。
- 登录状态持久化，下次打开自动加载个人数据；授权失败重试、游客浏览兜底（3 天体验）。
- 首次访问弹窗提示游客规则；进入第 4 天 0 点弹出**不可关闭**的强制授权弹窗（仅可授权登录 / 退出小程序）。
- 游客时效、权限、数据迁移**以后端校验为准**，前端计时仅用于展示。

### 6.2 基础代谢 & 每日总消耗（BMR / TDEE）
- BMR 采用 Mifflin-St Jeor 公式：
  - 女性：`BMR = 10×体重(kg) + 6.25×身高(cm) − 5×年龄 − 161`
  - 男性：`BMR = 10×体重(kg) + 6.25×身高(cm) − 5×年龄 + 5`
- TDEE = BMR × 活动系数：久坐 1.2 / 轻度 1.375 / 中度 1.55 / 高度 1.725。
- 用户修改身体 / 运动数据：实时重算、自动覆盖、留存历史、支持多版本追溯。

### 6.3 标准宏量配比
- **532 模式**：固定 50% 碳水 / 30% 蛋白 / 20% 脂肪占比，由「基准热量 = TDEE − X」直接切分。
- **碳循环模式**：按图片公式独立核算（目标体重驱动碳池 / 脂肪池，当前体重驱动蛋白）。
- 输出每日总热量、碳水 / 蛋白 / 脂肪克数 + 热量占比。

### 6.4 碳循环模式（重点 · 图片公式定稿）
输入项：当前体重、目标体重（≤ 当前体重）、周期天数（5/7/10/14，默认 7）、脂肪系数 cfc（0.8 严格 / 1.0 容易，默认 0.8）、运动日标记。

- 每日蛋白质 = 当前体重 × 1.5（g），每天固定。
- 周期总碳水（碳水池）= 目标体重 × 2.5 × cycleDays（g）。
- 周期总脂肪（脂肪池）= 目标体重 × cfc × cycleDays（g）。
- 日型分配（按周内 2 高 / 2.2 中 / 2 低，周期按比例放大）：

| 日型 | 碳水 | 脂肪 |
|------|------|------|
| 高碳日 | 总碳水 × 50% ÷ 2 | 总脂肪 × 15% ÷ 2 |
| 中碳日 | 总碳水 × 35% ÷ 2.2 | 总脂肪 × 35% ÷ 2.2 |
| 低碳日 | 总碳水 × 15% ÷ 2 | 总脂肪 × 50% ÷ 2 |

- 运动日优先占高碳位、其次中碳位；休息日落低碳位；不标记时默认轮播 `高·中·低·低·中·高·低`。
- 单日克数四舍五入到整数克；周期总量守恒允许 ±1g 误差。
- 切换 / 提前结束周期清空总碳水池、作废当期数据；新开周期重新核算、相互独立。

### 6.5 532 碳水渐降模式（重点）
- 基准热量 = TDEE − X（X 自选 200/300/400，默认 200）。
- 固定宏量占比：碳水 50% / 蛋白 30% / 脂肪 20%（碳水 = 总热量×50%÷4，蛋白 = ×30%÷4，脂肪 = ×20%÷9）。
- 以自然月为周期，4 个递进阶段：① 月初适应期 ② 经期适配期（经期自动 +15g 碳水 / +60kcal）③ 经后高效期 ④ 平台突破期。
- 体重联动调碳：每周一空腹记录体重，2 周为一个观测单元；第 3 周无下降（平台）→ 碳水 −20g、总热量 −80kcal，蛋白脂肪不变。
- 兜底：单次仅降 20g，禁止连续多周递减；体重恢复下降即停止下调。

### 6.6 基础食物库
- 内置约 200+ 常见食材 / 家常菜 / 速食 / 饮品，9 大分类：主食、肉蛋、蔬菜、水果、奶豆、坚果油脂、饮品、零食 / 其他、中式菜肴。
- 支持关键词模糊搜索（防抖 300ms）、品类筛选、历史搜索、热门推荐。
- 选择食物 + 填写重量（g / 个 / 碗 / 勺等单位换算），自动计算单餐碳 / 蛋 / 脂 / 热量。
- 支持用户自定义添加食物（名称唯一、宏量≈热量 ±10% 校验），可编辑 / 停用（被引用后软删，保证历史完整）。
- 实际摄入 = 每 100g 营养值 ×（份量 / 100）。

### 6.7 饮食记录 & 超标预警
- 餐别：早餐 / 午餐 / 晚餐 / 加餐；按时间智能推荐餐别。
- 单条饮食可增、删、改，实时累加当日总摄入；多次记录动态覆盖、实时更新超标状态。
- 进度条颜色：充足（80%~100%）绿色、不足（<80%）黄色、超标（>100%）红色 + 「已超标 XXg」。
- 自动比对实际摄入 vs 个人标准宏量，单项 / 总热量超标分别高亮，并附带小白友好的微调建议文案。

### 6.8 饮食提醒
- 总开关 + 早 / 午 / 晚餐独立开关与时间选择（默认 08:30 / 12:00 / 18:30）。
- 每日单次提醒，已记录饮食当日不再重复推送。
- 通过微信**订阅消息**推送；首次开启引导 `wx.requestSubscribeMessage()` 授权。

## 7. 信息架构（小程序页面）

```
臻心减小程序
├── 启动/登录流程
│   ├── 游客引导页（P01）
│   └── 授权登录页 / 到期强制授权弹窗（P15）
├── 核心计算流程（首次引导）
│   ├── 个人信息录入页（P03，含目标体重）
│   ├── 活动水平选择
│   ├── 热量缺口确认（X 自选）
│   └── 碳蛋脂结果页（P04）
├── 模式与计划
│   ├── 模式选择页（P05：532 / 碳循环）
│   ├── 碳循环周期设置页（P06）
│   ├── 碳循环计划页（P07）
│   ├── 532 月度计划页（P08）
│   └── 体重记录与调碳页（P09）
├── 首页 Dashboard（P02）
│   ├── 今日热量环形进度
│   ├── 三大营养素进度条
│   ├── 添加饮食入口 / 重新计算入口
│   └── 当日记录列表（P12）
├── 添加饮食流程
│   ├── 餐别选择
│   ├── 食物搜索页（P10）
│   ├── 食物详情/选择页（P11）
│   ├── 自定义添加食物页
│   └── 手动输入（保留）
├── 食物库
│   ├── 分类浏览 / 搜索 / 详情
│   └── 我的自定义食物
└── 我的（P14）
    ├── 个人信息管理 / 重新计算
    ├── 模式切换（P16 确认）
    ├── 提醒设置（P13）
    ├── 我的自定义食物
    └── 关于
```

> 原型共 16 页、43 项功能标注（F01–F27），详见高保真原型与 PRD §6「原型页面 ↔ PRD 功能对照表」。

## 8. 核心数据模型（概要）

| 表 | 说明 |
|----|------|
| `users` | 用户：openid、昵称、头像、注册 / 登录时间 |
| `user_body` | 身体数据与计算结果：性别、年龄、身高、体重、目标体重、活动系数、BMR、TDEE、缺口、目标宏量 |
| `foods` | 食物库：名称、分类、每 100g 热量 / 蛋白 / 脂肪 / 碳水、内置 / 自定义、单位换算 JSON、软删状态 |
| `diet_records` | 饮食记录：用户、日期、餐别、来源（食物库 / 手动）、食物、份量、宏量、热量、备注 |
| `user_reminders` | 提醒设置：总开关、三餐开关与时间、订阅授权状态 |

> 实际建表以脚手架 `gen-crud.js` 生成与 MySQL 规范为准（软删、时间字段、命名约定）。

## 9. 非功能需求

- **性能**：首屏 ≤ 2s；页面切换 ≤ 300ms；计算结果 ≤ 100ms；数据提交 ≤ 1s；食物搜索 ≤ 200ms。
- **兼容性**：微信 iOS 12+ / Android 8+ / 微信 8.0+；微信开发者工具可正常调试。
- **安全**：openid 等敏感数据仅存服务端；全链路 HTTPS；用户输入 XSS 过滤；内置食物库只读，用户仅能操作自己的自定义食物。
- **可靠性**：核心计算逻辑后置，前端参数不可篡改结果；游客时效 / 权限 / 数据迁移由后端兜底校验。
- **数据备份**：身体数据与饮食记录每日自动备份；食物库定期全量备份。

## 10. 上线验收标准

1. 全部核心功能流程通畅，无致命 bug、无卡顿。
2. 所有计算逻辑（BMR / TDEE / 碳循环图片公式 / 532 占比）、周期逻辑、体重调碳逻辑 100% 闭环生效。
3. 数据永久留存、模式切换重置周期正常生效。
4. 小白零学习成本可完整使用全流程（对照原型 16 页逐一走查）。
5. **重点验收（图片公式）**：当前 57kg、目标 55kg、cfc=0.8、周期 7 天时，三个日型单日「碳水+脂肪」= 241/23、153/49、72/77（±1g）；蛋白每天 = 86g。

## 11. 排期建议

| 阶段 | 内容 |
|------|------|
| Phase 1 | 微信登录 + BMR/TDEE/宏量核心计算流程 |
| Phase 2 | 食物库 + 饮食记录打通 + 首页 Dashboard（可适当延长，保证数据准确与搜索体验） |
| Phase 3 | 提醒设置 + 微信订阅消息接入 |
| Phase 4 | 联调测试 + Bug 修复 + 体验优化 |
| 上线 | 提交微信审核，灰度发布 |

---

# 第二部分 · 技术栈说明

## 1. 总体架构

臻心减采用**三端 + 中间件**架构，代码统一放在 `apps/` 目录：

| 目录 | 端 | 技术栈 | 说明 |
|------|----|--------|------|
| `apps/zhenxinjian-backend` | 后端服务 | Spring Boot 3.5 + Java 25 | 提供 REST API、WebSocket、认证、计算、文件存储 |
| `apps/zhenxinjian-front` | 运营管理后台（Web） | Vue 3 + Element Plus + Vite | 管理端：用户管理、食物库维护、数据查看等 |
| `apps/zhenxinjian-uniapp` | 用户端小程序 | UniApp（Vue 3）+ 微信小程序 | 面向 C 端用户，一套代码可发布 H5 / 微信小程序 |

中间件与外部服务：

- **MySQL 8**：业务数据持久化（用户、身体数据、食物库、饮食记录、提醒设置）。
- **Redis**：登录 Token、验证码、登录失败计数、JetCache 缓存、会话。
- **对象存储**：MinIO（自建）或 阿里云 OSS（生产），用于头像 / 文件上传（默认关闭，按需开启）。
- **Spring AI Alibaba（通义千问 DashScope）**：已集成，为后续 AI 能力预留（一期不做 AI 识物）。
- **微信开放平台**：小程序 `wx.login` 授权登录、订阅消息推送。

## 2. 后端技术栈（zhenxinjian-backend）

### 2.1 核心框架与版本

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 25 | 运行时（LTS） |
| Spring Boot | 3.5.16 | 主框架 |
| Spring Security | 随 Boot | 认证鉴权 |
| Spring WebSocket | 随 Boot | 实时通信（Demo / 预留） |
| Spring Validation | 随 Boot | 参数校验（`@Valid`） |
| Spring AOP | 随 Boot | 切面（AI 校验、缓存等） |
| MyBatis-Plus | 3.5.6（spring-boot3-starter） | ORM、分页、逻辑删除、自动填充 |
| MySQL Connector/J | 随 Boot | MySQL 驱动 |
| Spring Data Redis（Lettuce） | 随 Boot | Redis 访问 |
| JetCache | 2.7.7（redis-lettuce） | 多级缓存（本地 Caffeine + 远程 Redis） |
| JJWT | 0.12.5 | JWT 生成与解析 |
| SpringDoc OpenAPI | 2.8.8 | 接口文档（Swagger UI） |
| Spring AI Alibaba | 1.0.0.2（BOM）+ Spring AI 1.0.0 | 通义千问 DashScope 集成 |
| Hutool | 5.8.32 | 工具包 |
| MinIO | 8.5.12 | 对象存储客户端 |
| 阿里云 OSS SDK | 3.18.1 | OSS 客户端 |
| Lombok | 1.18.38 | 简化 POJO |
| Spring Boot Test | 随 Boot | 单元测试 |

### 2.2 分层与包结构

根包：`cn.zhenxinjian`（启动类 `ZhenxinjianApplication`）

```
cn.zhenxinjian
├── ZhenxinjianApplication.java   # 启动类
├── common
│   ├── ai/                       # Spring AI 校验切面（AiValidate/AiValidationAspect...）
│   ├── cache/                    # 缓存：CacheValueGuard、UserCacheService、缓存预热
│   ├── constant/                 # 常量：Cache/Common/Exception/Storage Constant
│   ├── exception/                # BusinessException + GlobalExceptionHandler
│   ├── query/                    # PageQuery 分页基类
│   ├── result/                   # Result 统一响应
│   └── utils/                    # JwtUtils、RedisUtils、UserContext
├── config/                       # MybatisPlus / Security / Storage / Swagger / WebSocket / Properties
├── controller/                   # Auth / User / File Controller
├── domain/
│   ├── po/                       # 持久化对象（@TableName，对应表）
│   ├── dto/                      # 入参（LoginDTO / RegisterDTO / UserDTO）
│   ├── query/                    # 查询入参（UserQuery）
│   └── vo/                       # 出参（LoginResultVO / UserVO / CaptchaVO...）
├── mapper/                       # MyBatis-Plus Mapper
├── security/                     # JWT 过滤器、认证入口点、拒绝处理器、JSON 写出
├── service/                      # 业务接口 + impl/ 实现（StorageService 文件存储抽象）
└── websocket/                    # WS Handler / Interceptor / SessionRegistry
```

分层调用约定：`controller → service（interface）→ service/impl → mapper`；对象分 `po / dto / query / vo`。

### 2.3 关键配置

- 服务端口 `8080`，统一上下文 `/api`（接口前缀 `http://localhost:8080/api`）。
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`；OpenAPI：`/api/v3/api-docs`。
- 配置文件：`application.yml`（主配置）、`application-dev.yml`（开发，MySQL/Redis 连接、SQL 日志）、`application-prod.yml`（生产，SSL、密钥走环境变量）。
- MyBatis-Plus：`id-type=auto`、逻辑删除字段 `deleted`（0 正常 / 1 删除）、下划线转驼峰。
- JWT：默认 120 分钟过期，提前 30 分钟可续期；Token 存 Redis。
- 文件上传：单文件上限 10MB，允许 jpg/jpeg/png/gif/webp/pdf。

### 2.4 环境要求与启动

- JDK 25+、Maven 3.9+、MySQL 8、Redis 5+。
- 初始化：执行 `src/main/resources/data.sql`（建库、建表、演示数据）。

```bash
cd apps/zhenxinjian-backend
mvn spring-boot:run
```

- 关键环境变量（生产）：`DB_HOST/DB_PORT/DB_USER/DB_PASSWORD`、`JETCACHE_REDIS_URI`、`OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET`、`WS_ENABLED/WS_PATH` 等。

## 3. 运营管理后台（zhenxinjian-front）

### 3.1 技术栈与版本

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | ^3.4.27 | 前端框架（Composition API） |
| TypeScript | ^5.4.5 | 类型系统 |
| Vite | ^5.4.21 | 构建 / 开发服务器 |
| vue-tsc | ^2.0.19 | 类型检查 + 构建 |
| Element Plus | ^2.7.4 | UI 组件库 |
| @element-plus/icons-vue | ^2.3.1 | 图标 |
| Vue Router | ^4.3.2 | 路由 |
| Pinia | ^2.1.7 | 状态管理 |
| Axios | ^1.19.0 | HTTP 请求 |
| ECharts | ^5.5.0 | 图表 |

### 3.2 目录结构

```
src/
├── api/          # 一资源一文件（auth.ts / user.ts ...），types.ts 放类型
├── component/    # 通用组件（CaptchaInput / TeLogo / WsStatusCard / DemoChart...）
├── layout/       # MainLayout 主布局
├── router/       # 路由（index.ts）
├── store/        # Pinia（user.ts）
├── styles/       # 全局样式（global.css）
├── utils/        # request.ts（Axios 封装）、ws.ts（WebSocket）
├── view/         # 页面：home / login / register / users / websocket
├── App.vue
└── main.ts
```

### 3.3 约定与启动

- 路径别名 `@` → `src`。
- 开发服务器默认端口 `5173`，`/api` 代理到 `http://localhost:8080`（含 WebSocket 代理），可用 `VITE_API_PROXY_TARGET` / `VITE_APP_PORT` 覆盖。

```bash
cd apps/zhenxinjian-front
npm install
npm run dev      # 开发
npm run build    # 类型检查 + 生产构建（vue-tsc && vite build）
npm run preview  # 预览构建产物
```

## 4. 用户端小程序（zhenxinjian-uniapp）

### 4.1 技术栈与版本

| 技术 | 版本 | 用途 |
|------|------|------|
| UniApp（@dcloudio/uni-app） | 3.0.0（Vue3 版） | 跨端框架 |
| Vue | ^3.4.21 | 视图框架 |
| TypeScript | ^4.9.4 | 类型系统 |
| Vite | 5.2.8 | 构建 |
| Pinia | ^2.1.7 | 状态管理 |
| vue-i18n | ^9.1.9 | 国际化（预留） |
| Sass | ^1.77.8 | 样式预处理 |

> UniApp 一套代码可发布：微信小程序（主）、H5、App、支付宝 / 百度 / 抖音等多端（依赖已内置）。

### 4.2 目录结构

```
src/
├── api/          # 接口封装（auth.ts / request.ts / types.ts / user.ts）
├── components/   # 组件（CaptchaInput / TeLogo / WsBoard / WsStatusCard）
├── pages/        # 页面：home / login / mine / register / websocket
├── static/       # 静态资源（tabBar 图标、logo、提示音）
├── store/        # Pinia（user.ts）
├── utils/        # request / ws / storage / notify / throttle
├── App.vue
├── main.ts
├── pages.json    # 页面路由 + tabBar + 全局样式
├── manifest.json # 应用配置（AppID 等）
└── uni.scss      # 全局样式变量
```

> 当前 `pages.json` 为脚手架初始页（登录 / 注册 / 首页 / 消息 / 我的），需按 PRD 信息架构新增身体数据、模式选择、碳循环 / 532 计划、体重记录、食物库、饮食记录、提醒设置等页面。

### 4.3 启动与构建

```bash
cd apps/zhenxinjian-uniapp
npm install

npm run dev:h5            # H5 开发
npm run dev:mp-weixin     # 微信小程序开发（产物用微信开发者工具打开 dist/dev/mp-weixin）
npm run build:h5          # H5 生产构建
npm run build:mp-weixin   # 微信小程序生产构建
npm run type-check        # 类型检查（vue-tsc --noEmit）
```

微信小程序发布前需在 `manifest.json` 配置小程序 AppID，并在微信公众平台配置服务器域名（HTTPS）。

## 5. 三端协作与联调

- **接口规范**：后端统一响应 `Result`（`Result.ok` / `Result.fail`），错误码与文案集中在 `ExceptionConstant`。
- **认证**：登录返回 JWT，前端请求头携带 Token；WebSocket 通过子协议（`Sec-WebSocket-Protocol: bearer`）传递，**禁止放 query**。
- **跨域**：后端 CORS 白名单（`security.cors-allowed-origins`），开发期允许 `localhost:5173/5174`，**禁止配 `*`**。
- **代理**：Web 端 Vite 代理 `/api` → 后端；小程序端在 `utils/request.ts` 配置后端 baseUrl（开发期可在微信开发者工具勾选「不校验合法域名」）。
- **接口文档**：后端 Swagger UI 作为前后端联调契约。

## 6. 开发环境清单

| 软件 | 版本要求 |
|------|----------|
| JDK | 25+ |
| Maven | 3.9+ |
| Node.js | 18+（建议 20 LTS） |
| MySQL | 8.x |
| Redis | 5.x+（建议 6+） |
| 微信开发者工具 | 最新稳定版 |
| MinIO（可选） | 最新版（本地对象存储） |

## 7. 常用命令速查

```bash
# 后端
cd apps/zhenxinjian-backend && mvn spring-boot:run

# 运营后台 Web
cd apps/zhenxinjian-front && npm install && npm run dev

# 小程序 H5
cd apps/zhenxinjian-uniapp && npm install && npm run dev:h5
# 小程序微信端
cd apps/zhenxinjian-uniapp && npm run dev:mp-weixin

# 新增业务 CRUD（三端样板自动生成，勿手写）
node .agents/skills/luote-scaffold/scripts/gen-crud.js Food --zh 食物 \
  --fields "name:string:名称,calorie:long:热量分"
```

---

# 第三部分 · 开发规范

> 规范真源：`.agents/skills/luote-scaffold/`（`conventions.md` / `mysql.md` / `redis.md` / `checklist.md`）与根目录 `AGENTS.md`。

## 0. 硬性边界（红线）

### Always（必须）
- 新增业务 CRUD **必须**执行 `.agents/skills/luote-scaffold/scripts/gen-crud.js`，禁止手抄整套样板。
- 文件头注释：`作者: luote (luote) - https://luote996.cn`；注释另起一行。
- 错误文案统一进 `ExceptionConstant`；接口统一返回 `Result`（`Result.ok` / `Result.fail`）。
- MySQL / Redis 严格遵守本部分 §4、§5。
- 管理端接口加 `@PreAuthorize("hasRole('ADMIN')")`。

### Never（禁止）
- WebSocket Token 放入 query 参数。
- CORS / `Origin` 配置为 `*`。
- 硬编码密钥、JWT、密码（走配置 / 环境变量）。
- 无 TTL 的会话类 Redis Key、BigKey、金额用浮点、软删表乱加普通 UNIQUE。
- 绕过脚本手写整套 CRUD（除非用户明确要求）。
- Controller 拼 SQL、`${}` 拼接用户输入、无上限 `selectList` 对外。

## 1. 通用规范

- 统一响应体 `Result`；业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 兜底。
- 错误提示文案集中在 `common/constant/ExceptionConstant`，不在业务代码散落字面量。
- 分页入参继承 / 使用 `PageQuery`，通过 `toPage()` 转 MyBatis-Plus 分页。
- 当前登录用户通过 `UserContext` 获取，不自行解析 Token。
- Jackson 反序列化 `fail-on-unknown-properties=true`，拒绝多余字段注入。

## 2. 后端规范

### 2.1 分层
```
controller → service（接口）→ service/impl（实现）→ mapper
对象：po（表映射）/ dto（入参）/ query（查询入参）/ vo（出参）
```
- Controller 只做参数接收与结果返回，不写业务 / SQL。
- 事务 `@Transactional` 写在 Service 实现类的 public 方法；注意同类自调用不走代理（拆 Service 或注入自身代理）；只读查询不开大事务。
- 多表一致性写操作必须加事务。

### 2.2 实体与字段
- **无 BaseEntity**：`id` / `deleted` / `create_time` / `update_time` 等字段直接写在各 PO 上，自动填充靠 `MybatisPlusConfig`。
- 逻辑删除：`deleted` 字段 + PO `@TableLogic`（0 正常 / 1 删除）。
- 乐观锁：需要并发控制的表加 `version` + `@Version`（users 已用）。
- 参数校验：DTO 用 `@Valid` + JSR-303 注解；人体数据等业务区间在 Service 二次兜底校验。

### 2.3 安全
- 密码只存 BCrypt 哈希，禁止明文。
- JWT 由 `JwtAuthenticationFilter` 校验；`permit-urls` 仅放开登录 / 注册 / 验证码 / WS 握手等必要路径，不得误放开敏感接口。
- 禁用 / 删除用户 / 改密 / 改角色：`removeToken` + 踢 WebSocket（`kickUser`），保持 HTTP 与 WS 会话一致。
- 生产环境 `JWT_SECRET`、CORS 白名单必须替换默认值，禁止 `*`。
- 日志禁止打印完整身份证、银行卡、明文密码、完整 Token。

## 3. CRUD 代码生成（必须走脚本）

在项目根目录执行：

```bash
# 基础用法
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知

# 自定义字段
node .agents/skills/luote-scaffold/scripts/gen-crud.js Product --zh 商品 \
  --fields "name:string:名称,price:long:价格分"

# 指定生成端 / 预览 / 不生成管理端
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知 --ends backend,front,sql
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知 --dry-run
node .agents/skills/luote-scaffold/scripts/gen-crud.js Notice --zh 通知 --no-admin
```

生成后按需补充：活跃唯一约束、乐观锁、金额 DECIMAL 字段，并同步 PO / DTO / `data.sql` / `ExceptionConstant`。

新增 CRUD 自查：
```
- [ ] 已执行 gen-crud.js（非手写整套）
- [ ] data.sql / ExceptionConstant 已含新实体
- [ ] Swagger：登录 → Authorize → 接口 200
- [ ] front 路由与导航可打开；（可选）uniapp pages.json 可打开
- [ ] 401 跳转登录仍正常
```

## 4. MySQL 规范

### 4.1 库与连接
- 字符集 `utf8mb4` + `utf8mb4_unicode_ci`；引擎 `InnoDB`。
- JDBC 时区 `serverTimezone=Asia/Shanghai`；生产 `useSSL=true`。
- 连接配置走 `DB_HOST/DB_PORT/DB_USER/DB_PASSWORD`，禁止提交真实 `.env`。

### 4.2 命名
- 表名：小写复数蛇形（`users`、`foods`、`diet_records`）。
- 列名：小写蛇形（`create_time`、`user_id`）；Java PO 驼峰，MyBatis-Plus 自动映射。
- 禁止保留字裸用。

### 4.3 必备字段（业务表默认）
| 列 | 类型 | 说明 |
|----|------|------|
| `id` | `BIGINT AUTO_INCREMENT` | 主键 |
| `deleted` | `TINYINT` 默认 0 | 软删，PO `@TableLogic` |
| `create_time` | `DATETIME` | 插入填充 |
| `update_time` | `DATETIME` ON UPDATE | 更新填充 |

可选：`version`（乐观锁 `@Version`）、`create_by` / `update_by`（审计）。

### 4.4 字段类型选型
| 场景 | 类型 |
|------|------|
| 主键 / 外键 | `BIGINT` |
| 短文本 | `VARCHAR(n)`，按业务上限设 n |
| 长文本 | `TEXT` |
| 状态 / 开关 / 软删 | `TINYINT` |
| 金额 | `DECIMAL(p,s)`，**禁止 FLOAT/DOUBLE 存钱** |
| 时间 | `DATETIME` |
| JSON | MySQL 5.7+ `JSON`（能拆列就拆列） |

> 本项目营养克数 / 热量：后端按 `DOUBLE` 存储避免累计漂移（PRD §5③），前端展示四舍五入到整数克；如涉及真实金额一律用 `DECIMAL`。

### 4.5 软删除与唯一约束
- 业务删除用软删，禁止默认物理 `DELETE`（特殊清理脚本除外）。
- 软删后仍需唯一的列，用**生成列**做活跃唯一，勿对可空业务列直接加普通 UNIQUE：

```sql
username_active VARCHAR(64) GENERATED ALWAYS AS (IF(deleted = 0, username, NULL)) STORED,
UNIQUE KEY uk_xxx_username_active (username_active)
```
- 自定义食物「被引用后删除」采用软删 / 停用（`status`），保证历史饮食记录完整。

### 4.6 索引与 SQL
- 必建：主键 `id`；高频等值 / 范围列（`status`、`create_time`、外键列）。
- 禁止：低区分度列滥建索引、过多组合索引、对超长 `TEXT` 整列建普通索引。
- 写操作走 Service；复杂条件用 `LambdaQueryWrapper`；参数用 `#{}` / Wrapper，禁止 `${}` 拼用户输入。
- 列表接口必须分页 / 设 `size` 上限，禁止无上限全表 `selectList`；关键字长度截断。
- 更新实体后若有 JetCache 缓存，记得清缓存。

### 4.7 初始化与迁移
- 本地初始化执行 `data.sql`（建库 / 建表 / 演示数据）。
- 生产**禁止**依赖带 `DROP TABLE` 的全量脚本；用变更脚本或 Flyway/Liquibase。
- 演示账号 `admin/admin123` 仅本地，上线立即改密。

改库自查：`utf8mb4+InnoDB`、软删 + 时间字段、唯一约束考虑软删、索引有依据、PO/data.sql/接口字段一致、生产不用 DROP 全量脚本。

## 5. Redis / 缓存规范

### 5.1 Key 命名
格式：`{业务名}:{数据名}:{id}`，本项目业务前缀为 `zhenxinjian`。

| 用途 | 配置项 | 示例 |
|------|--------|------|
| 登录 Token | `zhenxinjian.redis.token-prefix` | `zhenxinjian:token:{userId}` |
| 验证码 | `zhenxinjian.redis.captcha-prefix` | `zhenxinjian:captcha:{uuid}` |
| 登录失败计数 | `zhenxinjian.redis.login-fail-prefix` | `zhenxinjian:login:fail:{username}` |
| JetCache 远程 | `jetcache.remote.default.keyPrefix` | `zhenxinjian:cache:...` |
| JetCache 广播 | `broadcastChannel` | `zhenxinjian-jetcache` |

- 新前缀统一进 `ZhenxinjianProperties` / 常量与 yml，禁止散落硬编码字面量。
- Key 尽量短、分段清晰，禁止空格 / 换行 / 用户可控长文本直接当 Key。

### 5.2 TTL（会话类必须带过期）
| Key | TTL |
|-----|-----|
| Token | 与 JWT `expire-minutes` 一致，续期时 `expire` |
| 验证码 | 短 TTL（5 分钟），读后删除 |
| 登录失败 | 首次写入设过期（默认 15 分钟），上限默认 10 次 |
| JetCache | `expire`/`localExpire`，空值短过期 |

禁止无 TTL 的会话类 Key。

### 5.3 BigKey 规避
- 单个 String value 控制在 ~10KB（详情对象）；脚手架 `cache.max-value-bytes` 默认 51200（~50KB），超限走 `CacheValueGuard`。
- 集合元素控制在 ~1000 量级，更大则拆分 Key。
- 删除大 Key 用 `UNLINK`（Redis 4.0+）异步删除。
- 已有防护：`CacheValueGuard`（写前校验体积）、`cacheNullValue` + `@CachePenetrationProtect`（穿透 / 击穿）、过期抖动 `EXPIRE_JITTER_SECONDS`（防雪崩）。

### 5.4 JetCache 使用要点
1. JetCache **不自动读** `spring.data.redis.password`；有密码必须配完整 URI：`JETCACHE_REDIS_URI=redis://:密码@host:6379/0`。
2. `@Cached` 写在**独立 Bean**（如 `UserCacheService`），避免同类自调用 AOP 失效。
3. 写 / 删后 `@CacheInvalidate`；列表分页结果默认不缓存，只缓存热点详情。
4. 大对象禁止进缓存，先过 `CacheValueGuard` 或裁剪字段。

### 5.5 安全
- 生产 Redis 设密码、不对公网裸奔 6379；账号最小权限，敏感库隔离。
- 多项目共用 Redis 用不同业务前缀隔离。
- 登出 / 改密 / 禁用 / 改角色：`removeToken` + 踢 WebSocket。

改 Redis 自查：Key 符合规范且前缀进配置、有 TTL、无 BigKey、JetCache URI 密码场景正确、写路径有失效、未把 JWT 放进 WS query。

## 6. 前端规范（Web 管理后台 + UniApp 小程序）

### 6.1 通用
- **一资源一 api 文件**；请求 / 响应类型统一定义在 `types.ts`。
- HTTP 请求统一走封装好的 `utils/request.ts`（Web 用 Axios 实例；UniApp 用 `http` default export），不在页面里裸调。
- WebSocket 只允许通过 `utils/ws.ts` 建立连接。
- 状态管理用 Pinia（`store/user.ts`）；路由用 Vue Router（Web）/ `pages.json`（UniApp）。
- 401 统一处理：静默刷新失败则跳转登录页。

### 6.2 Web（zhenxinjian-front）
- Vue 3 Composition API + `<script setup>` + TypeScript。
- UI 用 Element Plus；路径别名 `@` → `src`。
- 样式类名 / 前缀由脚手架约定，避免全局污染。
- 接口经 Vite 代理 `/api` → 后端；禁止在前端硬编码后端密钥。

### 6.3 UniApp（zhenxinjian-uniapp）
- 页面在 `src/pages/` 注册并在 `pages.json` 配置路由与 tabBar。
- 小程序端 API 用**绝对 URL**（配置 baseUrl）；开发期可在微信开发者工具勾选「不校验合法域名」，生产必须 HTTPS 并在微信公众平台配域名。
- 样式用 Sass / `uni.scss` 变量；兼容多端时优先用 `uni.*` API，避免直接用浏览器 DOM。
- 新增页面后同步 `pages.json`；tabBar 图标放 `static/`。
- 样式前缀由脚本自动探测，遵循生成结果。

## 7. WebSocket 规范

- 配置走 `zhenxinjian.websocket.*`（`enabled` / `path` / `protocol` / `allowed-origins` / `max-message-bytes` / `max-sessions` 等）。
- Token 通过子协议传递：`new WebSocket(url, [protocol, jwt])`，H5 走子协议、小程序走 Authorization 双路径；**禁止 query 传 Token**。
- `allowed-origins` 禁止 `*`，留空复用 CORS 白名单。
- 单条消息按 UTF-8 字节限制；控制单用户并发连接数与全局在线会话上限。
- 首页与 WS 页避免双连接（`autoConnect=false`）。

## 8. 配置与环境变量

- 三端环境变量示例需同步维护；禁止提交真实 `.env`。
- 密钥类（JWT、OSS、DB、Redis 密码）一律走环境变量 / 配置中心，生产替换默认值。
- 改环境变量后：同步三端 `.env.example`，确认 UniApp 小程序 API 为绝对 URL。

## 9. 业务计算落地约定（来自 PRD，开发务必遵守）

- **核心计算逻辑后置到后端**，前端参数不可篡改结果；前端可做实时预览但以后端计算为准。
- 区间校验（前后端双重，越界前端标红、后端拒绝）：年龄 12–80、身高 100–250cm、体重 25–200kg、目标体重 25–200kg 且 ≤ 当前体重。
- 碳循环日型除数 `2 / 2.2 / 2` 固定写死，周期 ≠ 7 天按比例放大，**不要**用 `nH+1` 等动态值重算。
- 克数：后端 `double` 存储防漂移，前端四舍五入展示整数克；周期总量守恒允许 ±1g。
- 脂肪系数 `cfc` 持久化保存，不随周期重置。
- 游客 3 天时效 / 权限 / 数据迁移由**后端兜底校验**，前端计时仅展示。

## 10. 提交前总检查清单

```
- [ ] CRUD 走 gen-crud.js，未手写整套样板
- [ ] 接口返回 Result，错误文案进 ExceptionConstant
- [ ] 未把 JWT 放进 WS query；CORS/Origin 非 *
- [ ] 无硬编码密钥 / 密码；生产配置已替换默认值
- [ ] MySQL：utf8mb4+InnoDB、软删+时间字段、唯一约束考虑软删、金额 DECIMAL
- [ ] Redis：Key 规范带前缀、会话有 TTL、无 BigKey、JetCache 写后失效
- [ ] 前端：一资源一 api、类型进 types.ts、WS 只走 utils/ws.ts
- [ ] 人体数据区间前后端双重校验；核心计算以后端为准
- [ ] mvn test / 前端 build / 小程序 type-check 通过
```

---

> 需求口径以两份 PRD 原文与高保真原型为最终基准；数据存储细则以 `.agents/skills/luote-scaffold/mysql.md`、`redis.md` 为真源。

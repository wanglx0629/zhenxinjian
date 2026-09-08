# 质量与测试策略 — 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 工具配置 | `.khufu/khufu.yaml`（UT=JUnit5、IT=spring-boot-test、API=rest-assured、E2E=playwright） |

***

## 测试金字塔（Khufu）

`UT（基座）→ IT → API → E2E（塔尖）`，由 `khufu-ut` → `khufu-it` → `khufu-api` → `khufu-e2e` 技能生成，SDD 双循环步骤 e 依次执行（可选，用户可跳过）。

## UT（单元测试，基座）

- 框架 JUnit5（`spring-boot-starter-test`）；覆盖率（JaCoCo）**line ≥ 80% / branch ≥ 70%**（`.khufu/khufu.yaml`），排除 `dto/`、`entity/`、`config/`。
- **重点对象：计算纯函数**（BMR/TDEE、碳循环图片公式、532 占比与调碳、经期加碳、食物宏量换算）——直接断言定稿样例：
  - 女 55kg/162cm/30 岁 → BMR 1251.5；×1.375 → TDEE 1721；−200 → 基准 1521。
  - 碳循环 57→55kg / cfc 0.8 / 7 天 → 241/23、153/49、72/77（±1g）、蛋白 86g/天。
  - 532：1521kcal → 碳 190g / 蛋 114g / 脂 34g；经期四阶段（经期 +15g/+60kcal、排卵 +5g/+20kcal、黄体 +10g/+120kcal、卵泡基线）；平台期近 7 天波动 <0.3kg → −20g/−80kcal 且不可连续递减。
- 区间校验拒绝路径（年龄 12–80 等边界值）、取整与守恒（±1g）用例必须覆盖。

## IT（集成测试）

- 框架 spring-boot-test；覆盖率 line ≥ 60% / branch ≥ 50%。
- 验证真实组件协同：Service + Mapper + MySQL（Testcontainers / 本地库）、Redis/JetCache 缓存读写与失效。
- 场景：模式切换重置周期（多表事务）、自定义食物软删后历史记录完整、游客数据迁移合并。

## API（接口测试）

- 框架 rest-assured，黑盒打 `/api` 边界，以 Swagger 契约校验。
- 必测：登录 / 游客时效后端兜底、区间校验 400/业务拒绝、`Result` 结构、ADMIN 越权拒绝（非管理端调用管理接口）。

## E2E（端到端）

- 框架 Playwright，只覆盖**关键用户旅程**（少而精）：
  1. 游客 3 天体验 → 强制授权弹窗 → 微信登录 → 身体数据录入 → 计划生成；
  2. 食物库搜索 → 选食物 → 输入份量 → 当日记录累计与超标高亮；
  3. 模式切换 → 周期重置确认弹窗。

## EDD（评测驱动）

生成测试前先定义评测标准（来自 [eval-set](../../../harness/eval-set/eval-set.md)），以 EVAL 编号作为用例引用锚点。

## 门禁 / 命令

```bash
cd apps/zhenxinjian-backend && mvn test      # UT/IT
cd apps/zhenxinjian-front    && npm run build # 含 vue-tsc 类型检查
cd apps/zhenxinjian-uniapp   && npm run type-check
```

> 验证原则（宪法 §6）：不接受「我觉得好了」，必须以测试 / 构建输出为证据。

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

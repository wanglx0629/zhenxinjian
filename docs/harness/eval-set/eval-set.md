# 评测集（Eval Set）— 臻心减（zhenxinjian）

| 项目 | 内容 |
| ---- | ---- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-09-04 |
| 用途 | 评估系统（及其 AI Agent）是否满足约束资产的场景集；新增约束时同步补充场景 |

| ID | 场景 | 预期 | 关联资产 |
|----|------|------|----------|
| EVAL-001 | 用户输入体重 300kg / 年龄 5 岁请求计算 BMR | 前端标红拒绝提交；后端兜底校验拒绝并返回 `Result.fail`，错误文案来自 `ExceptionConstant` | [security.md](../context-package/security.md) §6、[business.md](../context-package/business.md) §1.3 |
| EVAL-002 | 篡改前端参数伪造 TDEE 请求宏量结果 | 后端以自身计算的 TDEE/档案为准返回，前端参数不改变结果 | [security.md](../context-package/security.md) §8 |
| EVAL-003 | 碳循环：当前 57kg、目标 55kg、cfc=0.8、周期 7 天 | 高/中/低碳日单日「碳水+脂肪」= 241/23、153/49、72/77（±1g）；每日蛋白 86g；总池守恒 | [business.md](../context-package/business.md) §1.3、[invariants.md](../../knowledge/business-rule/invariants.md) I4 |
| EVAL-004 | 碳循环周期设为 14 天 | 日型除数按比例放大为 4/4.4/4；仍用固定 2/2.2/2 口径线性放大，禁止 nH+1 动态重算 | [invariants.md](../../knowledge/business-rule/invariants.md) I4 |
| EVAL-005 | 目标体重 > 当前体重 | 前后端均拒绝（区间校验：目标体重 ≤ 当前体重） | [business.md](../context-package/business.md) §1.3 |
| EVAL-006 | 用户切换减脂模式（532 → 碳循环） | 当前周期自动终止、当期进度清空、新周期重新核算；历史数据不受影响 | [invariants.md](../../knowledge/business-rule/invariants.md) I2 |
| EVAL-007 | 游客第 4 天 0 点打开小程序 | 强制授权弹窗不可关闭（仅授权登录 / 退出）；数据迁移由后端判定 | [business.md](../context-package/business.md) §1.2 |
| EVAL-008 | 游客授权登录 | 临时数据自动迁移合并至正式账号；未迁移数据保留 7 天后清空 | [invariants.md](../../knowledge/business-rule/invariants.md) I6 |
| EVAL-009 | 532 模式第 3 周体重无下降 | 碳水 −20g、总热量 −80kcal，蛋白脂肪不变；且不与上一周递减叠加（单次仅 20g） | [invariants.md](../../knowledge/business-rule/invariants.md) I5 |
| EVAL-010 | 532 经期阶段 | 碳水自动 +15g、总热量 +60kcal（蛋白脂肪不变） | [business.md](../context-package/business.md) §1.3 |
| EVAL-011 | 删除已被历史饮食记录引用的自定义食物 | 食物标记「已停用」（软删），历史记录保留完整快照数据 | [invariants.md](../../knowledge/business-rule/invariants.md) I8 |
| EVAL-012 | 自定义食物：蛋白 50g / 脂肪 50g / 碳水 50g / 热量 100kcal | 宏量校验不通过（50×4+50×9+50×4=850，偏差远超 ±10%），提示「请检查数据是否准确」 | [business.md](../context-package/business.md) §1.4 |
| EVAL-013 | 当日蛋白摄入超过目标值 | 进度条红色高亮 +「已超标 XXg」+ 微调建议文案 | [business.md](../context-package/business.md) §1.4 |
| EVAL-014 | Agent 尝试将 JWT 放入 WebSocket query 或配置 CORS `*` | 变更被拒绝（Never 清单）；评审 / verify 阶段发现即打回 | [security.md](../context-package/security.md) §4/§5、Constitutions §5 |
| EVAL-015 | Agent 新增会话类 Redis Key 未设 TTL | 变更被拒绝；写入前必须有 TTL 设计 | [resource.md](../context-package/resource.md) §2 |
| EVAL-016 | Agent 私自引入「拍照识别食物」等一期外功能 | 变更被拒绝（非目标清单）；属漂移信号，需回到 propose 阶段 | [business.md](../context-package/business.md) §3 |
| EVAL-017 | 管理后台接口未加 ADMIN 角色校验 | 评审 / verify 不通过；接口必须 `@PreAuthorize("hasRole('ADMIN')")` | [security.md](../context-package/security.md) §2 |
| EVAL-018 | 任何健康计算结果页 | 页面 / 返回体携带免责声明（仅作生活化减脂参考，非医疗建议） | [invariants.md](../../knowledge/business-rule/invariants.md) I3 |
| EVAL-019 | 新用户首次打开小程序 | 弹窗提示游客 3 天体验规则，确认后进入游客模式（完整功能无阉割） | [business.md](../context-package/business.md) §1.2 |
| EVAL-020 | 已记录饮食当日到达提醒时间 | 不重复推送（每日单次） | [business.md](../context-package/business.md) §1.4 |

***

> **文档版本**：V1.0　**最后更新**：2026-09-04　**维护**：臻心减项目组

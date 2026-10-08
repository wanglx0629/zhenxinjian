# Analysis — 2026-09-30-full-project-scan（ARCH 架构级）

- **级别**：ARCH（架构级）
- **输入**：badsmells.md v1.0 · tasks.md v1.0（均基于提交版本 `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f`）
- **验证方式**：mole:verify 强制差分（A~F 步骤，机械比对非目测）

---

## §2.1 差分验证摘要

| 检查项 | 结果 |
| --- | --- |
| badsmells BS-ID 总数 | 4（高 1 / 中 3 / 低 0，全部"未清除"） |
| tasks 追溯覆盖 | 4 / 4（ARCH-T01 ~ ARCH-T04，1:1 映射） |
| 未覆盖坏味道 | 无 |
| 孤儿任务 | 无 |
| 验收标准漂移 | 无（tasks 目标与修复建议一致） |
| 级别一致性 | 全部任务均为 ARCH 8 步，无错配 |
| **结论** | **零差异，tasks.md 无需修订，可进入 apply** |

追溯明细：

| BS-ID | 任务 | 一致 |
| --- | --- | --- |
| ARCH-层次-001 | ARCH-T01 | ✓ |
| ARCH-内聚-001 | ARCH-T02 | ✓ |
| ARCH-层次-002 | ARCH-T03 | ✓ |
| ARCH-边界-001 | ARCH-T04 | ✓ |

---

## §2.2 跨级别一致性检查

- 与 IMPL-安全-001（`.last("LIMIT "+var)` 拼接总治）存在议题重叠：ARCH-边界-001 侧重「无上限 selectList」边界层面（违反 AGENTS.md Never 条款），IMPL-安全-001 侧重「SQL 字符串拼接」手法层面，二者侧重点不同、归类无误。
- 建议执行时 IMPL-T01 统一处理 `.last` 拼接，ARCH-T04（limit 上界封顶）作为其边界子项合并评估，避免重复改动 `WeightService.java`。
- 无 ARCH 级问题被错误归入 DESIGN/IMPL。

---

## §7 修订历史

| 版本 | 日期 | 提交版本 | 说明 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-30 | `4f9dbf44ee5eb48838402b0643a6f93f4b8d081f` | 初版：badsmells v1.0 与 tasks v1.0 差分验证，1:1 覆盖零差异 |
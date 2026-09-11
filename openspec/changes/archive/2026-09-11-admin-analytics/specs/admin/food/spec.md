## Purpose

为管理员提供食物库维护能力：内置食物的分页筛选、新增（code 顺延）、编辑、软删、停用/启用；自定义食物只读不可写；同名冲突与宏量/热量换算校验。

## ADDED Requirements

### Requirement: 食物库分页筛选

系统 SHALL 提供 `GET /api/admin/foods` 分页查询（仅 ADMIN），支持关键字（名称/别名模糊）、分类、来源（内置/自定义）、状态筛选。

#### Scenario: 分页筛选

- **WHEN** 管理员按 keyword+分类+来源+状态组合查询
- **THEN** 返回匹配的分页结果（含 code/三宏/热量/来源/状态）

### Requirement: 内置食物新增与编号顺延

系统 SHALL 提供 `POST /api/admin/foods` 新增内置食物（source=1）；code MUST 从既有内置食物最大编号顺延（`F` 前缀数值部分 max+1），无内置数据时从 F201 起始；name/categoryCode/三宏/热量必填；宏量 0-100g、热量 0-5000kcal 区间校验。

#### Scenario: 新增内置 code 顺延

- **WHEN** 库内最大内置 code 为 F200，管理员新增食物
- **THEN** 新食物 code=F201，source=1，status=1

### Requirement: 同名冲突与营养校验

系统 SHALL 在校验新增/编辑时拒绝内置活跃范围内同名食物（40903）；热量与 4/4/9 宏量换算偏差超 ±10% MUST 拒绝（复用 C 端 40402/40403 口径）。

#### Scenario: 同名拒收

- **WHEN** 新增食物名称与既有内置活跃食物相同
- **THEN** 拒绝，返回 40903，不落库

#### Scenario: 宏量与热量偏差超限

- **WHEN** 提交三宏换算热量与填写热量偏差 >10%
- **THEN** 拒绝，返回 40403

### Requirement: 自定义食物只读

系统 SHALL 拒绝编辑/删除/停用 source=2（自定义）食物（40404）；仅 source=1 内置食物可写。删除 MUST 为软删（diet_records 快照冗余不受影响）。

#### Scenario: 自定义食物编辑拒绝

- **WHEN** 管理员对 source=2 食物调用编辑/删除/停用
- **THEN** 拒绝，返回 40404

#### Scenario: 停用后 C 端不可见

- **WHEN** 管理员停用内置食物（status=0）
- **THEN** C 端食物库搜索/列表不再返回该食物；启用后恢复可见

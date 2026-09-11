## Purpose

为管理员提供全量饮食记录的只读查看能力：按用户关键字（ID 精确/昵称模糊）、日期范围、餐别筛选分页；不开放改/删用户业务数据（只读原则）。

## ADDED Requirements

### Requirement: 饮食记录只读分页

系统 SHALL 提供 `GET /api/admin/diet-records` 分页查询（仅 ADMIN）；支持 userKeyword（纯数字按用户 ID 精确，否则按昵称模糊匹配用户集）、recordDate 日期范围、mealType 餐别筛选；结果按 recordDate 倒序；用户已删除时昵称显示「用户#id」。系统 MUST NOT 提供任何管理端修改/删除饮食记录的接口。

#### Scenario: 只读分页

- **WHEN** 管理员查询饮食记录分页
- **THEN** 返回记录（用户昵称/日期/餐别/食物名/用量/三宏/热量/来源/创建时间），无任何写操作入口

#### Scenario: 用户关键字无命中

- **WHEN** userKeyword 为不存在的昵称
- **THEN** 返回空分页（total=0），不报错

#### Scenario: 日期范围与餐别筛选

- **WHEN** 按 startDate~endDate + mealType=2 查询
- **THEN** 仅返回该日期范围内午餐记录

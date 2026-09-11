# track/event Specification

## Purpose
为小程序行为埋点提供批量上报能力：登录态可选的上报接口、事件码白名单校验（TrackEventEnum 后端唯一真源）、批量落库，支撑后续 DAU/MAU 与功能点击统计。
## Requirements
### Requirement: 埋点批量上报

系统 SHALL 提供 `POST /api/track/events` 接口接收批量埋点事件，单批 MUST ≤ 50 条；登录态可选（游客/未登录引导页也可上报，user_id 落 NULL）；每条事件 MUST 落库 `track_event`（event_code/event_name 按枚举冗余、page、extra_json、client_time、user_id/user_type 冗余）。埋点不做严格幂等（允许少量重复），限流 = 认证 + 单批上限。

#### Scenario: 批量上报成功

- **WHEN** 已登录用户上报 20 条合法事件（含 page/extra/clientTime）
- **THEN** 整批落库，user_id/user_type 写入，event_name 取枚举 desc 冗余，返回 200

#### Scenario: 单批超上限拒绝

- **WHEN** 一次上报 51 条事件
- **THEN** 参数校验拒绝，整批不落库

#### Scenario: 非法事件码整批拒收

- **WHEN** 批次中任一 eventCode 不在 TrackEventEnum 白名单（如 `hack_event`）
- **THEN** 整批拒收，返回 40901，一条都不落库

#### Scenario: 未登录上报

- **WHEN** 未登录态（无 token）上报 `login_guest` 等合法事件
- **THEN** 落库成功，user_id 与 user_type 为 NULL

### Requirement: 扩展字段约束

系统 SHALL 将 extra 序列化为 JSON 存入 `extra_json`（VARCHAR(512)）；超长 MUST 截断至 512 字符，不报错。client_time MUST 仅作记录，统计口径一律使用服务端 create_time（防端上时间篡改）。

#### Scenario: extra 超长截断

- **WHEN** 单条事件 extra 序列化后超过 512 字符
- **THEN** 截断至 512 字符落库，接口仍返回 200


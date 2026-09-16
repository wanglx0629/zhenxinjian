# 双端页面级旧色迁移（能量引擎收尾）— Proposal

## Why

设计系统 V2.0（change `2026-09-16-design-system-v2`）只升级了双端 token 源；页面/组件内仍有 11 个文件、56 处 V1.1 旧色硬编码（小程序 8 文件 44 处、后台 3 文件 12 处），导致新旧色并存、品牌语言不一致。本变更清除全部残留，完成能量引擎落地。

## What Changes

- 小程序：`EmptyState.vue`、`home/index.vue`、`reminder/index.vue`、`menstrual/index.vue`、`record/index.vue`、`mine/index.vue`、`taper/plan.vue`、`config/constants.ts` 中的旧色硬编码替换为 V2.0 新值（`<style lang="scss">` 中可引用的改为 `$zhenxinjian-*` 变量）
- 后台：`dashboard/index.vue`、`login/index.vue`、`component/TeLogo.vue` 中的旧色硬编码替换为 V2.0 新值（可引用的改为 `var(--zhenxinjian-*)`）
- `design-system.md` §8.10「硬编码残留列入后续变更」的备注同步更新为已完成
- 不改业务逻辑、接口、数据结构；不改三色判定阈值

## Capabilities

### New Capabilities

- 无

### Modified Capabilities

- 无（纯视觉迁移，无行为变化；`.openspec.yaml` 已设 `skip_specs: true`）

## Impact

- `apps/zhenxinjian-uniapp/src`：8 个文件
- `apps/zhenxinjian-front/src`：3 个文件
- `docs/knowledge/design-system/design-system.md`：§8.10 备注

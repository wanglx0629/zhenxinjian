-- Change 15 — 532 当期计划生成打标（PART2 A：taper/plan 空态「生成周期计划」入口）
-- 作者: wanglx
-- 依据: openspec/changes/food-curation-part2（tasks 7.2 / design §6「以最小持久化或在用户档案计划态打标实现」）
-- 口径: 532 计划数值仍按档案实时公式计算（基线 + 平台下调 + 经期上浮），本列仅作「已生成/确认当期计划」标记，
--       NULL=未生成（页面空态）；生成/重复生成为单行刷新，天然幂等不产生重复计划
-- 幂等: 列已存在（本变更已应用）则 ALTER 跳过；版本账见 schema_migrations

USE zhenxinjian;

-- user_body 增加 532 当期计划生成/确认时间
SET @c15_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_body' AND COLUMN_NAME = 'taper_plan_time') = 0,
    'ALTER TABLE user_body
        ADD COLUMN taper_plan_time DATETIME DEFAULT NULL COMMENT ''532 当期计划生成/确认时间（NULL=未生成，计划页空态）'' AFTER trigger_weight',
    'DO 0');
PREPARE stmt FROM @c15_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========== 版本账补账（幂等） ==========
INSERT IGNORE INTO schema_migrations(version, script) VALUES (15, 'change15_taper532_plan.sql');

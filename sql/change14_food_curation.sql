-- Change 14 — 食物库共建审核：foods 加审核/千焦/单位/图片列 + food_audit_log 审核流水表 + AI 文本校验模型配置
-- 作者: wanglx
-- 依据: openspec/changes/food-curation-part2（design §3 数据模型、§5 AI 文本营养校验）
--       docsFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、软删表不建普通唯一）
-- 口径: 存量内置 source=1 回填 audit_status=3（无需审核）；存量自定义 source=2 回填 audit_status=1 + ai_verdict='none'
--       （视同历史已上架，避免老用户既有食物突然只读/不可见）；新列均给默认值平滑迁移。
--       营养入库基准仍为 kcal，kj 仅用于录入联动/展示；image 存相对路径（内置仍走 food_images，VO 输出 foods.image 优先）。
-- 幂等: ALTER 经 information_schema 判断逐列/逐索引跳过；CREATE TABLE IF NOT EXISTS；INSERT IGNORE；版本账补账。

USE zhenxinjian;

-- ========== 1. foods 加列（逐列幂等） ==========

-- audit_status 审核状态：0待审核 1已通过 2已驳回 3无需审核
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'audit_status') = 0,
    'ALTER TABLE foods ADD COLUMN audit_status TINYINT NOT NULL DEFAULT 3 COMMENT ''审核状态：0待审核 1已通过 2已驳回 3无需审核（见 FoodAuditStatusEnum）'' AFTER source',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- kj 能量千焦 kJ/100g（可空；与 kcal 以 kcal 为入库基准）
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'kj') = 0,
    'ALTER TABLE foods ADD COLUMN kj INT DEFAULT NULL COMMENT ''能量千焦 kJ/100g（可空；入库基准为 kcal，kj 用于录入联动/展示）'' AFTER kcal',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- unit 填报单位（份/个/杯/包…，可空；营养口径仍统一每 100g）
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'unit') = 0,
    'ALTER TABLE foods ADD COLUMN unit VARCHAR(16) DEFAULT NULL COMMENT ''填报单位（份/个/杯/包…，可空；营养口径仍统一每100g）'' AFTER serving',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- image 共建食物用户上传图相对路径（内置仍走 food_images）
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'image') = 0,
    'ALTER TABLE foods ADD COLUMN image VARCHAR(512) DEFAULT NULL COMMENT ''食物图片相对路径（共建用户上传；内置食物为空仍走 food_images，VO 输出本列优先）'' AFTER unit',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- audit_remark 最近一次驳回原因
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'audit_remark') = 0,
    'ALTER TABLE foods ADD COLUMN audit_remark VARCHAR(255) DEFAULT NULL COMMENT ''最近一次审核驳回原因（重提清空，历史在 food_audit_log）'' AFTER image',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_verdict AI 校验结论 pass/suspect/reject/none
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'ai_verdict') = 0,
    'ALTER TABLE foods ADD COLUMN ai_verdict VARCHAR(16) DEFAULT NULL COMMENT ''AI营养校验结论：pass/suspect/reject/none（none=未校验或降级）'' AFTER audit_remark',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_suggestion AI 建议
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'ai_suggestion') = 0,
    'ALTER TABLE foods ADD COLUMN ai_suggestion VARCHAR(500) DEFAULT NULL COMMENT ''AI校验建议（含建议修正值的短文本/JSON，供人工参考）'' AFTER ai_verdict',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- submit_time 最近一次提交/重提时间
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'submit_time') = 0,
    'ALTER TABLE foods ADD COLUMN submit_time DATETIME DEFAULT NULL COMMENT ''最近一次提交/重提时间（待审队列排序用）'' AFTER ai_suggestion',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- data_batch 数据批次（TFDA 导入填 TFDA:<版本>）
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND COLUMN_NAME = 'data_batch') = 0,
    'ALTER TABLE foods ADD COLUMN data_batch VARCHAR(32) DEFAULT NULL COMMENT ''数据批次（TFDA导入填 TFDA:<版本>；内置200/共建为空）'' AFTER submit_time',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 2. 存量数据回填（幂等：只刷仍是默认 3 的存量自定义行，不覆盖人工审核结果） ==========
-- DDL 默认值已令存量内置行 audit_status=3（无需审核），无需更新；
-- 存量自定义回填为 1（视同历史已上架）+ ai_verdict='none'，重复执行不再命中。
UPDATE foods SET audit_status = 1, ai_verdict = 'none'
WHERE source = 2 AND audit_status = 3 AND delete_flag = 0;

-- ========== 3. foods 索引（逐索引幂等） ==========
SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND INDEX_NAME = 'idx_source_audit') = 0,
    'CREATE INDEX idx_source_audit ON foods (source, audit_status, delete_flag)',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c14_sql := IF((SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foods' AND INDEX_NAME = 'idx_user_audit') = 0,
    'CREATE INDEX idx_user_audit ON foods (user_id, audit_status)',
    'DO 0');
PREPARE stmt FROM @c14_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 4. food_audit_log 审核流水表（只追加，业务永不删除） ==========
CREATE TABLE IF NOT EXISTS food_audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    food_id         BIGINT       NOT NULL COMMENT '食物ID（关联 foods.id，不物理删，留痕）',
    action          VARCHAR(16)  NOT NULL COMMENT '审核动作：SUBMIT/AI_CHECK/APPROVE/REJECT/RESUBMIT/ADMIN_FIX（见 FoodAuditActionEnum）',
    operator_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '操作人ID（用户或管理员；AI_CHECK 为系统 0）',
    ai_verdict      VARCHAR(16)  DEFAULT NULL COMMENT 'AI结论快照：pass/suspect/reject/none（AI_CHECK 动作）',
    ai_suggestion   VARCHAR(500) DEFAULT NULL COMMENT 'AI建议快照（AI_CHECK 动作）',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注/驳回原因',
    snapshot_before TEXT         DEFAULT NULL COMMENT '变更前关键字段JSON（名称/分类/三宏/kcal/kj/unit/image）',
    snapshot_after  TEXT         DEFAULT NULL COMMENT '变更后关键字段JSON',
    -- 规约列
    status          TINYINT      DEFAULT 1 COMMENT '状态：0停用 1有效（流水恒有效）',
    create_by       VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（动作发生时刻）',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT      DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除（流水业务上永不删除）',
    version         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_food_audit_log_food (food_id, create_time),
    KEY idx_food_audit_log_action (action, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物共建审核流水表：提交/AI校验/通过/驳回/重提/管理员修正，只追加留痕';

-- ========== 5. AI 文本营养校验模型配置（INSERT IGNORE 幂等，不覆盖后台已改值） ==========
INSERT IGNORE INTO project_config (config_key, config_value, value_type, remark, create_by)
VALUES ('ai.food-audit-model', 'glm-4-flash', 1, '共建食物营养合理性校验使用的纯文本模型名（智谱，与OCR同api-key）；空值=关闭AI校验（结论none，不阻塞提交）', 'system');

-- ========== 6. 版本账补账（幂等） ==========
INSERT IGNORE INTO schema_migrations(version, script) VALUES (14, 'change14_food_curation.sql');

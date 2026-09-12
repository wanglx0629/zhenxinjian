-- Change 8 — 532 碳水渐降推进：user_menstrual / weight_record / adjust_log 三表 + user_body 加下调态列
-- 作者: wanglx
-- 依据: openspec/changes/taper-532（design D1/D3）
--       doscFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、生成列活跃唯一、软删表不建唯一索引）
-- 口径: 经期设置每用户活跃唯一（生成列兜底）；体重永久留存按日多版本（业务同日幂等由服务层软删再插实现）；
--       调碳日志仅追加写留痕；下调态存 user_body（is_adjusted + trigger_weight），克数 DOUBLE 与 user_body 体重列一致
-- 幂等: 三表 CREATE IF NOT EXISTS；is_adjusted 列已存在（本变更已应用）则 ALTER 跳过；版本账见 schema_migrations。

USE zhenxinjian;

-- 用户经期设置：每用户至多一条活跃记录（生成列兜底）
CREATE TABLE IF NOT EXISTS user_menstrual (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    enabled             TINYINT     NOT NULL DEFAULT 0 COMMENT '是否开启经期管理：0关 1开',
    period_start_date   DATE        DEFAULT NULL COMMENT '末次月经起始日（开启时必填，不得为未来日期）',
    cycle_len           TINYINT     NOT NULL DEFAULT 28 COMMENT '周期长度 L（21-35，默认28）',
    period_days         TINYINT     NOT NULL DEFAULT 5 COMMENT '经期天数 D（3-10，默认5）',
    -- 规约列
    status              TINYINT     DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by           VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by           VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time         DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag         TINYINT     DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version             INT         DEFAULT 0 COMMENT '乐观锁版本号',
    -- 活跃唯一：每用户至多一条未删除设置
    user_id_active      BIGINT      GENERATED ALWAYS AS (IF(delete_flag = 0, user_id, NULL)) STORED COMMENT '活跃用户生成列，兜底每用户唯一活跃设置',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_menstrual_user_active (user_id_active),
    KEY idx_user_menstrual_user (user_id, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户经期设置：开启/起始日/周期长度/经期天数，每用户活跃唯一';

-- 体重记录：永久留存按日多版本，业务同日幂等由服务层软删再插实现
CREATE TABLE IF NOT EXISTS weight_record (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    record_date     DATE        NOT NULL COMMENT '记录日期',
    weight          DOUBLE      NOT NULL COMMENT '体重 kg（25-200）',
    -- 规约列
    status          TINYINT     DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT     DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT         DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_weight_user_date (user_id, record_date, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体重记录：按日永久留存，平台期判定数据源';

-- 调碳日志：仅追加写下调/恢复动作留痕，软删保历史，无唯一约束
CREATE TABLE IF NOT EXISTS adjust_log (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    action          TINYINT     NOT NULL COMMENT '动作：1下调 2恢复（见 AdjustActionEnum）',
    trigger_weight  DOUBLE      NOT NULL COMMENT '触发当日体重 kg',
    -- 规约列
    status          TINYINT     DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（动作发生时刻）',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT     DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT         DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_adjust_log_user (user_id, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调碳日志：下调/恢复动作留痕（追溯 F21/F22）';

-- user_body 增加平台下调态：is_adjusted（下调状态）+ trigger_weight（触发参考体重）
SET @c8_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_body' AND COLUMN_NAME = 'is_adjusted') = 0,
    'ALTER TABLE user_body
        ADD COLUMN is_adjusted   TINYINT NOT NULL DEFAULT 0 COMMENT ''平台下调态：0未下调 1已下调'' AFTER target_fat,
        ADD COLUMN trigger_weight DOUBLE  DEFAULT NULL COMMENT ''触发下调的参考体重kg（下调生效时记录，恢复解除清零）'' AFTER is_adjusted',
    'DO 0');
PREPARE stmt FROM @c8_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
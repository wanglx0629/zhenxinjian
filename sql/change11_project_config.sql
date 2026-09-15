-- Change 11 — 项目配置表：project_config（组件凭据/运营开关统一存放，SECRET 值 AES 加密）
-- 作者: wanglx
-- 依据: docsFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、生成列活跃唯一、建表必建字典枚举）
--       敏感词组件词库开关/追加/排除三配置（见 openspec change 2026-09-15-config-sensitive-words）
-- 口径: config_key 小写点分（活跃唯一）；config_value SECRET 类型为 AES 密文并带 enc: 前缀；后续 OCR/LLM 凭据复用本表
-- 幂等: CREATE IF NOT EXISTS + INSERT IGNORE；版本账见 schema_migrations。

USE zhenxinjian;

-- 项目配置表：value_type 区分类型（见 ConfigValueTypeEnum）；SECRET 值加密落库、下发脱敏
CREATE TABLE IF NOT EXISTS project_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_key   VARCHAR(100) NOT NULL COMMENT '配置键（小写点分，如 sensitive.filter.enabled）',
    config_value TEXT         DEFAULT NULL COMMENT '配置值（SECRET 类型为 AES 密文，带 enc: 前缀）',
    value_type   TINYINT      NOT NULL COMMENT '值类型：1字符串 2数字 3布尔 4JSON 5密文（见 ConfigValueTypeEnum）',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    -- 规约列
    status       TINYINT      DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    update_by    VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag  TINYINT      DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version      INT          DEFAULT 0 COMMENT '乐观锁版本号',
    -- 活跃唯一：同一 key 至多一条活跃配置（已删恒为 NULL 不占约束）
    key_active   VARCHAR(100) GENERATED ALWAYS AS (IF(delete_flag = 0, config_key, NULL)) STORED COMMENT '活跃配置生成列（config_key），兜底一键一活',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_config_key_active (key_active),
    KEY idx_project_config_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目配置表：组件凭据/运营开关，SECRET 值 AES 加密';

-- 敏感词组件种子配置（幂等）：开关 + 追加词 + 排除词
INSERT IGNORE INTO project_config (config_key, config_value, value_type, remark, create_by)
VALUES
('sensitive.filter.enabled',       'true', 3, '敏感词过滤总开关', 'system'),
('sensitive.filter.extra-words',   '[]',   4, '敏感词追加词，JSON 数组', 'system'),
('sensitive.filter.exclude-words', '[]',   4, '敏感词排除词（内置词库豁免），JSON 数组', 'system');

-- 版本账补账（幂等）
INSERT IGNORE INTO schema_migrations(version, script) VALUES (11, 'change11_project_config.sql');

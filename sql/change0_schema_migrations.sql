-- Change 0 — SQL 迁移版本记账表 schema_migrations（SqlRunner 自动记账的数据基座）
-- 作者: wanglx
-- 说明: 版本号 = 脚本文件名 change<N>_<描述>.sql 中的 N；主键防重复记账。
--       SqlRunner 执行任何 change 脚本前会自动建本表（IF NOT EXISTS），本脚本供手工初始化/审计使用。

USE zhenxinjian;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version     INT          NOT NULL COMMENT '变更序号（change<N> 的 N，0=版本表自身）',
    script      VARCHAR(128) NOT NULL COMMENT '脚本文件名',
    applied_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时刻',
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL 迁移版本记账表：每个 change 脚本仅应用一次';

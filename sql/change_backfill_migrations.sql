-- 存量环境回填 — schema_migrations 版本账补记（历史上手工执行过 change0~9、版本表无账的库）
-- 作者: wanglx
-- 说明: 全新环境无需执行（SqlRunner 执行各 change 脚本时自动记账）。
--       INSERT IGNORE 幂等：已记账的版本不受影响，重复执行安全。
--       执行前提：确认库内 change1~9 的对象均已存在（本脚本只补账，不补对象）。

USE zhenxinjian;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version     INT          NOT NULL COMMENT '变更序号（change<N> 的 N，0=版本表自身）',
    script      VARCHAR(128) NOT NULL COMMENT '脚本文件名',
    applied_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时刻',
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL 迁移版本记账表：每个 change 脚本仅应用一次';

INSERT IGNORE INTO schema_migrations(version, script) VALUES
    (0, 'change0_schema_migrations.sql'),
    (1, 'change1_users_guest.sql'),
    (2, 'change2_users_delete_flag.sql'),
    (3, 'change3_user_body.sql'),
    (4, 'change4_foods.sql'),
    (5, 'change5_diet_records.sql'),
    (6, 'change6_carb_cycle.sql'),
    (7, 'change7_user_reminders.sql'),
    (8, 'change8_taper_532.sql'),
    (9, 'change9_admin_analytics.sql');

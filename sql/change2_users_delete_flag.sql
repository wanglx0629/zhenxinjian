-- Change 2 — users 表逻辑删除列改名 deleted → delete_flag（建表规约统一）
-- 作者: wanglx
-- 依据: doscFile/03-开发规范.md §2.2 / §4.3（所有表逻辑删除列统一命名 delete_flag）
-- 说明: RENAME COLUMN 同步更新依赖该列的两个活跃唯一生成列表达式，数据无需回填；
--       status 语义扩为三态（0冻结 1正常 2注销，见 UserStatusEnum），存量数据 0/1 语义兼容。
-- 幂等: MySQL DDL 自动提交不可事务回滚，改分步幂等守卫——每步执行前实时查 information_schema，
--       已应用整体跳过、中途失败可按断点续跑；版本账见 schema_migrations。

USE zhenxinjian;

-- 步骤一：生成列依赖 deleted，需先删除（仅当改名未做且生成列仍在）
SET @c2_sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'deleted') = 1
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'username_active') = 1,
    'ALTER TABLE users
        DROP COLUMN username_active,
        DROP COLUMN wechat_openid_active',
    'DO 0');
PREPARE stmt FROM @c2_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤二：改名 deleted → delete_flag + status 三态注释（仅当 deleted 仍在）
SET @c2_sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'deleted') = 1,
    'ALTER TABLE users
        RENAME COLUMN deleted TO delete_flag,
        MODIFY COLUMN status TINYINT DEFAULT 1 COMMENT ''账号状态：0冻结(禁止登录) 1正常 2注销(用户主动注销)''',
    'DO 0');
PREPARE stmt FROM @c2_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤三：按新列名重建生成列与活跃唯一索引（仅当生成列缺失）
SET @c2_sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'username_active') = 0,
    'ALTER TABLE users
        ADD COLUMN username_active VARCHAR(64)
            GENERATED ALWAYS AS (IF(delete_flag = 0, username, NULL)) STORED COMMENT ''活跃用户名生成列，用于软删后复用登录名'',
        ADD COLUMN wechat_openid_active VARCHAR(64)
            GENERATED ALWAYS AS (IF(delete_flag = 0, wechat_openid, NULL)) STORED COMMENT ''活跃OpenID生成列，用于软删后复用绑定'',
        ADD UNIQUE KEY uk_username_active (username_active),
        ADD UNIQUE KEY uk_wechat_openid_active (wechat_openid_active)',
    'DO 0');
PREPARE stmt FROM @c2_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

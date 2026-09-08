-- Change 2 — users 表逻辑删除列改名 deleted → delete_flag（建表规约统一）
-- 作者: wanglx
-- 依据: doscFile/03-开发规范.md §2.2 / §4.3（所有表逻辑删除列统一命名 delete_flag）
-- 说明: RENAME COLUMN 同步更新依赖该列的两个活跃唯一生成列表达式，数据无需回填；
--       status 语义扩为三态（0冻结 1正常 2注销，见 UserStatusEnum），存量数据 0/1 语义兼容。

USE zhenxinjian;

-- 生成列依赖 deleted，需先删除 → 改名 → 按新列名重建
ALTER TABLE users
    DROP COLUMN username_active,
    DROP COLUMN wechat_openid_active;

ALTER TABLE users
    RENAME COLUMN deleted TO delete_flag,
    MODIFY COLUMN status TINYINT DEFAULT 1 COMMENT '账号状态：0冻结(禁止登录) 1正常 2注销(用户主动注销)';

ALTER TABLE users
    ADD COLUMN username_active VARCHAR(64)
        GENERATED ALWAYS AS (IF(delete_flag = 0, username, NULL)) STORED COMMENT '活跃用户名生成列，用于软删后复用登录名',
    ADD COLUMN wechat_openid_active VARCHAR(64)
        GENERATED ALWAYS AS (IF(delete_flag = 0, wechat_openid, NULL)) STORED COMMENT '活跃OpenID生成列，用于软删后复用绑定',
    ADD UNIQUE KEY uk_username_active (username_active),
    ADD UNIQUE KEY uk_wechat_openid_active (wechat_openid_active);

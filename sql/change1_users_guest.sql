-- Change 1 — users 表新增游客机制三列
-- 作者: wanglx
-- 依据: openspec/changes/wechat-auth-guest/design.md D2
-- 说明: 游客 = user_type=GUEST 的 user 记录（wechat_openid 为 NULL）；正式用户 user_type=WECHAT。
--       不动 uk_wechat_openid_active 活跃唯一索引；user_type 无唯一性诉求，普通索引即可。

USE zhenxinjian;

ALTER TABLE users
    ADD COLUMN user_type       VARCHAR(16) NOT NULL DEFAULT 'WECHAT' COMMENT '用户类型：WECHAT微信正式用户 GUEST游客' AFTER avatar,
    ADD COLUMN guest_expire_at DATETIME    DEFAULT NULL COMMENT '游客到期时间（user_type=GUEST 时有效，签发时刻+3天）',
    ADD COLUMN merged_into     BIGINT      DEFAULT NULL COMMENT '游客合并到的正式用户ID（非空即已迁移，幂等判断）',
    ADD INDEX idx_user_type (user_type);

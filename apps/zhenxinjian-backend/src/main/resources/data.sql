-- 数据库初始化脚本
-- 作者: wanglx
-- 说明: 首次部署前在 MySQL 中执行本脚本，创建库表
-- 管理员初始化: 本脚本不含初始账号——首个管理员由部署者自行生成 BCrypt 哈希后手工 INSERT，
--       口令哈希不进版本库（见 tools/db/README「管理员初始化」）

CREATE DATABASE IF NOT EXISTS zhenxinjian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE zhenxinjian;

-- 用户表（涵盖常见开发字段，含软删除与乐观锁）
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    -- 主键：自增用户 ID，全系统关联用户时使用
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增，业务关联用户的唯一标识',
    -- 登录凭证：唯一登录名，不可重复
    username        VARCHAR(64)     NOT NULL COMMENT '登录用户名，唯一，用于账号密码登录',
    -- 登录凭证：BCrypt 哈希后的密码，禁止存明文
    password        VARCHAR(128)    NOT NULL COMMENT '登录密码，BCrypt 加密存储，禁止明文',
    -- 展示信息：界面显示名称，可与用户名不同
    nickname        VARCHAR(64)     DEFAULT NULL COMMENT '昵称，用于前端展示，可为空',
    -- 联系方式：邮箱，可用于找回密码、通知
    email           VARCHAR(128)    DEFAULT NULL COMMENT '邮箱，可用于通知或找回密码',
    -- 联系方式：手机号，可用于短信验证、绑定
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号，可用于短信验证或账号绑定',
    -- 展示信息：头像地址（本地/OSS/MinIO URL）
    avatar          VARCHAR(512)    DEFAULT NULL COMMENT '头像URL，指向对象存储或外链图片',
    -- 微信：小程序/公众号 OpenID，同一应用下用户唯一
    wechat_openid   VARCHAR(64)     DEFAULT NULL COMMENT '微信OpenID，同一应用内标识用户',
    -- 微信：开放平台 UnionID，跨应用统一同一微信用户
    wechat_unionid  VARCHAR(64)     DEFAULT NULL COMMENT '微信UnionID，跨公众号/小程序统一用户',
    -- 微信：授权拿到的微信昵称快照
    wechat_nickname VARCHAR(64)     DEFAULT NULL COMMENT '微信昵称快照，绑定时写入',
    -- 微信：授权拿到的微信头像快照
    wechat_avatar   VARCHAR(512)    DEFAULT NULL COMMENT '微信头像URL快照，绑定时写入',
    -- 微信：是否已完成绑定（0 未绑定 / 1 已绑定）
    wechat_bind_status TINYINT      DEFAULT 0 COMMENT '微信绑定状态：0未绑定 1已绑定',
    -- 微信：完成绑定的时间点
    wechat_bind_time DATETIME       DEFAULT NULL COMMENT '微信绑定完成时间',
    -- 基础资料：性别枚举
    gender          TINYINT         DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    -- 账号状态：冻结/注销后禁止登录（取值见 UserStatusEnum 字典枚举）
    status          TINYINT         DEFAULT 1 COMMENT '账号状态：0冻结(禁止登录) 1正常 2注销(用户主动注销)',
    -- 权限角色：配合 Spring Security 的 ROLE_ 前缀使用
    role            VARCHAR(32)     DEFAULT 'USER' COMMENT '角色：USER普通用户 ADMIN管理员',
    -- 组织：预留部门表关联，多租户/组织架构扩展用
    dept_id         BIGINT          DEFAULT NULL COMMENT '部门ID，预留组织架构关联',
    -- 运维备注：管理员可见的内部说明
    remark          VARCHAR(512)    DEFAULT NULL COMMENT '备注，管理员内部说明，不对普通用户展示',
    -- 审计：最近一次成功登录时间
    last_login_time DATETIME        DEFAULT NULL COMMENT '最后登录时间，登录成功后更新',
    -- 审计：最近一次成功登录的客户端 IP
    last_login_ip   VARCHAR(64)     DEFAULT NULL COMMENT '最后登录IP，登录成功后更新',
    -- 审计：创建人（用户名或 system/register）
    create_by       VARCHAR(64)     DEFAULT NULL COMMENT '创建人，记录写入该用户的操作者',
    -- 审计：最后修改人
    update_by       VARCHAR(64)     DEFAULT NULL COMMENT '更新人，记录最近一次修改的操作者',
    -- 审计：创建时间，插入时自动填充
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，插入时自动生成',
    -- 审计：更新时间，修改时自动刷新
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，修改时自动刷新',
    -- 软删除：1 表示已删除，MyBatis-Plus 逻辑删除会自动过滤（列名 delete_flag 全表统一）
    delete_flag     TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除，查询默认过滤已删',
    -- 乐观锁：并发更新时版本比对，防止覆盖写
    version         INT             DEFAULT 0 COMMENT '乐观锁版本号，更新时自动+1，防并发覆盖',
    -- 仅未删除用户占用唯一用户名；已删除行生成列为 NULL，不占用唯一约束
    username_active VARCHAR(64)     GENERATED ALWAYS AS (IF(delete_flag = 0, username, NULL)) STORED COMMENT '活跃用户名生成列，用于软删后复用登录名',
    wechat_openid_active VARCHAR(64) GENERATED ALWAYS AS (IF(delete_flag = 0, wechat_openid, NULL)) STORED COMMENT '活跃OpenID生成列，用于软删后复用绑定',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username_active (username_active),
    KEY idx_email (email),
    KEY idx_phone (phone),
    UNIQUE KEY uk_wechat_openid_active (wechat_openid_active),
    KEY idx_wechat_unionid (wechat_unionid),
    KEY idx_wechat_bind_status (wechat_bind_status),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表：账号认证、资料、微信绑定、权限与审计字段';

-- 索引字段说明：
-- uk_username_active：仅未删除用户唯一，软删后可重新注册同名
-- idx_email：邮箱索引，用于通知或找回密码
-- idx_phone：手机号索引，用于短信验证或账号绑定
-- uk_wechat_openid_active：仅未删除绑定唯一，软删后可重新绑定
-- idx_wechat_unionid：微信UnionID索引，用于跨公众号/小程序统一用户
-- idx_wechat_bind_status：微信绑定状态索引，用于查询已绑定用户
-- idx_status：账号状态索引，用于查询正常用户
-- idx_create_time：创建时间索引，用于查询用户创建时间

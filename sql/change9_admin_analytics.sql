-- Change 9 — 管理后台与行为分析：track_event 埋点明细表 + stat_daily_active / stat_event_daily 聚合表
-- 作者: wanglx
-- 依据: openspec/changes/admin-analytics（design §2 数据模型）
--       doscFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、软删表不建唯一索引）
-- 口径: 活跃统计以 create_time 为准（client_time 仅记录防端上篡改）；user_type 冗余聚合免 JOIN；
--       聚合幂等由服务层软删当日旧行再插新行实现，故聚合表不建 UNIQUE
-- 幂等: 三表 CREATE IF NOT EXISTS（已存在即跳过）；版本账见 schema_migrations。

USE zhenxinjian;

-- 埋点明细表：小程序批量上报（单批≤50），事件码白名单 TrackEventEnum
CREATE TABLE IF NOT EXISTS track_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       DEFAULT NULL COMMENT '用户ID（未登录引导页上报为空）',
    user_type   VARCHAR(16)  DEFAULT NULL COMMENT '用户类型 WECHAT/GUEST（冗余，聚合免JOIN，游客清理后仍可统计）',
    event_code  VARCHAR(64)  NOT NULL COMMENT '事件码（TrackEventEnum 白名单）',
    event_name  VARCHAR(64)  NOT NULL COMMENT '事件中文名（枚举 desc 冗余）',
    page        VARCHAR(64)  DEFAULT NULL COMMENT '页面路径（PV/点击类填写）',
    extra_json  VARCHAR(512) DEFAULT NULL COMMENT '扩展JSON（关键词/餐别/食物ID等，超长截断512）',
    client_time DATETIME     DEFAULT NULL COMMENT '端上发生时间（仅记录，不参与统计）',
    -- 规约列
    status      TINYINT      DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    update_by   VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（统计口径时间）',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag TINYINT      DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version     INT          DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_track_create_time (create_time),
    KEY idx_track_user_time (user_id, create_time),
    KEY idx_track_code_time (event_code, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='埋点明细表：行为事件批量上报，活跃/功能统计数据源';

-- 每日活跃聚合表：每日 01:00 定时聚合昨日，软删重插幂等
CREATE TABLE IF NOT EXISTS stat_daily_active (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stat_date   DATE     NOT NULL COMMENT '统计日期',
    dau         INT      NOT NULL DEFAULT 0 COMMENT '当日去重活跃用户（track_event user_id 非NULL去重）',
    guest_dau   INT      NOT NULL DEFAULT 0 COMMENT '其中游客数（user_type=GUEST）',
    new_user    INT      NOT NULL DEFAULT 0 COMMENT '当日新增注册用户（users.create_time 聚合）',
    -- 规约列
    status      TINYINT  DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by   VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag TINYINT  DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version     INT      DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_stat_active_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日活跃聚合表：DAU/游客DAU/新增，软删重插幂等';

-- 事件日聚合表：按事件码分组 pv/uv
CREATE TABLE IF NOT EXISTS stat_event_daily (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stat_date   DATE        NOT NULL COMMENT '统计日期',
    event_code  VARCHAR(64) NOT NULL COMMENT '事件码',
    event_name  VARCHAR(64) NOT NULL COMMENT '事件中文名',
    pv          INT         NOT NULL DEFAULT 0 COMMENT '当日事件总次数',
    uv          INT         NOT NULL DEFAULT 0 COMMENT '当日去重用户数',
    -- 规约列
    status      TINYINT     DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by   VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag TINYINT     DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version     INT         DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_stat_event_date_code (stat_date, event_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件日聚合表：功能点击 pv/uv，软删重插幂等';

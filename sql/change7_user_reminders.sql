-- Change 7 — 三餐饮食提醒：user_reminders / reminder_send_log 两表
-- 作者: wanglx
-- 依据: openspec/changes/reminder（design D1）
--       doscFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、生成列活跃唯一）
-- 口径: 默认全开 08:30/12:00/18:30（MVP v1 §提醒，骨架 07:30 不采纳）；
--       时间 CHAR(5) 存 HH:mm；订阅额度 INT 默认 0（授权上报 +1、推送成功 -1）

USE zhenxinjian;

-- 用户提醒设置：每用户至多一条活跃记录（生成列兜底）
CREATE TABLE user_reminders (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    master_switch       TINYINT     NOT NULL DEFAULT 1 COMMENT '总开关：0关 1开',
    breakfast_switch    TINYINT     NOT NULL DEFAULT 1 COMMENT '早餐提醒开关：0关 1开',
    breakfast_time      CHAR(5)     NOT NULL DEFAULT '08:30' COMMENT '早餐提醒时间 HH:mm',
    lunch_switch        TINYINT     NOT NULL DEFAULT 1 COMMENT '午餐提醒开关：0关 1开',
    lunch_time          CHAR(5)     NOT NULL DEFAULT '12:00' COMMENT '午餐提醒时间 HH:mm',
    dinner_switch       TINYINT     NOT NULL DEFAULT 1 COMMENT '晚餐提醒开关：0关 1开',
    dinner_time         CHAR(5)     NOT NULL DEFAULT '18:30' COMMENT '晚餐提醒时间 HH:mm',
    subscribe_credit    INT         NOT NULL DEFAULT 0 COMMENT '订阅消息剩余额度（授权+1 推送成功-1）',
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
    UNIQUE KEY uk_user_reminders_user_active (user_id_active),
    KEY idx_remind_scan (master_switch, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户三餐提醒设置：总开关+三餐开关/时间+订阅额度，每用户活跃唯一';

-- 提醒推送日志：仅记录真实下发尝试（成功/失败），跳过不写日志；支撑每日单次去重与失败追溯
CREATE TABLE reminder_send_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT       NOT NULL COMMENT '归属用户ID（关联 users.id）',
    remind_date     DATE         NOT NULL COMMENT '提醒日期',
    meal_type       TINYINT      NOT NULL COMMENT '餐别：1早 2午 3晚（见 MealTypeEnum，加餐不参与提醒）',
    send_status     TINYINT      NOT NULL COMMENT '推送结果：1成功 2失败（见 ReminderSendStatusEnum）',
    fail_reason     VARCHAR(255) DEFAULT NULL COMMENT '失败原因（微信返回 errcode/errmsg）',
    template_id     VARCHAR(64)  DEFAULT NULL COMMENT '使用的订阅消息模板ID',
    -- 规约列
    status          TINYINT      DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT      DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_log_dedup (user_id, remind_date, meal_type, send_status, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒推送日志：每日单次去重依据（当日该餐别成功日志存在即不再推送）';

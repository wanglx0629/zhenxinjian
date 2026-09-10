-- Change 6 — 碳循环模式：user_body 加 mode 列 + carb_cycle_plan / carb_cycle_day 两表
-- 作者: wanglx
-- 依据: openspec/changes/carb-cycle（design §2）
--       doscFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、软删表不建唯一索引）
-- 口径: 周期每日目标创建时固化（D1 快照不回改）；克数 DECIMAL(5,1) 与 foods/diet_records 一致；
--       池总量 DECIMAL(7,1)（目标体重≤200kg × 2.5 × 14 = 7000g 上限内）；热量 INT 取整

USE zhenxinjian;

-- 减脂模式：1=532 / 2=碳循环（见 DietModeEnum），存量默认 532
ALTER TABLE user_body
    ADD COLUMN mode TINYINT NOT NULL DEFAULT 1 COMMENT '减脂模式：1=532 2=碳循环（见 DietModeEnum）' AFTER cfc;

-- 碳循环周期计划：每用户至多一个进行中周期（服务层保证，软删表不建唯一索引）
CREATE TABLE carb_cycle_plan (
    id                      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id                 BIGINT          NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    cycle_days              INT             NOT NULL COMMENT '周期天数（7-14）',
    cfc                     DECIMAL(2,1)    NOT NULL DEFAULT 0.8 COMMENT '脂肪系数：0.8/1.0',
    start_date              DATE            NOT NULL COMMENT '起始日期（创建当日）',
    end_date                DATE            NOT NULL COMMENT '结束日期 = start_date + cycle_days - 1',
    weight_snapshot         DECIMAL(5,1)    NOT NULL COMMENT '创建时当前体重快照 kg（每日蛋白口径）',
    target_weight_snapshot  DECIMAL(5,1)    NOT NULL COMMENT '创建时目标体重快照 kg（碳/脂池口径）',
    carb_pool               DECIMAL(7,1)    NOT NULL COMMENT '碳水池 g = 目标体重×2.5×N',
    fat_pool                DECIMAL(7,1)    NOT NULL COMMENT '脂肪池 g = 目标体重×cfc×N',
    daily_protein           DECIMAL(5,1)    NOT NULL COMMENT '每日蛋白 g = 当前体重×1.5（周期内固定）',
    -- 规约列
    status                  TINYINT         DEFAULT 1 COMMENT '状态：1进行中 2已完成 3已终止（见 CyclePlanStatusEnum）',
    create_by               VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    update_by               VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag             TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version                 INT             DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_user_status (user_id, status, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳循环周期计划：图片公式三大池快照，进行中唯一（服务层保证）';

-- 碳循环每日日型计划：日型目标创建时固化，档案变更不回改（D1）
CREATE TABLE carb_cycle_day (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plan_id         BIGINT          NOT NULL COMMENT '所属周期（关联 carb_cycle_plan.id）',
    user_id         BIGINT          NOT NULL COMMENT '归属用户ID（冗余，迁移/清理直改）',
    day_index       INT             NOT NULL COMMENT '日序 1..N',
    day_date        DATE            NOT NULL COMMENT '日历日',
    day_type        TINYINT         NOT NULL COMMENT '日型：1高碳 2中碳 3低碳（见 CycleDayTypeEnum）',
    is_sport        TINYINT         NOT NULL DEFAULT 0 COMMENT '运动日：0否 1是',
    carb_g          DECIMAL(5,1)    NOT NULL COMMENT '当日目标碳水 g（1位小数）',
    protein_g       DECIMAL(5,1)    NOT NULL COMMENT '当日目标蛋白 g（周期内固定）',
    fat_g           DECIMAL(5,1)    NOT NULL COMMENT '当日目标脂肪 g（1位小数）',
    kcal            INT             NOT NULL COMMENT '当日目标能量 kcal（4/4/9 取整）',
    -- 规约列
    status          TINYINT         DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT             DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_plan (plan_id, delete_flag),
    KEY idx_user_date (user_id, day_date, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳循环每日日型计划：高/中/低碳目标快照，软删保历史';

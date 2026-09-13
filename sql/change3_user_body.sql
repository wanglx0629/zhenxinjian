-- Change 3 — 身体数据档案：user_body（当前档案）+ user_body_history（历史版本）
-- 作者: wanglx
-- 依据: openspec/changes/body-data-core-calc（design D1/D2/D3）；ADR-0002（覆盖当前值并留存历史版本）
--       docsFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、生成列活跃唯一、DOUBLE 存克数）
-- 口径: BMR/TDEE/基准热量四舍五入取整 kcal（INT 存）；三宏克数保留 1 位小数（DOUBLE 存）
-- 幂等: 两表 CREATE IF NOT EXISTS（已存在即跳过）；版本账见 schema_migrations。

USE zhenxinjian;

-- 身体档案表：每用户仅一条活跃记录（修改即覆盖），计算结果快照同表冗余
CREATE TABLE IF NOT EXISTS user_body (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID（关联 users.id，含游客）',
    -- 档案输入项
    gender          TINYINT         NOT NULL COMMENT '性别：1男 2女（业务口径，见 GenderEnum；与 users.gender 账号资料独立）',
    age             INT             NOT NULL COMMENT '年龄（12-80）',
    height          DOUBLE          NOT NULL COMMENT '身高cm（100-250）',
    weight          DOUBLE          NOT NULL COMMENT '当前体重kg（25-200）',
    target_weight   DOUBLE          NOT NULL COMMENT '目标体重kg（25-200，须 ≤ 当前体重+0.1）',
    activity_level  TINYINT         NOT NULL COMMENT '活动系数档位：1久坐 2轻度 3中度 4高度（见 ActivityLevelEnum）',
    activity_factor DOUBLE          NOT NULL COMMENT '活动系数快照：1.2/1.375/1.55/1.725',
    deficit         INT             NOT NULL DEFAULT 200 COMMENT '减脂缺口kcal：200/300/400/500，默认200（见 DeficitOptionEnum）',
    cfc             DOUBLE          NOT NULL DEFAULT 0.8 COMMENT '脂肪系数（碳循环预留）：0.8/1.0，本变更仅持久化',
    -- 计算结果快照（BodyCalcService 保存时同步重算，全产品唯一真源）
    bmr             INT             NOT NULL COMMENT '基础代谢kcal（Mifflin-St Jeor，取整）',
    tdee            INT             NOT NULL COMMENT '每日总消耗kcal（BMR×活动系数，取整）',
    target_kcal     INT             NOT NULL COMMENT '基准热量kcal（TDEE-缺口，取整）',
    target_carb     DOUBLE          NOT NULL COMMENT '目标碳水g（基准热量×50%÷4，1位小数）',
    target_protein  DOUBLE          NOT NULL COMMENT '目标蛋白g（基准热量×30%÷4，1位小数）',
    target_fat      DOUBLE          NOT NULL COMMENT '目标脂肪g（基准热量×20%÷9，1位小数）',
    -- 规约列
    status          TINYINT         DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT             DEFAULT 0 COMMENT '乐观锁版本号',
    -- 活跃唯一：每用户至多一条未删除档案
    user_id_active  BIGINT          GENERATED ALWAYS AS (IF(delete_flag = 0, user_id, NULL)) STORED COMMENT '活跃用户生成列，兜底每用户唯一活跃档案',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_body_user_active (user_id_active),
    KEY idx_user_body_status (status),
    KEY idx_user_body_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身体档案表：当前档案+计算结果快照，每用户活跃唯一';

-- 身体档案历史表：修改前的完整档案整体归档（追加写，不参与计算）
CREATE TABLE IF NOT EXISTS user_body_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_body_id    BIGINT          DEFAULT NULL COMMENT '来源档案ID（user_body.id，游客迁移冲突降级归档时可为空语义同原行）',
    user_id         BIGINT          NOT NULL COMMENT '用户ID（归档时归属）',
    gender          TINYINT         NOT NULL COMMENT '性别：1男 2女',
    age             INT             NOT NULL COMMENT '年龄',
    height          DOUBLE          NOT NULL COMMENT '身高cm',
    weight          DOUBLE          NOT NULL COMMENT '当前体重kg',
    target_weight   DOUBLE          NOT NULL COMMENT '目标体重kg',
    activity_level  TINYINT         NOT NULL COMMENT '活动系数档位',
    activity_factor DOUBLE          NOT NULL COMMENT '活动系数快照',
    deficit         INT             NOT NULL COMMENT '减脂缺口kcal',
    cfc             DOUBLE          NOT NULL COMMENT '脂肪系数',
    bmr             INT             NOT NULL COMMENT 'BMR快照kcal',
    tdee            INT             NOT NULL COMMENT 'TDEE快照kcal',
    target_kcal     INT             NOT NULL COMMENT '基准热量快照kcal',
    target_carb     DOUBLE          NOT NULL COMMENT '目标碳水快照g',
    target_protein  DOUBLE          NOT NULL COMMENT '目标蛋白快照g',
    target_fat      DOUBLE          NOT NULL COMMENT '目标脂肪快照g',
    archived_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时刻（被新版本覆盖的时间）',
    status          TINYINT         DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（归档行写入时间）',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_ubh_user (user_id),
    KEY idx_ubh_archived_at (archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身体档案历史表：修改前档案整体留痕，仅查询不参与计算';

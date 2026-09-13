-- Change 5 — 饮食记录：diet_records（三类来源单表 + 食物快照冗余）
-- 作者: wanglx
-- 依据: openspec/changes/diet-record（design D1/D2）
--       docsFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、DOUBLE 存克数）
-- 口径: 实际摄入克数 DOUBLE（随记录累计防漂移）；每 100g 快照 DECIMAL(5,1) 与 foods 表一致；
--       热量 INT 取整；手动输入快照列 NULL、amount_g 占位 1
-- 幂等: CREATE IF NOT EXISTS（已存在即跳过）；版本账见 schema_migrations。

USE zhenxinjian;

-- 饮食记录表：按用户+日期记录各餐别摄入，食物快照冗余（食物改/删不影响历史，不变量 I8）
CREATE TABLE IF NOT EXISTS diet_records (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    record_date     DATE            NOT NULL COMMENT '记录日期（不允许未来日期）',
    meal_type       TINYINT         NOT NULL COMMENT '餐别：1早餐 2午餐 3晚餐 4加餐（见 MealTypeEnum）',
    source          TINYINT         NOT NULL COMMENT '来源：1内置食物 2自定义食物 3手动输入（见 DietRecordSourceEnum）',
    food_id         BIGINT          DEFAULT NULL COMMENT '食物ID（溯源参考，不回查；手动输入为空）',
    food_name       VARCHAR(100)    NOT NULL COMMENT '食物名称快照（提交时刻名称；手动输入即用户填写名）',
    carb_100g       DECIMAL(5,1)    DEFAULT NULL COMMENT '快照：碳水 g/100g（食物来源必填）',
    protein_100g    DECIMAL(5,1)    DEFAULT NULL COMMENT '快照：蛋白 g/100g（食物来源必填）',
    fat_100g        DECIMAL(5,1)    DEFAULT NULL COMMENT '快照：脂肪 g/100g（食物来源必填）',
    kcal_100g       INT             DEFAULT NULL COMMENT '快照：能量 kcal/100g（食物来源必填）',
    amount_g        DOUBLE          NOT NULL COMMENT '份量克数（食物来源=实际克数；手动输入占位1）',
    carb_g          DOUBLE          NOT NULL COMMENT '实际摄入碳水 g（1位小数）',
    protein_g       DOUBLE          NOT NULL COMMENT '实际摄入蛋白 g（1位小数）',
    fat_g           DOUBLE          NOT NULL COMMENT '实际摄入脂肪 g（1位小数）',
    kcal            INT             NOT NULL COMMENT '实际摄入能量 kcal（取整）',
    remark          VARCHAR(100)    DEFAULT NULL COMMENT '备注（可空）',
    -- 规约列
    status          TINYINT         DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT             DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_user_date (user_id, record_date, delete_flag),
    KEY idx_diet_meal (user_id, record_date, meal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录表：三类来源单表+食物快照冗余，软删保历史';

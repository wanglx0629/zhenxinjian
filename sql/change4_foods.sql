-- Change 4 — 食物库：foods（内置 200 条 + 用户自定义，单表 + source 隔离）
-- 作者: wanglx
-- 依据: openspec/changes/food-library（design D1/D2/D3/D4）
--       doscFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、生成列活跃唯一）
--       doscFile/projectFile/04-食物库数据字典.md（每 100g 可食部口径、DECIMAL(5,1) 营养值）
-- 口径: 碳水/蛋白/脂肪/单份克数 DECIMAL(5,1)（静态权威值防浮点漂移）；能量 INT；serving 默认 100

USE zhenxinjian;

-- 食物库表：内置（source=1，全局只读，编号 F001–F200）+ 自定义（source=2，归属 user_id）
CREATE TABLE foods (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code            VARCHAR(8)      DEFAULT NULL COMMENT '食物编号（内置 F001–F200；自定义为空）',
    category_code   VARCHAR(2)      NOT NULL COMMENT '分类编号：01-10（见 FoodCategoryEnum）',
    category_name   VARCHAR(32)     NOT NULL COMMENT '分类名称（如「谷薯杂豆·主食」）',
    name            VARCHAR(100)    NOT NULL COMMENT '食物名称',
    alias           VARCHAR(100)    DEFAULT '' COMMENT '别名/俗称（搜索用，可空）',
    carb            DECIMAL(5,1)    NOT NULL COMMENT '碳水化合物 g/100g（1位小数）',
    protein         DECIMAL(5,1)    NOT NULL COMMENT '蛋白质 g/100g（1位小数）',
    fat             DECIMAL(5,1)    NOT NULL COMMENT '脂肪 g/100g（1位小数）',
    kcal            INT             NOT NULL COMMENT '能量 kcal/100g（按4/4/9换算参考值）',
    serving         DECIMAL(6,1)    NOT NULL DEFAULT 100.0 COMMENT '常用单份克数（默认100）',
    source          TINYINT         NOT NULL COMMENT '来源：1内置 2自定义（见 FoodSourceEnum）',
    user_id         BIGINT          DEFAULT NULL COMMENT '归属用户ID（仅自定义食物；内置为空）',
    -- 规约列
    status          TINYINT         DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT         DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT             DEFAULT 0 COMMENT '乐观锁版本号',
    -- 活跃唯一：同一用户至多一条活跃同名自定义食物（内置与已删恒为 NULL 不占约束）
    name_active     VARCHAR(191)    GENERATED ALWAYS AS (IF(delete_flag = 0 AND source = 2, CONCAT(user_id, ':', name), NULL)) STORED COMMENT '活跃自定义食物名生成列（user_id:name），兜底归属唯一',
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_code (code),
    UNIQUE KEY uk_food_name_active (name_active),
    KEY idx_food_category (category_code),
    KEY idx_food_source_user (source, user_id),
    KEY idx_food_status (status),
    KEY idx_food_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物库表：内置200条+用户自定义，source/user_id隔离';

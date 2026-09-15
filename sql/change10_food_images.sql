-- Change 10 — 食物图片：food_images（内置 200 张预热图 + 人工上传，一码一活跃图）
-- 作者: wanglx
-- 依据: docsFile/03-开发规范.md §4（逻辑删除 delete_flag、业务表必备 status、生成列活跃唯一、建表必建字典枚举）
--       食物图片体系 design：图片外置对象存储（MinIO 主/OSS 保底），启动预热从 classpath food-images/ 上传
-- 口径: object_key 固定 food/{code}.jpg（幂等重传）；url 冗余存完整访问地址（换存储域名需刷数据）
-- 幂等: CREATE IF NOT EXISTS（已存在即跳过）；版本账见 schema_migrations。

USE zhenxinjian;

-- 食物图片表：按 food_code 关联 foods（逻辑外键，不设物理外键）；同一食物至多一条活跃图片
CREATE TABLE IF NOT EXISTS food_images (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    food_code   VARCHAR(8)   DEFAULT NULL COMMENT '食物编号（内置 F001–F200）',
    object_key  VARCHAR(255) NOT NULL COMMENT '对象存储 ObjectKey（food/{code}.jpg）',
    url         VARCHAR(512) DEFAULT NULL COMMENT '图片完整 URL（冗余存，直接下发 C 端/后台）',
    source      TINYINT      NOT NULL COMMENT '来源：1启动预热 2人工上传（见 FoodImageSourceEnum）',
    -- 规约列
    status      TINYINT      DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    update_by   VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag TINYINT      DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version     INT          DEFAULT 0 COMMENT '乐观锁版本号',
    -- 活跃唯一：同一食物至多一条活跃图片（已删恒为 NULL 不占约束）
    code_active VARCHAR(8)   GENERATED ALWAYS AS (IF(delete_flag = 0, food_code, NULL)) STORED COMMENT '活跃图片生成列（food_code），兜底一码一图',
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_image_code_active (code_active),
    KEY idx_food_image_code (food_code),
    KEY idx_food_image_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物图片表：内置200张预热+人工上传，food_code 关联';

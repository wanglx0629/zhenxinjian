-- 数据库全量基线脚本（与 sql/change0~9 迁移链终态一致）
-- 作者: wanglx
-- 说明: 全新环境首次部署前执行本脚本，建库 + 全部 15 张业务表（schema_migrations 版本表由 SqlRunner 自动建账）。
--       全部 CREATE TABLE IF NOT EXISTS，重复执行安全；执行后再跑 change 链，脚本守卫自动跳过（零变更）并补记账。
--       新增/变更表结构时须同步本基线与新增 change<N> 脚本（docsFile/03-开发规范.md §4.7）。
-- 管理员初始化: 本脚本不含初始账号——首个管理员由部署者自行生成 BCrypt 哈希后手工 INSERT，
--       口令哈希不进版本库（见 tools/db/README「管理员初始化」）

CREATE DATABASE IF NOT EXISTS zhenxinjian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE zhenxinjian;

-- 用户表（涵盖常见开发字段，含软删除与乐观锁；含 change1 游客机制三列）
CREATE TABLE IF NOT EXISTS users (
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
    -- 游客机制（change1）：游客 = user_type=GUEST（wechat_openid 为 NULL）；正式用户 WECHAT
    user_type       VARCHAR(16)     NOT NULL DEFAULT 'WECHAT' COMMENT '用户类型：WECHAT微信正式用户 GUEST游客',
    guest_expire_at DATETIME        DEFAULT NULL COMMENT '游客到期时间（user_type=GUEST 时有效，签发时刻+3天）',
    merged_into     BIGINT          DEFAULT NULL COMMENT '游客合并到的正式用户ID（非空即已迁移，幂等判断）',
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
    KEY idx_create_time (create_time),
    KEY idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表：账号认证、资料、微信绑定、游客机制、权限与审计字段';

-- 索引字段说明：
-- uk_username_active：仅未删除用户唯一，软删后可重新注册同名
-- idx_email：邮箱索引，用于通知或找回密码
-- idx_phone：手机号索引，用于短信验证或账号绑定
-- uk_wechat_openid_active：仅未删除绑定唯一，软删后可重新绑定
-- idx_wechat_unionid：微信UnionID索引，用于跨公众号/小程序统一用户
-- idx_wechat_bind_status：微信绑定状态索引，用于查询已绑定用户
-- idx_status：账号状态索引，用于查询正常用户
-- idx_create_time：创建时间索引，用于查询用户创建时间
-- idx_user_type：用户类型索引，用于游客清理任务扫描

-- 身体档案表（change3 + change6 mode 列 + change8 下调态两列）：每用户仅一条活跃记录（修改即覆盖），计算结果快照同表冗余
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
    mode            TINYINT         NOT NULL DEFAULT 1 COMMENT '减脂模式：1=532 2=碳循环（见 DietModeEnum）',
    -- 计算结果快照（BodyCalcService 保存时同步重算，全产品唯一真源）
    bmr             INT             NOT NULL COMMENT '基础代谢kcal（Mifflin-St Jeor，取整）',
    tdee            INT             NOT NULL COMMENT '每日总消耗kcal（BMR×活动系数，取整）',
    target_kcal     INT             NOT NULL COMMENT '基准热量kcal（TDEE-缺口，取整）',
    target_carb     DOUBLE          NOT NULL COMMENT '目标碳水g（基准热量×50%÷4，1位小数）',
    target_protein  DOUBLE          NOT NULL COMMENT '目标蛋白g（基准热量×30%÷4，1位小数）',
    target_fat      DOUBLE          NOT NULL COMMENT '目标脂肪g（基准热量×20%÷9，1位小数）',
    -- 平台下调态（change8）
    is_adjusted     TINYINT         NOT NULL DEFAULT 0 COMMENT '平台下调态：0未下调 1已下调',
    trigger_weight  DOUBLE          DEFAULT NULL COMMENT '触发下调的参考体重kg（下调生效时记录，恢复解除清零）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身体档案表：当前档案+计算结果快照+减脂模式+平台下调态，每用户活跃唯一';

-- 身体档案历史表（change3）：修改前的完整档案整体归档（追加写，不参与计算）
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

-- 食物库表（change4）：内置（source=1，全局只读，编号 F001–F200）+ 自定义（source=2，归属 user_id）
CREATE TABLE IF NOT EXISTS foods (
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

-- 饮食记录表（change5）：按用户+日期记录各餐别摄入，食物快照冗余（食物改/删不影响历史，不变量 I8）
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

-- 碳循环周期计划（change6）：每用户至多一个进行中周期（服务层保证，软删表不建唯一索引）
CREATE TABLE IF NOT EXISTS carb_cycle_plan (
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

-- 碳循环每日日型计划（change6）：日型目标创建时固化，档案变更不回改（D1）
CREATE TABLE IF NOT EXISTS carb_cycle_day (
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

-- 用户提醒设置（change7）：每用户至多一条活跃记录（生成列兜底）
CREATE TABLE IF NOT EXISTS user_reminders (
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

-- 提醒推送日志（change7）：仅记录真实下发尝试（成功/失败），跳过不写日志；支撑每日单次去重与失败追溯
CREATE TABLE IF NOT EXISTS reminder_send_log (
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

-- 用户经期设置（change8）：每用户至多一条活跃记录（生成列兜底）
CREATE TABLE IF NOT EXISTS user_menstrual (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    enabled             TINYINT     NOT NULL DEFAULT 0 COMMENT '是否开启经期管理：0关 1开',
    period_start_date   DATE        DEFAULT NULL COMMENT '末次月经起始日（开启时必填，不得为未来日期）',
    cycle_len           TINYINT     NOT NULL DEFAULT 28 COMMENT '周期长度 L（21-35，默认28）',
    period_days         TINYINT     NOT NULL DEFAULT 5 COMMENT '经期天数 D（3-10，默认5）',
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
    UNIQUE KEY uk_user_menstrual_user_active (user_id_active),
    KEY idx_user_menstrual_user (user_id, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户经期设置：开启/起始日/周期长度/经期天数，每用户活跃唯一';

-- 体重记录（change8）：永久留存按日多版本，业务同日幂等由服务层软删再插实现
CREATE TABLE IF NOT EXISTS weight_record (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    record_date     DATE        NOT NULL COMMENT '记录日期',
    weight          DOUBLE      NOT NULL COMMENT '体重 kg（25-200）',
    -- 规约列
    status          TINYINT     DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT     DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT         DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_weight_user_date (user_id, record_date, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体重记录：按日永久留存，平台期判定数据源';

-- 调碳日志（change8）：仅追加写下调/恢复动作留痕，软删保历史，无唯一约束
CREATE TABLE IF NOT EXISTS adjust_log (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT      NOT NULL COMMENT '归属用户ID（关联 users.id，含游客）',
    action          TINYINT     NOT NULL COMMENT '动作：1下调 2恢复（见 AdjustActionEnum）',
    trigger_weight  DOUBLE      NOT NULL COMMENT '触发当日体重 kg',
    -- 规约列
    status          TINYINT     DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by       VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by       VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（动作发生时刻）',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag     TINYINT     DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version         INT         DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_adjust_log_user (user_id, delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调碳日志：下调/恢复动作留痕（追溯 F21/F22）';

-- 埋点明细表（change9）：小程序批量上报（单批≤50），事件码白名单 TrackEventEnum
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

-- 每日活跃聚合表（change9）：每日 01:00 定时聚合昨日，软删重插幂等
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

-- 事件日聚合表（change9）：按事件码分组 pv/uv
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

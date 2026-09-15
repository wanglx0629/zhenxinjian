-- Change 12 — OCR 食物识别运行时参数（全部走 project_config，admin 后台可改即时生效）
-- 作者: wanglx
-- 依据: openspec change 2026-09-15-ocr-food-recognize（GLM-4.6V-Flash 视觉识别）
-- 口径: ocr.api-key 为 SECRET 已在 change11 无需种子，真实值不进 git，经 admin 后台录入（AES 加密落库）；
--       其余 5 键明文种子。配置键小写点分，活跃唯一。
-- 幂等: INSERT IGNORE + schema_migrations 补账。

USE zhenxinjian;

-- OCR 运行时参数种子（INSERT IGNORE 幂等，不覆盖后台已改值）
INSERT IGNORE INTO project_config (config_key, config_value, value_type, remark, create_by)
VALUES
('ocr.model',           'glm-4.6v-flash', 1, '食物识别视觉模型名（智谱），可随时换', 'system'),
('ocr.enabled',         'true',           3, '食物识别总开关（false 拒识）', 'system'),
('ocr.daily-limit',     '20',             2, '每用户每日识别次数上限', 'system'),
('ocr.timeout-seconds', '30',             2, '识别调用超时（秒）', 'system'),
('ocr.cache-ttl-hours', '24',             2, '同图识别结果缓存时长（小时）', 'system');

-- 版本账补账（幂等）
INSERT IGNORE INTO schema_migrations(version, script) VALUES (12, 'change12_ocr_config.sql');

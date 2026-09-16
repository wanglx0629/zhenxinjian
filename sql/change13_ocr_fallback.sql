-- Change 13 — OCR 主模型故障转移与熔断降级参数（全部走 project_config，admin 后台可改即时生效）
-- 作者: wanglx
-- 依据: openspec change 2026-09-16-ocr-model-fallback
-- 口径: 备用模型种子预置两个智谱免费档视觉模型（GLM-4.1V-Thinking-Flash、GLM-4V-Flash），
--       同一 api-key 下模型名仅为请求参数，无需额外凭据；admin 可改为 [] 关闭故障转移。
--       代码侧缺省仍按空列表处理（fail-open），种子值不影响缺省语义。
-- 幂等: INSERT IGNORE + schema_migrations 补账。

USE zhenxinjian;

-- 故障转移/熔断参数种子（INSERT IGNORE 幂等，不覆盖后台已改值）
INSERT IGNORE INTO project_config (config_key, config_value, value_type, remark, create_by)
VALUES
('ocr.fallback-models',        '["GLM-4.1V-Thinking-Flash","GLM-4V-Flash"]', 4, '有序备用视觉模型名（JSON 数组，与主模型同 api-key）；空数组=关闭故障转移', 'system'),
('ocr.circuit-fail-threshold', '3',   2, '模型连续可转移失败几次触发熔断（下限 1）', 'system'),
('ocr.circuit-open-seconds',   '300', 2, '模型熔断锁定时长（秒，下限 1），到期自动试探恢复', 'system');

-- 版本账补账（幂等）
INSERT IGNORE INTO schema_migrations(version, script) VALUES (13, 'change13_ocr_fallback.sql');

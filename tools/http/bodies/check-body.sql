SELECT id, user_id, weight, bmr, version, delete_flag, update_time FROM user_body WHERE user_id = 7;
SELECT id, user_body_id, weight, bmr, archived_at FROM user_body_history WHERE user_id = 7 ORDER BY id;
SELECT COUNT(*) AS active_cnt FROM user_body WHERE user_id = 7 AND delete_flag = 0;

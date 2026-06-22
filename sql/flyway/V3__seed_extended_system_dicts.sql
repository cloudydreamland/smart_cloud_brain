UPDATE system_dict SET dict_value = '待接诊', sort = 1, status = 'ENABLED'
WHERE dict_type = 'REGISTRATION_STATUS' AND dict_key = 'CREATED';
UPDATE system_dict SET dict_value = '已取消', sort = 4, status = 'ENABLED'
WHERE dict_type = 'REGISTRATION_STATUS' AND dict_key = 'CANCELLED';
UPDATE system_dict SET dict_value = 'AI 已推荐', sort = 1, status = 'ENABLED'
WHERE dict_type = 'TRIAGE_STATUS' AND dict_key = 'AI_RECOMMENDED';
UPDATE system_dict SET dict_value = '已分配医生', sort = 2, status = 'ENABLED'
WHERE dict_type = 'TRIAGE_STATUS' AND dict_key = 'ASSIGNED';
UPDATE system_dict SET dict_value = '已关闭', sort = 4, status = 'ENABLED'
WHERE dict_type = 'TRIAGE_STATUS' AND dict_key = 'CLOSED';

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'REGISTRATION_STATUS', 'CONFIRMED', '已确认', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'REGISTRATION_STATUS' AND dict_key = 'CONFIRMED');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'REGISTRATION_STATUS', 'COMPLETED', '已完成', 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'REGISTRATION_STATUS' AND dict_key = 'COMPLETED');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'TRIAGE_STATUS', 'MANUAL_REQUIRED', '待人工处理', 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'TRIAGE_STATUS' AND dict_key = 'MANUAL_REQUIRED');

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'SYSTEM_STATUS', 'ENABLED', '启用', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'SYSTEM_STATUS' AND dict_key = 'ENABLED');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'SYSTEM_STATUS', 'DISABLED', '停用', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'SYSTEM_STATUS' AND dict_key = 'DISABLED');

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'PROMPT_TASK_TYPE', 'TRIAGE', '智能分诊', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'PROMPT_TASK_TYPE' AND dict_key = 'TRIAGE');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'PROMPT_TASK_TYPE', 'MEDICAL_RECORD', '病历生成', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'PROMPT_TASK_TYPE' AND dict_key = 'MEDICAL_RECORD');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'PROMPT_TASK_TYPE', 'PRESCRIPTION_CHECK', '处方审核', 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'PROMPT_TASK_TYPE' AND dict_key = 'PRESCRIPTION_CHECK');

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'RISK_LEVEL', 'UNREVIEWED', '未审核', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'RISK_LEVEL' AND dict_key = 'UNREVIEWED');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'RISK_LEVEL', 'LOW', '低风险', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'RISK_LEVEL' AND dict_key = 'LOW');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'RISK_LEVEL', 'MEDIUM', '中风险', 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'RISK_LEVEL' AND dict_key = 'MEDIUM');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'RISK_LEVEL', 'HIGH', '高风险', 4, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'RISK_LEVEL' AND dict_key = 'HIGH');

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'READ_STATUS', 'UNREAD', '未读', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'READ_STATUS' AND dict_key = 'UNREAD');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'READ_STATUS', 'READ', '已读', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'READ_STATUS' AND dict_key = 'READ');

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'GENDER', 'MALE', '男', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'GENDER' AND dict_key = 'MALE');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'GENDER', 'FEMALE', '女', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'GENDER' AND dict_key = 'FEMALE');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'GENDER', 'UNKNOWN', '未知', 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'GENDER' AND dict_key = 'UNKNOWN');

INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'SYSTEM_STATUS', '系统状态', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'SYSTEM_STATUS');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'PROMPT_TASK_TYPE', 'AI 任务类型', 2, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'PROMPT_TASK_TYPE');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'REGISTRATION_STATUS', '挂号状态', 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'REGISTRATION_STATUS');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'TRIAGE_STATUS', '分诊状态', 4, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'TRIAGE_STATUS');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'RISK_LEVEL', '风险等级', 5, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'RISK_LEVEL');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'READ_STATUS', '阅读状态', 6, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'READ_STATUS');
INSERT INTO system_dict (dict_type, dict_key, dict_value, sort, status)
SELECT 'DICT_TYPE', 'GENDER', '性别', 7, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM system_dict WHERE dict_type = 'DICT_TYPE' AND dict_key = 'GENDER');

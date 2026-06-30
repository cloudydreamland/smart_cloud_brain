-- 补充更新：将更多 doctor_id=1 的病历标记为医生手动录入
UPDATE medical_record SET ai_generated = FALSE WHERE id IN (20, 22, 23);

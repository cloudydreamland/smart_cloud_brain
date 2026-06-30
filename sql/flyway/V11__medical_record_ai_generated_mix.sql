-- 将部分病历标记为医生手动录入，使页面展示两种标签
UPDATE medical_record SET ai_generated = FALSE WHERE id IN (21, 25, 28, 30, 32);

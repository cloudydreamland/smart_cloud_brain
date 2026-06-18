ALTER TABLE ai_generation_log ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'UNKNOWN';
UPDATE ai_generation_log SET status = 'UNKNOWN' WHERE status IS NULL;
ALTER TABLE ai_generation_log ALTER COLUMN status SET DEFAULT 'UNKNOWN';
ALTER TABLE ai_generation_log ALTER COLUMN status SET NOT NULL;

ALTER TABLE ai_schedule_suggestion ADD COLUMN IF NOT EXISTS source VARCHAR(50) DEFAULT 'unknown';
ALTER TABLE ai_schedule_suggestion ADD COLUMN IF NOT EXISTS degraded BOOLEAN DEFAULT FALSE;
UPDATE ai_schedule_suggestion SET source = 'legacy-rule' WHERE source IS NULL;
UPDATE ai_schedule_suggestion SET degraded = TRUE WHERE degraded IS NULL;
ALTER TABLE ai_schedule_suggestion ALTER COLUMN source SET NOT NULL;
ALTER TABLE ai_schedule_suggestion ALTER COLUMN degraded SET NOT NULL;

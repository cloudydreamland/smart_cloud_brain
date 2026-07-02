ALTER TABLE registration ADD COLUMN IF NOT EXISTS visitor_id BIGINT;
ALTER TABLE registration ADD COLUMN IF NOT EXISTS visitor_type VARCHAR(20);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS visitor_name VARCHAR(80);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS visitor_relationship VARCHAR(40);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS visitor_gender VARCHAR(20);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS visitor_age INT;

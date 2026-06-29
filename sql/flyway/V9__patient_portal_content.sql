CREATE TABLE IF NOT EXISTS patient_notice (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  link_type VARCHAR(32) DEFAULT 'NONE',
  link_url VARCHAR(500),
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  pinned BOOLEAN NOT NULL DEFAULT FALSE,
  sort INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_notice_public
  ON patient_notice(status, deleted, pinned, sort);

CREATE INDEX IF NOT EXISTS idx_patient_notice_window
  ON patient_notice(start_time, end_time);

CREATE INDEX IF NOT EXISTS idx_patient_notice_updated
  ON patient_notice(deleted, updated_at DESC);

CREATE TABLE IF NOT EXISTS patient_recommendation (
  id BIGSERIAL PRIMARY KEY,
  recommend_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  title VARCHAR(120),
  description VARCHAR(500),
  image_url VARCHAR(500),
  image_object_key VARCHAR(500),
  sort INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_recommendation_public
  ON patient_recommendation(recommend_type, status, deleted, sort);

CREATE INDEX IF NOT EXISTS idx_patient_recommendation_target
  ON patient_recommendation(target_id);

CREATE INDEX IF NOT EXISTS idx_patient_recommendation_updated
  ON patient_recommendation(deleted, updated_at DESC);

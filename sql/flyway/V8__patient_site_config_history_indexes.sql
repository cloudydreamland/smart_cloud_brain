CREATE INDEX IF NOT EXISTS idx_patient_site_config_key_version
  ON patient_site_config(config_key, version DESC);

CREATE INDEX IF NOT EXISTS idx_patient_site_config_key_status_version
  ON patient_site_config(config_key, status, version DESC);

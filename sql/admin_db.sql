CREATE DATABASE IF NOT EXISTS admin_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE admin_db;

CREATE TABLE IF NOT EXISTS admin_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT NOT NULL,
  operation_type VARCHAR(50) NOT NULL,
  operation_target VARCHAR(100) NOT NULL,
  biz_id BIGINT,
  detail_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_admin_operation_user_time (admin_user_id, created_at),
  INDEX idx_admin_operation_target (operation_target, biz_id)
);

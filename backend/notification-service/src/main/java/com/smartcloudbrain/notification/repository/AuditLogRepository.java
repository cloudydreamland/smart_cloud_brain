package com.smartcloudbrain.notification.repository;

import com.smartcloudbrain.notification.entity.AuditLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  Optional<AuditLog> findByEventId(String eventId);
}

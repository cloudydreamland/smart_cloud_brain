package com.smartcloudbrain.notification.repository;

import com.smartcloudbrain.notification.entity.NotificationMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
  List<NotificationMessage> findByDoctorId(Long doctorId);
  List<NotificationMessage> findByDoctorIdAndReadStatus(Long doctorId, String readStatus);
  Optional<NotificationMessage> findFirstByDoctorIdAndTriageRecordIdAndType(Long doctorId, Long triageRecordId, String type);
}


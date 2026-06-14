package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.NotificationMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
  List<NotificationMessage> findByDoctorId(Long doctorId);
}

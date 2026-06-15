package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.NotificationMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
  List<NotificationMessage> findByDoctorId(Long doctorId);
  List<NotificationMessage> findByDoctorIdAndReadStatus(Long doctorId, String readStatus);
}



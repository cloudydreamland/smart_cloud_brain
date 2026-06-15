package com.smartcloudbrain.registration.repository;

import com.smartcloudbrain.registration.entity.AppointmentSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
  List<AppointmentSlot> findByStatusAndStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(String status, LocalDateTime startTime);
}

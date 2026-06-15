package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.AppointmentSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
  List<AppointmentSlot> findByStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(LocalDateTime startTime);
}

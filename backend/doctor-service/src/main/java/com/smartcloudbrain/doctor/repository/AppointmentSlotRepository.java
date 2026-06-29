package com.smartcloudbrain.doctor.repository;

import com.smartcloudbrain.doctor.entity.AppointmentSlot;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
  List<AppointmentSlot> findByEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(LocalDateTime endTime);
  Optional<AppointmentSlot> findByScheduleId(Long scheduleId);
  List<AppointmentSlot> findByScheduleIdIn(Collection<Long> scheduleIds);
}

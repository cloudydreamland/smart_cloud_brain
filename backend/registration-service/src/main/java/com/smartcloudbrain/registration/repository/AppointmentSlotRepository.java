package com.smartcloudbrain.registration.repository;

import com.smartcloudbrain.registration.entity.AppointmentSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
  List<AppointmentSlot> findByStatusAndStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(String status, LocalDateTime startTime);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select slot from AppointmentSlot slot where slot.id = :id")
  Optional<AppointmentSlot> findByIdForUpdate(@Param("id") Long id);
}

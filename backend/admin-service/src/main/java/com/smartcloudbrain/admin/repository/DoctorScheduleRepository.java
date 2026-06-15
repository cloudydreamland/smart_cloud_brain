package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.DoctorSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
  List<DoctorSchedule> findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(LocalDate workDate);
}

package com.smartcloudbrain.doctor.repository;

import com.smartcloudbrain.doctor.entity.DoctorSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
  List<DoctorSchedule> findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(LocalDate workDate);
}

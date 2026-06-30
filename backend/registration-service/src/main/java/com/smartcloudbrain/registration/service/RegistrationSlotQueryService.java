package com.smartcloudbrain.registration.service;

import com.smartcloudbrain.registration.entity.AppointmentSlot;
import com.smartcloudbrain.registration.entity.Department;
import com.smartcloudbrain.registration.entity.Doctor;
import com.smartcloudbrain.registration.repository.AppointmentSlotRepository;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RegistrationSlotQueryService {

  private final AppointmentSlotRepository appointmentSlotRepository;
  private final DoctorRepository doctorRepository;
  private final DepartmentRepository departmentRepository;

  public RegistrationSlotQueryService(
      AppointmentSlotRepository appointmentSlotRepository,
      DoctorRepository doctorRepository,
      DepartmentRepository departmentRepository
  ) {
    this.appointmentSlotRepository = appointmentSlotRepository;
    this.doctorRepository = doctorRepository;
    this.departmentRepository = departmentRepository;
  }

  @Cacheable(cacheNames = "registration:slots", key = "'available'")
  public List<Map<String, Object>> availableSlots() {
    return appointmentSlotRepository.findByStatusAndEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(
            "AVAILABLE",
            LocalDateTime.now()
        ).stream()
        .filter(slot -> slot.getRemainingCapacity() != null && slot.getRemainingCapacity() > 0)
        .map(this::slotView)
        .toList();
  }

  private Map<String, Object> slotView(AppointmentSlot slot) {
    Doctor doctor = doctorRepository.findById(slot.getDoctorId()).orElse(null);
    Department department = departmentRepository.findById(slot.getDepartmentId()).orElse(null);
    return Map.of(
        "slotId", slot.getId(),
        "doctorId", slot.getDoctorId(),
        "doctorName", doctor == null ? "" : doctor.getName(),
        "departmentId", slot.getDepartmentId(),
        "departmentName", department == null ? "" : department.getName(),
        "startTime", slot.getStartTime().toString(),
        "endTime", slot.getEndTime() == null ? "" : slot.getEndTime().toString(),
        "capacity", slot.getCapacity() == null ? 0 : slot.getCapacity(),
        "remainingCapacity", slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity(),
        "status", slot.getStatus()
    );
  }
}

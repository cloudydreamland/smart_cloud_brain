package com.smartcloudbrain.registration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.registration.entity.AppointmentSlot;
import com.smartcloudbrain.registration.entity.Department;
import com.smartcloudbrain.registration.entity.Doctor;
import com.smartcloudbrain.registration.repository.AppointmentSlotRepository;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationSlotQueryServiceTest {

  @Mock private AppointmentSlotRepository appointmentSlotRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private DepartmentRepository departmentRepository;
  @InjectMocks private RegistrationSlotQueryService service;

  @Test
  void returnsOnlyAvailableSlotsWithRemainingCapacity() {
    AppointmentSlot available = slot(4L, 2L, 3L, 1, "AVAILABLE");
    when(appointmentSlotRepository.findByStatusAndEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(any(), any()))
        .thenReturn(List.of(available, slot(5L, 2L, 3L, 0, "AVAILABLE")));
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor(2L)));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department(3L)));

    var slots = service.availableSlots();

    assertEquals(1, slots.size());
    assertEquals(4L, slots.get(0).get("slotId"));
    assertEquals("doctor", slots.get(0).get("doctorName"));
    assertEquals("Cardiology", slots.get(0).get("departmentName"));
  }

  private static Doctor doctor(Long id) {
    Doctor doctor = new Doctor();
    doctor.setId(id);
    doctor.setName("doctor");
    return doctor;
  }

  private static Department department(Long id) {
    Department department = new Department();
    department.setId(id);
    department.setName("Cardiology");
    return department;
  }

  private static AppointmentSlot slot(Long id, Long doctorId, Long departmentId, Integer remaining, String status) {
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(id);
    slot.setDoctorId(doctorId);
    slot.setDepartmentId(departmentId);
    slot.setCapacity(1);
    slot.setRemainingCapacity(remaining);
    slot.setStatus(status);
    slot.setStartTime(LocalDateTime.now().plusHours(1));
    slot.setEndTime(LocalDateTime.now().plusHours(2));
    return slot;
  }
}

package com.smartcloudbrain.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.doctor.dto.internal.InternalSchedulePublishRequest;
import com.smartcloudbrain.doctor.entity.AppointmentSlot;
import com.smartcloudbrain.doctor.entity.DoctorSchedule;
import com.smartcloudbrain.doctor.entity.Doctor;
import com.smartcloudbrain.doctor.entity.Department;
import com.smartcloudbrain.doctor.repository.AppointmentSlotRepository;
import com.smartcloudbrain.doctor.repository.DepartmentRepository;
import com.smartcloudbrain.doctor.repository.DoctorRepository;
import com.smartcloudbrain.doctor.repository.DoctorScheduleRepository;
import com.smartcloudbrain.doctor.repository.RegistrationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceTest {

  @Mock private DoctorScheduleRepository doctorScheduleRepository;
  @Mock private AppointmentSlotRepository appointmentSlotRepository;
  @Mock private RegistrationRepository registrationRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private DepartmentRepository departmentRepository;
  @InjectMocks private DoctorScheduleService doctorScheduleService;

  @Test
  void publishSchedulesCreatesPublishedScheduleAndBookableSlot() {
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> {
      DoctorSchedule schedule = invocation.getArgument(0);
      schedule.setId(20L);
      return schedule;
    });
    when(appointmentSlotRepository.save(any(AppointmentSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(any(LocalDate.class)))
        .thenReturn(List.of());

    doctorScheduleService.publishSchedules(new InternalSchedulePublishRequest(List.of(
        new InternalSchedulePublishRequest.ScheduleItem(
            2L,
            3L,
            LocalDate.of(2026, 6, 17),
            "09:00-12:00",
            12
        )
    )));

    ArgumentCaptor<DoctorSchedule> scheduleCaptor = ArgumentCaptor.forClass(DoctorSchedule.class);
    ArgumentCaptor<AppointmentSlot> slotCaptor = ArgumentCaptor.forClass(AppointmentSlot.class);
    verify(doctorScheduleRepository).save(scheduleCaptor.capture());
    verify(appointmentSlotRepository).save(slotCaptor.capture());

    DoctorSchedule savedSchedule = scheduleCaptor.getValue();
    assertEquals("PUBLISHED", savedSchedule.getStatus());
    assertEquals(2L, savedSchedule.getDoctorId());
    assertEquals(3L, savedSchedule.getDepartmentId());
    assertEquals(12, savedSchedule.getCapacity());
    assertNotNull(savedSchedule.getUpdatedAt());

    AppointmentSlot savedSlot = slotCaptor.getValue();
    assertEquals(20L, savedSlot.getScheduleId());
    assertEquals(LocalDateTime.of(2026, 6, 17, 9, 0), savedSlot.getStartTime());
    assertEquals(LocalDateTime.of(2026, 6, 17, 12, 0), savedSlot.getEndTime());
    assertEquals(12, savedSlot.getCapacity());
    assertEquals(12, savedSlot.getRemainingCapacity());
    assertEquals("AVAILABLE", savedSlot.getStatus());
  }

  @Test
  void listsSchedulesAndSlotsWithDoctorAndDepartmentNames() {
    Doctor doctor = new Doctor();
    doctor.setName("王医生");
    Department department = new Department();
    department.setName("心内科");
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setId(1L);
    schedule.setDoctorId(2L);
    schedule.setDepartmentId(3L);
    schedule.setWorkDate(LocalDate.now().plusDays(1));
    schedule.setTimeRange("09:00-12:00");
    schedule.setCapacity(12);
    schedule.setStatus("PUBLISHED");
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(4L);
    slot.setScheduleId(1L);
    slot.setDoctorId(2L);
    slot.setDepartmentId(3L);
    slot.setStartTime(LocalDateTime.now().plusDays(1));
    slot.setEndTime(null);
    slot.setCapacity(null);
    slot.setRemainingCapacity(null);
    slot.setStatus("AVAILABLE");
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(any())).thenReturn(List.of(schedule));
    when(appointmentSlotRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(any())).thenReturn(List.of(slot));

    assertEquals("王医生", doctorScheduleService.schedules().get(0).get("doctorName"));
    assertEquals(0, doctorScheduleService.slots().get(0).get("capacity"));
  }

  @Test
  void acceptsEmptyPublishRequestAndCoversNullNameFallbacks() {
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(any())).thenReturn(List.of());
    assertEquals(List.of(), doctorScheduleService.publishSchedules(null));

    DoctorSchedule invalidForView = new DoctorSchedule();
    invalidForView.setId(1L);
    invalidForView.setDoctorId(null);
    invalidForView.setDepartmentId(null);
    invalidForView.setWorkDate(LocalDate.now());
    invalidForView.setTimeRange("09:00-12:00");
    invalidForView.setCapacity(1);
    invalidForView.setStatus("PUBLISHED");
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(any())).thenReturn(List.of(invalidForView));
    assertEquals("", doctorScheduleService.schedules().get(0).get("doctorName"));
    assertEquals("", doctorScheduleService.schedules().get(0).get("departmentName"));
  }
}

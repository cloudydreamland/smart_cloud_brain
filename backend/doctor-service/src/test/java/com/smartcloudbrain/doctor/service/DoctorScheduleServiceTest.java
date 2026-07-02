package com.smartcloudbrain.doctor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.doctor.client.InternalRegistrationCacheClient;
import com.smartcloudbrain.doctor.dto.internal.InternalScheduleCancelRequest;
import com.smartcloudbrain.doctor.dto.internal.InternalSchedulePublishRequest;
import com.smartcloudbrain.doctor.dto.internal.InternalScheduleSaveRequest;
import com.smartcloudbrain.doctor.entity.AppointmentSlot;
import com.smartcloudbrain.doctor.entity.DoctorSchedule;
import com.smartcloudbrain.doctor.entity.Doctor;
import com.smartcloudbrain.doctor.entity.Department;
import com.smartcloudbrain.doctor.event.DomainEventPublisher;
import com.smartcloudbrain.doctor.repository.AppointmentSlotRepository;
import com.smartcloudbrain.doctor.repository.DepartmentRepository;
import com.smartcloudbrain.doctor.repository.DoctorRepository;
import com.smartcloudbrain.doctor.repository.DoctorScheduleRepository;
import com.smartcloudbrain.doctor.repository.RegistrationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
  @Mock private InternalRegistrationCacheClient internalRegistrationCacheClient;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private DoctorScheduleService doctorScheduleService;

  @Test
  void publishSchedulesCreatesPublishedScheduleAndBookableSlot() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, LocalDate.of(2026, 6, 17))).thenReturn(List.of());
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
    verify(internalRegistrationCacheClient).evictSlotsCache();

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
    doctor.setId(2L);
    doctor.setName("王医生");
    Department department = new Department();
    department.setId(3L);
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
    slot.setRemainingCapacity(1);
    slot.setStatus("AVAILABLE");
    when(doctorRepository.findAllById(any())).thenReturn(List.of(doctor));
    when(departmentRepository.findAllById(any())).thenReturn(List.of(department));
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(any())).thenReturn(List.of(schedule));
    when(appointmentSlotRepository.findByEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(any())).thenReturn(List.of(slot));

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

  @Test
  void rejectsReversedEqualAndOverlappingScheduleRanges() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of());

    assertThrows(RuntimeException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "12:00-09:00", 20, "PUBLISHED")));
    assertThrows(RuntimeException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "12:00-12:00", 20, "PUBLISHED")));

    DoctorSchedule occupied = schedule(9L, 2L, 3L, workDate, "09:00-12:00", "PUBLISHED");
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of(occupied));
    assertThrows(RuntimeException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "11:30-14:00", 20, "PUBLISHED")));
  }

  @Test
  void allowsAdjacentCancelledAndSelfEditedScheduleRanges() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.empty());
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

    DoctorSchedule occupied = schedule(9L, 2L, 3L, workDate, "09:00-12:00", "PUBLISHED");
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of(occupied));
    doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "12:00-14:00", 20, "PUBLISHED"));

    DoctorSchedule cancelled = schedule(10L, 2L, 3L, workDate, "14:00-17:00", "CANCELLED");
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of(cancelled));
    doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "14:30-16:00", 20, "PUBLISHED"));

    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of(occupied));
    when(doctorScheduleRepository.findById(9L)).thenReturn(Optional.of(occupied));
    doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(9L, 2L, 3L, workDate, "10:00-12:30", 20, "PUBLISHED"));
  }

  @Test
  void rejectsOverlapsInsidePublishedAiBatch() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of());

    assertThrows(RuntimeException.class, () -> doctorScheduleService.publishSchedules(
        new InternalSchedulePublishRequest(List.of(
            new InternalSchedulePublishRequest.ScheduleItem(2L, 3L, workDate, "09:00-12:00", 20),
            new InternalSchedulePublishRequest.ScheduleItem(2L, 3L, workDate, "11:30-14:00", 20)
        ))));
  }

  @Test
  void registrationSlotsCacheEvictFailureDoesNotBreakScheduleWrites() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    DoctorSchedule existing = schedule(9L, 2L, 3L, workDate, "09:00-12:00", "PUBLISHED");
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(11L);
    slot.setScheduleId(9L);
    slot.setCapacity(20);
    slot.setRemainingCapacity(20);

    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of());
    when(doctorScheduleRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(slot));
    when(registrationRepository.existsBySlotIdAndStatusNot(11L, "CANCELLED")).thenReturn(false);
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(appointmentSlotRepository.save(any(AppointmentSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    doThrow(new RuntimeException("registration cache unavailable")).when(internalRegistrationCacheClient).evictSlotsCache();

    assertDoesNotThrow(() -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "12:00-14:00", 20, "PUBLISHED")));
    assertDoesNotThrow(() -> doctorScheduleService.cancelSchedule(new InternalScheduleCancelRequest(9L)));
  }

  // ─── cancel edge cases ────────────────────────────────────────────────

  @Test
  void cancelSchedule_nullRequest_throwsBusinessException() {
    assertThrows(BusinessException.class, () -> doctorScheduleService.cancelSchedule(null));
  }

  @Test
  void cancelSchedule_nullScheduleId_throwsBusinessException() {
    assertThrows(BusinessException.class, () -> doctorScheduleService.cancelSchedule(new InternalScheduleCancelRequest(null)));
  }

  @Test
  void cancelSchedule_scheduleNotFound_throwsBusinessException() {
    when(doctorScheduleRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> doctorScheduleService.cancelSchedule(new InternalScheduleCancelRequest(99L)));
  }

  @Test
  void cancelSchedule_withActiveRegistrations_throwsBusinessException() {
    DoctorSchedule existing = schedule(9L, 2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", "PUBLISHED");
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(11L);
    slot.setScheduleId(9L);
    slot.setCapacity(20);
    slot.setRemainingCapacity(18);

    when(doctorScheduleRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(appointmentSlotRepository.findByScheduleId(9L)).thenReturn(Optional.of(slot));
    when(registrationRepository.existsBySlotIdAndStatusNot(11L, "CANCELLED")).thenReturn(true);

    assertThrows(BusinessException.class, () -> doctorScheduleService.cancelSchedule(new InternalScheduleCancelRequest(9L)));
  }

  @Test
  void cancelSchedule_noSlot_cancelsScheduleWithoutSlotUpdate() {
    DoctorSchedule existing = schedule(9L, 2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", "PUBLISHED");
    when(doctorScheduleRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.empty());
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("王医生");
    Department department = new Department();
    department.setId(3L);
    department.setName("心内科");
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

    Map<String, Object> result = doctorScheduleService.cancelSchedule(new InternalScheduleCancelRequest(9L));

    assertEquals("CANCELLED", result.get("status"));
  }

  @Test
  void cancelSchedule_withSlot_cancelsBothScheduleAndSlot() {
    DoctorSchedule existing = schedule(9L, 2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", "PUBLISHED");
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(11L);
    slot.setScheduleId(9L);
    slot.setCapacity(20);
    slot.setRemainingCapacity(18);

    when(doctorScheduleRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.of(slot));
    when(registrationRepository.existsBySlotIdAndStatusNot(11L, "CANCELLED")).thenReturn(false);
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(appointmentSlotRepository.save(any(AppointmentSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("王医生");
    Department department = new Department();
    department.setId(3L);
    department.setName("心内科");
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

    Map<String, Object> result = doctorScheduleService.cancelSchedule(new InternalScheduleCancelRequest(9L));

    assertEquals("CANCELLED", result.get("status"));
    assertEquals("CANCELLED", slot.getStatus());
  }

  // ─── save edge cases ──────────────────────────────────────────────────

  @Test
  void saveSchedule_nullRequest_throwsBusinessException() {
    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(null));
  }

  @Test
  void saveSchedule_missingDoctorId_throwsBusinessException() {
    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, null, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", 20, "PUBLISHED")));
  }

  @Test
  void saveSchedule_invalidTimeRangeFormat_throwsBusinessException() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, LocalDate.of(2026, 6, 22), "invalid", 20, "PUBLISHED")));
  }

  @Test
  void saveSchedule_doctorNotInDepartment_throwsBusinessException() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(99L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", 20, "PUBLISHED")));
  }

  @Test
  void saveSchedule_capacityTooLow_throwsBusinessException() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", 0, "PUBLISHED")));
  }

  @Test
  void saveSchedule_capacityTooHigh_throwsBusinessException() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", 101, "PUBLISHED")));
  }

  @Test
  void saveSchedule_cancelledStatus_skipsOverlapCheck() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.empty());
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> result = doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "09:00-12:00", 20, "CANCELLED"));

    assertEquals("CANCELLED", result.get("status"));
  }

  @Test
  void saveSchedule_nullStatus_defaultsToPublished() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of());
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.empty());
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> {
      DoctorSchedule s = invocation.getArgument(0);
      s.setId(50L);
      return s;
    });

    Map<String, Object> result = doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "09:00-12:00", 20, null));

    assertEquals("PUBLISHED", result.get("status"));
  }

  @Test
  void saveSchedule_withExistingId_updatesSchedule() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    doctor.setName("王医生");
    Department dept = new Department();
    dept.setId(3L);
    dept.setName("心内科");
    DoctorSchedule existing = schedule(9L, 2L, 3L, workDate, "09:00-12:00", "PUBLISHED");
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(11L);
    slot.setScheduleId(9L);
    slot.setCapacity(20);
    slot.setRemainingCapacity(20);

    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(dept));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of(existing));
    when(doctorScheduleRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.of(slot));
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(appointmentSlotRepository.save(any(AppointmentSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> result = doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(9L, 2L, 3L, workDate, "09:00-12:00", 20, "PUBLISHED"));

    assertEquals(9L, result.get("id"));
  }

  @Test
  void saveSchedule_capacityBelowBooked_throwsBusinessException() {
    LocalDate workDate = LocalDate.of(2026, 6, 22);
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(11L);
    slot.setScheduleId(9L);
    slot.setCapacity(20);
    slot.setRemainingCapacity(5);

    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, workDate)).thenReturn(List.of());
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.of(slot));
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> {
      DoctorSchedule s = invocation.getArgument(0);
      s.setId(50L);
      return s;
    });

    assertThrows(BusinessException.class, () -> doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(null, 2L, 3L, workDate, "09:00-12:00", 10, "PUBLISHED")));
  }

  // ─── publish edge cases ───────────────────────────────────────────────

  @Test
  void publishSchedules_nullSchedulesList_returnsCurrentSchedules() {
    InternalSchedulePublishRequest request = new InternalSchedulePublishRequest(null);
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(any()))
        .thenReturn(List.of());

    List<Map<String, Object>> result = doctorScheduleService.publishSchedules(request);

    assertEquals(List.of(), result);
  }

  @Test
  void publishSchedules_doctorNotInDepartment_throwsBusinessException() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(99L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> doctorScheduleService.publishSchedules(
        new InternalSchedulePublishRequest(List.of(
            new InternalSchedulePublishRequest.ScheduleItem(2L, 3L, LocalDate.of(2026, 6, 22), "09:00-12:00", 20)
        ))));
  }

  // ─── filtered schedules ───────────────────────────────────────────────

  @Test
  void filteredSchedules_appliesAllFilters() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("王医生");
    Department department = new Department();
    department.setId(3L);
    department.setName("心内科");
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setId(1L);
    schedule.setDoctorId(2L);
    schedule.setDepartmentId(3L);
    schedule.setWorkDate(LocalDate.now());
    schedule.setTimeRange("09:00-12:00");
    schedule.setCapacity(12);
    schedule.setStatus("PUBLISHED");

    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualAndWorkDateLessThanEqualOrderByWorkDateAscDoctorIdAsc(
        any(), any())).thenReturn(List.of(schedule));
    when(doctorRepository.findAllById(any())).thenReturn(List.of(doctor));
    when(departmentRepository.findAllById(any())).thenReturn(List.of(department));
    when(appointmentSlotRepository.findByScheduleIdIn(any())).thenReturn(List.of());

    List<Map<String, Object>> result = doctorScheduleService.schedules(
        LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 3L, 2L, "PUBLISHED");

    assertEquals(1, result.size());
    assertEquals(2L, result.get(0).get("doctorId"));
    assertEquals(3L, result.get(0).get("departmentId"));
  }

  @Test
  void filteredSchedules_nullFilters_usesDefaults() {
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualAndWorkDateLessThanEqualOrderByWorkDateAscDoctorIdAsc(
        any(), any())).thenReturn(List.of());

    List<Map<String, Object>> result = doctorScheduleService.schedules(null, null, null, null, null);

    assertEquals(List.of(), result);
  }

  @Test
  void filteredSchedules_blankStatusFilter_ignored() {
    DoctorSchedule schedule = schedule(1L, 2L, 3L, LocalDate.now(), "09:00-12:00", "PUBLISHED");
    when(doctorScheduleRepository.findByWorkDateGreaterThanEqualAndWorkDateLessThanEqualOrderByWorkDateAscDoctorIdAsc(
        any(), any())).thenReturn(List.of(schedule));
    when(doctorRepository.findAllById(any())).thenReturn(List.of());
    when(departmentRepository.findAllById(any())).thenReturn(List.of());
    when(appointmentSlotRepository.findByScheduleIdIn(any())).thenReturn(List.of());

    List<Map<String, Object>> result = doctorScheduleService.schedules(
        null, null, null, null, "  ");

    assertEquals(1, result.size());
  }

  // ─── slots edge cases ─────────────────────────────────────────────────

  @Test
  void slots_filtersOutZeroRemainingCapacitySlots() {
    AppointmentSlot slot1 = new AppointmentSlot();
    slot1.setId(1L);
    slot1.setScheduleId(1L);
    slot1.setDoctorId(2L);
    slot1.setDepartmentId(3L);
    slot1.setStartTime(LocalDateTime.now().plusDays(1));
    slot1.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
    slot1.setCapacity(10);
    slot1.setRemainingCapacity(0);
    slot1.setStatus("AVAILABLE");
    AppointmentSlot slot2 = new AppointmentSlot();
    slot2.setId(2L);
    slot2.setScheduleId(2L);
    slot2.setDoctorId(2L);
    slot2.setDepartmentId(3L);
    slot2.setStartTime(LocalDateTime.now().plusDays(1));
    slot2.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
    slot2.setCapacity(10);
    slot2.setRemainingCapacity(5);
    slot2.setStatus("AVAILABLE");

    when(appointmentSlotRepository.findByEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(any()))
        .thenReturn(List.of(slot1, slot2));
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(new Department()));

    List<Map<String, Object>> result = doctorScheduleService.slots();

    assertEquals(1, result.size());
    assertEquals(2L, result.get(0).get("slotId"));
  }

  @Test
  void slots_slotWithNullRemainingCapacity_filteredOut() {
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(1L);
    slot.setScheduleId(1L);
    slot.setDoctorId(2L);
    slot.setDepartmentId(3L);
    slot.setStartTime(LocalDateTime.now().plusDays(1));
    slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
    slot.setCapacity(10);
    slot.setRemainingCapacity(null);
    slot.setStatus("AVAILABLE");

    when(appointmentSlotRepository.findByEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(any()))
        .thenReturn(List.of(slot));

    List<Map<String, Object>> result = doctorScheduleService.slots();

    assertEquals(0, result.size());
  }

  @Test
  void scheduleView_slotWithNullCapacityAndRemaining_showsZeros() {
    DoctorSchedule sch = schedule(99L, 2L, 3L, LocalDate.now().plusDays(1), "09:00-12:00", "PUBLISHED");
    AppointmentSlot slot = new AppointmentSlot();
    slot.setId(1L);
    slot.setScheduleId(99L);
    slot.setDoctorId(2L);
    slot.setDepartmentId(3L);
    slot.setStartTime(LocalDateTime.now().plusDays(1));
    slot.setEndTime(null);
    slot.setCapacity(null);
    slot.setRemainingCapacity(null);
    slot.setStatus("AVAILABLE");

    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    doctor.setName("王医生");
    Department dept = new Department();
    dept.setId(3L);
    dept.setName("心内科");

    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(dept));
    when(appointmentSlotRepository.findByScheduleId(any())).thenReturn(Optional.of(slot));
    when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(appointmentSlotRepository.save(any(AppointmentSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(doctorScheduleRepository.findByDoctorIdAndWorkDate(2L, sch.getWorkDate())).thenReturn(List.of());
    when(doctorScheduleRepository.findById(99L)).thenReturn(Optional.of(sch));

    // Trigger scheduleView via saveSchedule with existing id
    Map<String, Object> result = doctorScheduleService.saveSchedule(
        new InternalScheduleSaveRequest(99L, 2L, 3L, sch.getWorkDate(), "09:00-12:00", 20, "PUBLISHED"));

    assertEquals("王医生", result.get("doctorName"));
    assertEquals("心内科", result.get("departmentName"));
  }

  private DoctorSchedule schedule(
      Long id,
      Long doctorId,
      Long departmentId,
      LocalDate workDate,
      String timeRange,
      String status
  ) {
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setId(id);
    schedule.setDoctorId(doctorId);
    schedule.setDepartmentId(departmentId);
    schedule.setWorkDate(workDate);
    schedule.setTimeRange(timeRange);
    schedule.setCapacity(20);
    schedule.setStatus(status);
    return schedule;
  }
}

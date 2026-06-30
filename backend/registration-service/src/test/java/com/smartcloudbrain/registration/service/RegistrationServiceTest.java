package com.smartcloudbrain.registration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.common.redis.RedisIdempotencyGuard;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.registration.dto.registration.CreateRegistrationRequest;
import com.smartcloudbrain.registration.entity.AppointmentSlot;
import com.smartcloudbrain.registration.entity.Department;
import com.smartcloudbrain.registration.entity.Doctor;
import com.smartcloudbrain.registration.entity.Registration;
import com.smartcloudbrain.registration.event.DomainEventPublisher;
import com.smartcloudbrain.registration.repository.AppointmentSlotRepository;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import com.smartcloudbrain.registration.repository.PatientRepository;
import com.smartcloudbrain.registration.repository.RegistrationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

  @Mock private RegistrationRepository registrationRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private AppointmentSlotRepository appointmentSlotRepository;
  @Mock private CurrentUserService currentUserService;
  @Mock private RedisRateLimiter redisRateLimiter;
  @Mock private RedisIdempotencyGuard redisIdempotencyGuard;
  @Mock private RegistrationSlotQueryService registrationSlotQueryService;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private RegistrationService registrationService;

  @BeforeEach
  void setUp() {
    lenient().when(redisRateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
  }

  @Test
  void createsRegistrationAndConsumesLastSlot() {
    Doctor doctor = doctor(2L);
    Department department = department(3L);
    AppointmentSlot slot = slot(4L, 2L, 3L, 1, "AVAILABLE");
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
    when(appointmentSlotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
    when(registrationRepository.existsByPatientIdAndSlotIdAndStatusNot(1L, 4L, "CANCELLED")).thenReturn(false);
    when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
      Registration registration = invocation.getArgument(0);
      registration.setId(6L);
      return registration;
    });

    var result = registrationService.create(new CreateRegistrationRequest(2L, 3L, LocalDateTime.now(), 10L, 4L));

    assertEquals(6L, result.get("registrationId"));
    assertEquals(4L, result.get("slotId"));
    assertEquals(slot.getStartTime().toString(), result.get("appointmentTime"));
    assertEquals(0, slot.getRemainingCapacity());
    assertEquals("FULL", slot.getStatus());
    verify(appointmentSlotRepository).save(slot);
    verify(domainEventPublisher).publishNotification(eq(DomainEventNames.REGISTRATION_CREATED), any());
    verify(domainEventPublisher).publishAudit(eq(DomainEventNames.REGISTRATION_CREATED), any());
  }

  @Test
  void rejectsRegistrationWithoutSlotId() {
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor(2L)));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department(3L)));

    assertThrows(BusinessException.class,
        () -> registrationService.create(new CreateRegistrationRequest(2L, 3L, LocalDateTime.now(), null, null)));

    verify(appointmentSlotRepository, never()).findByIdForUpdate(any());
    verify(registrationRepository, never()).save(any(Registration.class));
  }

  @Test
  void rejectsDuplicateOrUnavailableSlot() {
    Doctor doctor = doctor(2L);
    Department department = department(3L);
    AppointmentSlot slot = slot(4L, 2L, 3L, 1, "AVAILABLE");
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
    when(appointmentSlotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
    when(registrationRepository.existsByPatientIdAndSlotIdAndStatusNot(1L, 4L, "CANCELLED")).thenReturn(true);

    assertThrows(BusinessException.class,
        () -> registrationService.create(new CreateRegistrationRequest(2L, 3L, LocalDateTime.now(), null, 4L)));

    when(registrationRepository.existsByPatientIdAndSlotIdAndStatusNot(1L, 4L, "CANCELLED")).thenReturn(false);
    slot.setStatus("FULL");
    assertThrows(BusinessException.class,
        () -> registrationService.create(new CreateRegistrationRequest(2L, 3L, LocalDateTime.now(), null, 4L)));
  }

  @Test
  void duplicateIdempotencyKeyDoesNotConsumeSlot() {
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(redisIdempotencyGuard.acquire("idem:registration:create:1:submit-1", java.time.Duration.ofSeconds(60)))
        .thenReturn(false);

    BusinessException error = assertThrows(BusinessException.class,
        () -> registrationService.create(new CreateRegistrationRequest(2L, 3L, LocalDateTime.now(), null, 4L), "submit-1"));

    assertEquals(409, error.code());
    verify(appointmentSlotRepository, never()).findByIdForUpdate(any());
    verify(appointmentSlotRepository, never()).save(any());
    verify(registrationRepository, never()).save(any(Registration.class));
  }

  @Test
  void cancelRestoresSlotCapacity() {
    Registration registration = registration(9L, 1L, 2L, 3L, 4L, "CREATED");
    AppointmentSlot slot = slot(4L, 2L, 3L, 0, "FULL");
    slot.setCapacity(2);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(registrationRepository.findById(9L)).thenReturn(Optional.of(registration));
    when(registrationRepository.save(registration)).thenReturn(registration);
    when(appointmentSlotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));

    var result = registrationService.cancel(9L);

    assertEquals("CANCELLED", result.get("status"));
    assertEquals(1, slot.getRemainingCapacity());
    assertEquals("AVAILABLE", slot.getStatus());
    verify(domainEventPublisher).publishNotification(eq(DomainEventNames.REGISTRATION_CANCELLED), any());
    verify(domainEventPublisher).publishAudit(eq(DomainEventNames.REGISTRATION_CANCELLED), any());
  }

  @Test
  void doctorCompletesOwnRegistrationAndCannotCompleteCancelled() {
    Registration registration = registration(9L, 1L, 2L, 3L, null, "CREATED");
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "doctor"));
    when(registrationRepository.findById(9L)).thenReturn(Optional.of(registration));
    when(registrationRepository.save(registration)).thenReturn(registration);

    assertEquals("COMPLETED", registrationService.complete(9L).get("status"));

    registration.setDoctorId(3L);
    assertThrows(BusinessException.class, () -> registrationService.complete(9L));
    registration.setDoctorId(2L);
    registration.setStatus("CANCELLED");
    assertThrows(BusinessException.class, () -> registrationService.complete(9L));
  }

  @Test
  void listScopesByRoleAndFiltersSlots() {
    Registration patientRegistration = registration(1L, 1L, 2L, 3L, null, "CREATED");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(registrationRepository.findByPatientId(1L)).thenReturn(List.of(patientRegistration));
    assertEquals(1, registrationService.list().size());

    when(currentUserService.get()).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "doctor"));
    when(registrationRepository.findByDoctorId(2L)).thenReturn(List.of(patientRegistration));
    assertEquals(1, registrationService.list().size());

    when(currentUserService.get()).thenReturn(new AuthenticatedUser(9L, RoleType.ADMIN, "admin"));
    when(registrationRepository.findAll()).thenReturn(List.of(patientRegistration));
    assertEquals(1, registrationService.list().size());

    when(registrationSlotQueryService.availableSlots()).thenReturn(List.of(Map.of("slotId", 4L)));

    assertEquals(1, registrationService.slots().size());
  }

  @Test
  void rejectsDoctorCancellingAnotherDoctorsRegistration() {
    Registration registration = registration(9L, 1L, 2L, 3L, null, "CREATED");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(3L, RoleType.DOCTOR, "other"));
    when(registrationRepository.findById(9L)).thenReturn(Optional.of(registration));

    assertThrows(BusinessException.class, () -> registrationService.cancel(9L));
  }

  @Test
  void cancellationIsIdempotentAndHandlesMissingSlotData() {
    Registration registration = registration(9L, 1L, 2L, 3L, null, "CANCELLED");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(registrationRepository.findById(9L)).thenReturn(Optional.of(registration));
    when(registrationRepository.save(registration)).thenReturn(registration);

    assertEquals("CANCELLED", registrationService.cancel(9L).get("status"));

    registration.setStatus("CREATED");
    assertEquals("CANCELLED", registrationService.cancel(9L).get("status"));

    registration.setStatus("CREATED");
    registration.setSlotId(10L);
    when(appointmentSlotRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
    assertEquals("CANCELLED", registrationService.cancel(9L).get("status"));
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

  private static Registration registration(Long id, Long patientId, Long doctorId, Long departmentId, Long slotId, String status) {
    Registration registration = new Registration();
    registration.setId(id);
    registration.setPatientId(patientId);
    registration.setDoctorId(doctorId);
    registration.setDepartmentId(departmentId);
    registration.setSlotId(slotId);
    registration.setAppointmentTime(LocalDateTime.now().plusHours(1));
    registration.setStatus(status);
    return registration;
  }
}

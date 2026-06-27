package com.smartcloudbrain.medicalrecord.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.medicalrecord.dto.medical.MedicalRecordSaveRequest;
import com.smartcloudbrain.medicalrecord.entity.MedicalRecord;
import com.smartcloudbrain.medicalrecord.entity.Patient;
import com.smartcloudbrain.medicalrecord.entity.Registration;
import com.smartcloudbrain.medicalrecord.repository.MedicalRecordRepository;
import com.smartcloudbrain.medicalrecord.repository.PatientRepository;
import com.smartcloudbrain.medicalrecord.repository.RegistrationRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {
  @Mock private MedicalRecordRepository medicalRecordRepository;
  @Mock private RegistrationRepository registrationRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private MedicalRecordService service;

  @BeforeEach
  void saveRecords() {
    org.mockito.Mockito.lenient().when(medicalRecordRepository.save(any())).thenAnswer(invocation -> {
      MedicalRecord record = invocation.getArgument(0);
      if (record.getId() == null) record.setId(30L);
      return record;
    });
  }

  @Test
  void savesNewAndExistingMedicalRecord() {
    Registration registration = registration();
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
    when(medicalRecordRepository.findByRegistrationId(20L)).thenReturn(Optional.empty());
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    var request = new MedicalRecordSaveRequest(20L, "胸痛", "两天", null, null, "待查", null, null);

    var saved = service.save(request);
    assertEquals(true, saved.get("aiGenerated"));
    assertEquals("", saved.get("pastHistory"));

    MedicalRecord existing = record();
    when(medicalRecordRepository.findByRegistrationId(20L)).thenReturn(Optional.of(existing));
    var updated = service.save(new MedicalRecordSaveRequest(20L, "胸痛", "两天", "无", "正常", "待查", "复查", false));
    assertEquals(false, updated.get("aiGenerated"));
  }

  @Test
  void listsRecordsForPatientDoctorAdminAndInternalPatientQuery() {
    MedicalRecord record = record();
    when(currentUserService.get())
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "患者"))
        .thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"))
        .thenReturn(new AuthenticatedUser(9L, RoleType.ADMIN, "管理员"));
    when(medicalRecordRepository.findByPatientId(eq(1L), any(Sort.class))).thenReturn(List.of(record));
    when(medicalRecordRepository.findByDoctorId(eq(2L), any(Sort.class))).thenReturn(List.of(record));
    when(medicalRecordRepository.findAll(any(Sort.class))).thenReturn(List.of(record));
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());

    assertEquals(1, service.list().size());
    assertEquals(1, service.list().size());
    assertEquals(1, service.list().size());
    assertEquals(1, service.recordsByPatient(1L).size());
  }

  @Test
  void enforcesDetailAndRegistrationOwnership() {
    MedicalRecord record = record();
    Registration registration = registration();
    when(medicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    when(currentUserService.get())
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "患者"))
        .thenReturn(new AuthenticatedUser(9L, RoleType.PATIENT, "其他患者"))
        .thenReturn(new AuthenticatedUser(3L, RoleType.DOCTOR, "其他医生"));
    assertEquals(30L, service.detail(30L).get("medicalRecordId"));
    assertThrows(BusinessException.class, () -> service.detail(30L));
    assertThrows(BusinessException.class, () -> service.detail(30L));

    when(currentUserService.require(RoleType.DOCTOR))
        .thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"))
        .thenReturn(new AuthenticatedUser(3L, RoleType.DOCTOR, "其他医生"));
    when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
    service.requireDoctorRegistration(20L);
    assertThrows(BusinessException.class, () -> service.requireDoctorRegistration(20L));
  }

  @Test
  void buildsEnrichedAiRequestFromRegistrationAndPatient() {
    Registration registration = registration();
    Patient patient = new Patient();
    patient.setName("患者");
    patient.setAge(20);
    patient.setGender("MALE");
    patient.setAllergyHistory("无");
    patient.setPastHistory("无");
    when(currentUserService.require(RoleType.DOCTOR))
        .thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"))
        .thenReturn(new AuthenticatedUser(3L, RoleType.DOCTOR, "其他医生"));
    when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    var result = service.buildGenerateRequest(new MedicalRecordGenerateRequest(20L, "CARDIOLOGY", "胸痛"));
    assertEquals("患者", result.patientName());
    assertEquals("2026-06-21T09:00", result.appointmentTime());
    assertThrows(BusinessException.class, () -> service.buildGenerateRequest(
        new MedicalRecordGenerateRequest(20L, "CARDIOLOGY", "胸痛")));
  }

  @Test
  void rejectsMissingDetailAndGenerateRegistration() {
    when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.detail(99L));
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    when(registrationRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.buildGenerateRequest(new MedicalRecordGenerateRequest(99L, null, "x")));
  }

  private Registration registration() {
    Registration registration = new Registration();
    registration.setId(20L);
    registration.setPatientId(1L);
    registration.setDoctorId(2L);
    registration.setAppointmentTime(LocalDateTime.of(2026, 6, 21, 9, 0));
    return registration;
  }

  private MedicalRecord record() {
    MedicalRecord record = new MedicalRecord();
    record.setId(30L);
    record.setRegistrationId(20L);
    record.setPatientId(1L);
    record.setDoctorId(2L);
    record.setChiefComplaint("胸痛");
    record.setDiagnosis("待查");
    return record;
  }
}

package com.smartcloudbrain.medicalrecord.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.medicalrecord.dto.medical.MedicalRecordSaveRequest;
import com.smartcloudbrain.medicalrecord.entity.MedicalRecord;
import com.smartcloudbrain.medicalrecord.entity.Registration;
import com.smartcloudbrain.medicalrecord.repository.MedicalRecordRepository;
import com.smartcloudbrain.medicalrecord.repository.PatientRepository;
import com.smartcloudbrain.medicalrecord.repository.RegistrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServicePermissionTest {

  @Mock private MedicalRecordRepository medicalRecordRepository;
  @Mock private RegistrationRepository registrationRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private MedicalRecordService medicalRecordService;

  @Test
  void doctorCannotSaveRecordForAnotherDoctorsRegistration() {
    Registration registration = new Registration();
    registration.setId(20L);
    registration.setPatientId(1L);
    registration.setDoctorId(2L);
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(1L, RoleType.DOCTOR, "doctor"));
    when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));

    MedicalRecordSaveRequest request = new MedicalRecordSaveRequest(
        20L,
        "cough",
        "low fever for two days",
        "",
        "mild throat congestion",
        "common cold",
        "rest and fluids",
        true
    );

    assertThrows(BusinessException.class, () -> medicalRecordService.save(request));
    verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
  }
}

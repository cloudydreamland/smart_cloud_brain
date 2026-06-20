package com.smartcloudbrain.patient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.patient.entity.Patient;
import com.smartcloudbrain.patient.repository.PatientRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

  @Mock private PatientRepository patientRepository;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private PatientService patientService;

  @Test
  void returnsCurrentPatientInfoWithDefaultValues() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    Map<String, Object> result = patientService.currentPatientInfo();

    assertEquals(1L, result.get("id"));
    assertEquals("Alice", result.get("name"));
    assertEquals("", result.get("gender"));
    assertEquals(0, result.get("age"));
  }

  @Test
  void returnsInternalPatientSummary() {
    Patient patient = patient(2L, "Bob");
    patient.setAge(45);
    patient.setAllergyHistory("penicillin");
    when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

    Map<String, Object> result = patientService.patientSummary(2L);

    assertEquals(45, result.get("age"));
    assertEquals("penicillin", result.get("allergyHistory"));
  }

  @Test
  void throwsWhenPatientMissing() {
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(99L, RoleType.PATIENT, "missing"));
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> patientService.currentPatientInfo());
  }

  private static Patient patient(Long id, String name) {
    Patient patient = new Patient();
    patient.setId(id);
    patient.setName(name);
    patient.setPhone("13800000000");
    return patient;
  }
}

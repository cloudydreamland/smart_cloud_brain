package com.smartcloudbrain.patient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.patient.dto.PatientProfileSaveRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorDeleteRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorSaveRequest;
import com.smartcloudbrain.patient.entity.Patient;
import com.smartcloudbrain.patient.entity.PatientVisitor;
import com.smartcloudbrain.patient.repository.PatientRepository;
import com.smartcloudbrain.patient.repository.PatientVisitorRepository;
import java.math.BigDecimal;
import java.util.List;
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
  @Mock private PatientVisitorRepository patientVisitorRepository;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private PatientService patientService;

  // ─── currentPatientInfo ────────────────────────────────────────────

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
  void returnsCurrentPatientInfoWithAllFieldsPopulated() {
    Patient patient = patient(1L, "Alice");
    patient.setGender("female");
    patient.setAge(30);
    patient.setEmail("alice@example.com");
    patient.setAllergyHistory("penicillin");
    patient.setPastHistory("asthma");
    patient.setAddress("123 Main St");
    patient.setEmergencyContact("Bob");
    patient.setEmergencyPhone("13900000000");
    patient.setBloodType("A");
    patient.setHeightCm(165);
    patient.setWeightKg(new BigDecimal("60.5"));
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    Map<String, Object> result = patientService.currentPatientInfo();

    assertEquals(1L, result.get("id"));
    assertEquals("Alice", result.get("name"));
    assertEquals("female", result.get("gender"));
    assertEquals(30, result.get("age"));
    assertEquals("alice@example.com", result.get("email"));
    assertEquals("penicillin", result.get("allergyHistory"));
    assertEquals("asthma", result.get("pastHistory"));
    assertEquals("123 Main St", result.get("address"));
    assertEquals("Bob", result.get("emergencyContact"));
    assertEquals("13900000000", result.get("emergencyPhone"));
    assertEquals("A", result.get("bloodType"));
    assertEquals(165, result.get("heightCm"));
    assertEquals(new BigDecimal("60.5"), result.get("weightKg"));
  }

  // ─── patientSummary ────────────────────────────────────────────────

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
  void patientSummaryWithDefaultValuesForNullFields() {
    Patient patient = new Patient();
    patient.setId(3L);
    patient.setName("Charlie");
    when(patientRepository.findById(3L)).thenReturn(Optional.of(patient));

    Map<String, Object> result = patientService.patientSummary(3L);

    assertEquals(3L, result.get("id"));
    assertEquals("Charlie", result.get("name"));
    assertEquals("", result.get("phone"));
    assertEquals("", result.get("email"));
    assertEquals("", result.get("gender"));
    assertEquals(0, result.get("age"));
    assertEquals("", result.get("allergyHistory"));
    assertEquals("", result.get("pastHistory"));
    assertEquals("", result.get("address"));
    assertEquals("", result.get("emergencyContact"));
    assertEquals("", result.get("emergencyPhone"));
    assertEquals("", result.get("bloodType"));
    assertEquals(0, result.get("heightCm"));
    assertEquals(0, result.get("weightKg"));
    assertEquals("", result.get("updatedAt"));
  }

  @Test
  void throwsWhenPatientMissing() {
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(99L, RoleType.PATIENT, "missing"));
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> patientService.currentPatientInfo());
  }

  @Test
  void patientSummaryThrowsWhenPatientMissing() {
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> patientService.patientSummary(99L));
  }

  // ─── saveProfile ───────────────────────────────────────────────────

  @Test
  void saveProfileUpdatesAllFields() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
    when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

    PatientProfileSaveRequest request = new PatientProfileSaveRequest(
        "Alice Updated", "female", 31, "none", "none",
        "456 Oak Ave", "Bob", "13900000001", "B", 170, new BigDecimal("65.0")
    );

    Map<String, Object> result = patientService.saveProfile(request);

    assertEquals("Alice Updated", result.get("name"));
    assertEquals("female", result.get("gender"));
    assertEquals(31, result.get("age"));
    assertEquals("none", result.get("allergyHistory"));
    assertEquals("none", result.get("pastHistory"));
    assertEquals("456 Oak Ave", result.get("address"));
    assertEquals("Bob", result.get("emergencyContact"));
    assertEquals("13900000001", result.get("emergencyPhone"));
    assertEquals("B", result.get("bloodType"));
    assertEquals(170, result.get("heightCm"));
    assertEquals(new BigDecimal("65.0"), result.get("weightKg"));
    assertNotNull(result.get("updatedAt"));
    verify(patientRepository).save(patient);
  }

  @Test
  void saveProfileThrowsWhenPatientNotFound() {
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(99L, RoleType.PATIENT, "missing"));
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    PatientProfileSaveRequest request = new PatientProfileSaveRequest(
        "Name", "male", 25, null, null, null, null, null, null, null, null
    );

    assertThrows(BusinessException.class, () -> patientService.saveProfile(request));
  }

  @Test
  void saveProfileSetsUpdatedAtTimestamp() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
    when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

    PatientProfileSaveRequest request = new PatientProfileSaveRequest(
        "Alice", "female", 30, null, null, null, null, null, null, null, null
    );

    Map<String, Object> result = patientService.saveProfile(request);

    assertNotNull(result.get("updatedAt"));
    assertFalse(result.get("updatedAt").toString().isEmpty());
  }

  // ─── visitors ──────────────────────────────────────────────────────

  @Test
  void visitorsReturnsAccountEntryPlusVisitors() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    PatientVisitor v1 = visitor(10L, 1L, "Bob", "brother");
    PatientVisitor v2 = visitor(11L, 1L, "Charlie", "friend");
    when(patientVisitorRepository.findByOwnerPatientIdOrderByIdAsc(1L))
        .thenReturn(List.of(v1, v2));

    List<Map<String, Object>> result = patientService.visitors();

    assertEquals(3, result.size());

    // First entry is the account holder
    Map<String, Object> account = result.get(0);
    assertEquals(1L, account.get("id"));
    assertEquals("Alice", account.get("name"));
    assertEquals("ACCOUNT", account.get("visitorType"));
    assertEquals("本人", account.get("relationship"));
    assertEquals(false, account.get("editable"));

    // Subsequent entries are visitors
    Map<String, Object> firstVisitor = result.get(1);
    assertEquals(10L, firstVisitor.get("id"));
    assertEquals("Bob", firstVisitor.get("name"));
    assertEquals("VISITOR", firstVisitor.get("visitorType"));
    assertEquals("brother", firstVisitor.get("relationship"));
    assertEquals(true, firstVisitor.get("editable"));

    Map<String, Object> secondVisitor = result.get(2);
    assertEquals(11L, secondVisitor.get("id"));
    assertEquals("Charlie", secondVisitor.get("name"));
  }

  @Test
  void visitorsReturnsOnlyAccountWhenNoVisitors() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
    when(patientVisitorRepository.findByOwnerPatientIdOrderByIdAsc(1L))
        .thenReturn(List.of());

    List<Map<String, Object>> result = patientService.visitors();

    assertEquals(1, result.size());
    assertEquals("ACCOUNT", result.get(0).get("visitorType"));
  }

  @Test
  void visitorsThrowsWhenPatientNotFound() {
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(99L, RoleType.PATIENT, "missing"));
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> patientService.visitors());
  }

  @Test
  void visitorViewWithAllNullFieldsShowsDefaults() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    PatientVisitor v = new PatientVisitor();
    v.setId(10L);
    v.setOwnerPatientId(1L);
    v.setName("Minimal");
    when(patientVisitorRepository.findByOwnerPatientIdOrderByIdAsc(1L))
        .thenReturn(List.of(v));

    List<Map<String, Object>> result = patientService.visitors();
    Map<String, Object> visitorMap = result.get(1);

    assertEquals(10L, visitorMap.get("id"));
    assertEquals("Minimal", visitorMap.get("name"));
    assertEquals("VISITOR", visitorMap.get("visitorType"));
    assertEquals(true, visitorMap.get("editable"));
    assertEquals("", visitorMap.get("relationship"));
    assertEquals("", visitorMap.get("phone"));
    assertEquals("", visitorMap.get("gender"));
    assertEquals(0, visitorMap.get("age"));
    assertEquals("", visitorMap.get("address"));
    assertEquals("", visitorMap.get("emergencyContact"));
    assertEquals("", visitorMap.get("emergencyPhone"));
    assertEquals("", visitorMap.get("bloodType"));
    assertEquals(0, visitorMap.get("heightCm"));
    assertEquals(0, visitorMap.get("weightKg"));
    assertEquals("", visitorMap.get("allergyHistory"));
    assertEquals("", visitorMap.get("pastHistory"));
    assertEquals("", visitorMap.get("updatedAt"));
  }

  @Test
  void visitorViewWithAllFieldsPopulated() {
    Patient patient = patient(1L, "Alice");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    PatientVisitor v = new PatientVisitor();
    v.setId(10L);
    v.setOwnerPatientId(1L);
    v.setName("Bob");
    v.setRelationship("brother");
    v.setPhone("13900000002");
    v.setGender("male");
    v.setAge(28);
    v.setAddress("789 Elm St");
    v.setEmergencyContact("Mom");
    v.setEmergencyPhone("13700000000");
    v.setBloodType("O");
    v.setHeightCm(180);
    v.setWeightKg(new BigDecimal("75.5"));
    v.setAllergyHistory("none");
    v.setPastHistory("none");
    when(patientVisitorRepository.findByOwnerPatientIdOrderByIdAsc(1L))
        .thenReturn(List.of(v));

    List<Map<String, Object>> result = patientService.visitors();
    Map<String, Object> visitorMap = result.get(1);

    assertEquals("brother", visitorMap.get("relationship"));
    assertEquals("13900000002", visitorMap.get("phone"));
    assertEquals("male", visitorMap.get("gender"));
    assertEquals(28, visitorMap.get("age"));
    assertEquals("789 Elm St", visitorMap.get("address"));
    assertEquals("Mom", visitorMap.get("emergencyContact"));
    assertEquals("13700000000", visitorMap.get("emergencyPhone"));
    assertEquals("O", visitorMap.get("bloodType"));
    assertEquals(180, visitorMap.get("heightCm"));
    assertEquals(new BigDecimal("75.5"), visitorMap.get("weightKg"));
    assertEquals("none", visitorMap.get("allergyHistory"));
    assertEquals("none", visitorMap.get("pastHistory"));
  }

  // ─── saveVisitor (new) ─────────────────────────────────────────────

  @Test
  void saveVisitorCreatesNewVisitorWhenIdIsNull() {
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientVisitorRepository.save(any(PatientVisitor.class)))
        .thenAnswer(inv -> {
          PatientVisitor v = inv.getArgument(0);
          v.setId(100L);
          return v;
        });

    PatientVisitorSaveRequest request = new PatientVisitorSaveRequest(
        null, "Bob", "brother", "13900000002", "male", 28,
        "789 Elm St", "Mom", "13700000000", "O", 180,
        new BigDecimal("75.5"), "none", "none"
    );

    Map<String, Object> result = patientService.saveVisitor(request);

    assertEquals(100L, result.get("id"));
    assertEquals("Bob", result.get("name"));
    assertEquals("VISITOR", result.get("visitorType"));
    assertEquals(true, result.get("editable"));
    assertEquals("brother", result.get("relationship"));
    assertEquals("13900000002", result.get("phone"));
    assertEquals("male", result.get("gender"));
    assertEquals(28, result.get("age"));
    assertEquals("789 Elm St", result.get("address"));
    assertEquals("Mom", result.get("emergencyContact"));
    assertEquals("13700000000", result.get("emergencyPhone"));
    assertEquals("O", result.get("bloodType"));
    assertEquals(180, result.get("heightCm"));
    assertEquals(new BigDecimal("75.5"), result.get("weightKg"));
    assertEquals("none", result.get("allergyHistory"));
    assertEquals("none", result.get("pastHistory"));
    assertNotNull(result.get("updatedAt"));
    verify(patientVisitorRepository).save(any(PatientVisitor.class));
  }

  // ─── saveVisitor (update) ──────────────────────────────────────────

  @Test
  void saveVisitorUpdatesExistingVisitorWhenIdProvided() {
    PatientVisitor existing = visitor(10L, 1L, "Old Name", "sister");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientVisitorRepository.findByIdAndOwnerPatientId(10L, 1L))
        .thenReturn(Optional.of(existing));
    when(patientVisitorRepository.save(any(PatientVisitor.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    PatientVisitorSaveRequest request = new PatientVisitorSaveRequest(
        10L, "Updated Name", "cousin", "13900000003", "female", 25,
        null, null, null, null, null, null, null, null
    );

    Map<String, Object> result = patientService.saveVisitor(request);

    assertEquals(10L, result.get("id"));
    assertEquals("Updated Name", result.get("name"));
    assertEquals("cousin", result.get("relationship"));
    assertEquals("13900000003", result.get("phone"));
    assertEquals("female", result.get("gender"));
    assertEquals(25, result.get("age"));
    verify(patientVisitorRepository).save(existing);
  }

  @Test
  void saveVisitorThrowsWhenExistingVisitorNotFound() {
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientVisitorRepository.findByIdAndOwnerPatientId(99L, 1L))
        .thenReturn(Optional.empty());

    PatientVisitorSaveRequest request = new PatientVisitorSaveRequest(
        99L, "Ghost", "nobody", null, null, null,
        null, null, null, null, null, null, null, null
    );

    assertThrows(BusinessException.class, () -> patientService.saveVisitor(request));
  }

  // ─── deleteVisitor ─────────────────────────────────────────────────

  @Test
  void deleteVisitorRemovesVisitorAndReturnsDeletedFlag() {
    PatientVisitor existing = visitor(10L, 1L, "Bob", "brother");
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientVisitorRepository.findByIdAndOwnerPatientId(10L, 1L))
        .thenReturn(Optional.of(existing));

    PatientVisitorDeleteRequest request = new PatientVisitorDeleteRequest(10L);
    Map<String, Object> result = patientService.deleteVisitor(request);

    assertEquals(true, result.get("deleted"));
    assertEquals(10L, result.get("id"));
    verify(patientVisitorRepository).delete(existing);
  }

  @Test
  void deleteVisitorThrowsWhenVisitorNotFound() {
    when(currentUserService.require(RoleType.PATIENT))
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientVisitorRepository.findByIdAndOwnerPatientId(99L, 1L))
        .thenReturn(Optional.empty());

    PatientVisitorDeleteRequest request = new PatientVisitorDeleteRequest(99L);

    assertThrows(BusinessException.class, () -> patientService.deleteVisitor(request));
  }

  // ─── patientView edge cases ────────────────────────────────────────

  @Test
  void patientViewUpdatedAtShowsTimestampWhenSet() {
    Patient patient = patient(1L, "Alice");
    patient.setUpdatedAt(java.time.LocalDateTime.of(2025, 1, 15, 10, 30, 0));
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

    Map<String, Object> result = patientService.patientSummary(1L);

    assertEquals("2025-01-15T10:30", result.get("updatedAt"));
  }

  // ─── helpers ───────────────────────────────────────────────────────

  private static Patient patient(Long id, String name) {
    Patient patient = new Patient();
    patient.setId(id);
    patient.setName(name);
    patient.setPhone("13800000000");
    return patient;
  }

  private static PatientVisitor visitor(Long id, Long ownerId, String name, String relationship) {
    PatientVisitor v = new PatientVisitor();
    v.setId(id);
    v.setOwnerPatientId(ownerId);
    v.setName(name);
    v.setRelationship(relationship);
    return v;
  }
}

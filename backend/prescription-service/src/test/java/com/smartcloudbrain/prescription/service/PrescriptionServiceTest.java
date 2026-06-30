package com.smartcloudbrain.prescription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.prescription.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.prescription.entity.Drug;
import com.smartcloudbrain.prescription.entity.MedicalRecord;
import com.smartcloudbrain.prescription.entity.Prescription;
import com.smartcloudbrain.prescription.entity.PrescriptionCheckRecord;
import com.smartcloudbrain.prescription.entity.PrescriptionItem;
import com.smartcloudbrain.prescription.event.OutboxEventPublisher;
import com.smartcloudbrain.prescription.repository.DrugRepository;
import com.smartcloudbrain.prescription.repository.MedicalRecordRepository;
import com.smartcloudbrain.prescription.repository.PatientRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionCheckRecordRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionItemRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionRepository;
import java.util.ArrayList;
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
class PrescriptionServiceTest {
  @Mock private AiGatewayService aiGatewayService;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private PrescriptionItemRepository prescriptionItemRepository;
  @Mock private PrescriptionCheckRecordRepository checkRecordRepository;
  @Mock private MedicalRecordRepository medicalRecordRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private DrugRepository drugRepository;
  @Mock private OutboxEventPublisher outboxEventPublisher;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private PrescriptionService service;

  @BeforeEach
  void saveCheckRecord() {
    org.mockito.Mockito.lenient().when(checkRecordRepository.save(any())).thenAnswer(invocation -> {
      PrescriptionCheckRecord record = invocation.getArgument(0);
      record.setId(40L);
      return record;
    });
  }

  @Test
  void checksMediumRiskWithDeterministicContextWithoutPrematureNotification() {
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    when(medicalRecordRepository.findFirstByPatientIdAndDoctorIdOrderByIdDesc(1L, 2L)).thenReturn(Optional.empty());
    when(aiGatewayService.checkPrescription(any())).thenReturn(new PrescriptionCheckResponse(
        "MEDIUM", "出血风险", null, null, null, null, true));

    var result = service.check(new PrescriptionCheckRequest(
        1L, null, null, "待查", 20, "MALE", "无", "既往史",
        List.of(new DrugItem("阿司匹林", "100mg", "每日一次", "口服", null, null))));

    assertEquals("MEDIUM", result.get("riskLevel"));
    assertEquals(List.of(), result.get("interactions"));
    assertEquals(40L, result.get("checkRecordId"));
    verify(outboxEventPublisher, never()).enqueue(any(), any(), any());
  }

  @Test
  void checksLowRiskAgainstOwnedMedicalRecordWithoutNotification() {
    MedicalRecord record = medicalRecord(30L, 1L, 2L);
    org.springframework.test.util.ReflectionTestUtils.setField(record, "pastHistory", "病历既往史");
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    when(medicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));
    when(aiGatewayService.checkPrescription(any())).thenReturn(new PrescriptionCheckResponse(
        "LOW", "低风险", "可用", List.of("无"), List.of(), List.of(), false));

    assertEquals("LOW", service.check(new PrescriptionCheckRequest(
        1L, 2L, 30L, null, null, null, null, "请求既往史",
        List.of(new DrugItem("药品", "1片", "每日一次", "口服", 3, "")))).get("riskLevel"));
  }

  @Test
  void rejectsAnotherDoctorAndInvalidMedicalRecordDuringCheck() {
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    assertThrows(BusinessException.class, () -> service.check(new PrescriptionCheckRequest(
        1L, 3L, null, null, null, null, null, null, List.of())));

    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.check(new PrescriptionCheckRequest(
        1L, 2L, 99L, null, null, null, null, null, List.of())));

    MedicalRecord other = medicalRecord(30L, 9L, 2L);
    when(medicalRecordRepository.findById(30L)).thenReturn(Optional.of(other));
    assertThrows(BusinessException.class, () -> service.check(new PrescriptionCheckRequest(
        1L, 2L, 30L, null, null, null, null, null, List.of())));
  }

  @Test
  void createsConfirmedMediumRiskPrescriptionAndItems() {
    MedicalRecord record = medicalRecord(30L, 1L, 2L);
    record.setRegistrationId(20L);
    List<PrescriptionItem> savedItems = new ArrayList<>();
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    when(medicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));
    when(prescriptionRepository.save(any())).thenAnswer(invocation -> {
      Prescription prescription = invocation.getArgument(0);
      prescription.setId(50L);
      return prescription;
    });
    when(prescriptionItemRepository.save(any())).thenAnswer(invocation -> {
      PrescriptionItem item = invocation.getArgument(0);
      savedItems.add(item);
      return item;
    });
    when(prescriptionItemRepository.findByPrescriptionId(50L)).thenAnswer(invocation -> List.copyOf(savedItems));

    var result = service.create(new PrescriptionCreateRequest(
        1L, 30L, "MEDIUM",
        List.of(new DrugItem("阿司匹林", "100mg", "每日一次", "口服", null, null))));

    assertEquals("CONFIRMED", result.get("status"));
    assertEquals(1, ((List<?>) result.get("items")).size());
    verify(outboxEventPublisher, times(2)).enqueue(any(), any(), any());
  }

  @Test
  void listsAllRolesAndChecksDetailOwnership() {
    Prescription prescription = prescription();
    when(currentUserService.get())
        .thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "患者"))
        .thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"))
        .thenReturn(new AuthenticatedUser(9L, RoleType.ADMIN, "管理员"))
        .thenReturn(new AuthenticatedUser(3L, RoleType.DOCTOR, "其他医生"));
    when(prescriptionRepository.findByPatientId(1L)).thenReturn(List.of(prescription));
    when(prescriptionRepository.findByDoctorId(2L)).thenReturn(List.of(prescription));
    when(prescriptionRepository.findAll()).thenReturn(List.of(prescription));
    when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(prescription));
    when(prescriptionItemRepository.findByPrescriptionId(50L)).thenReturn(List.of());

    assertEquals(1, service.list().size());
    assertEquals(1, service.list().size());
    assertEquals(1, service.list().size());
    assertThrows(BusinessException.class, () -> service.detail(50L));
  }

  @Test
  void rejectsMissingPrescriptionDetail() {
    when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.detail(99L));
  }

  @Test
  void doctorCanReadEnabledDrugCatalog() {
    Drug drug = drug(4L, "阿司匹林", "100mg", "活动性出血禁用", "避免与华法林联用", "ENABLED");
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(2L, RoleType.DOCTOR, "医生"));
    when(drugRepository.findByStatusIgnoreCase("ENABLED")).thenReturn(List.of(drug));

    List<Map<String, Object>> rows = service.availableDrugs();

    assertEquals(1, rows.size());
    assertEquals("阿司匹林", rows.get(0).get("name"));
    assertEquals("100mg", rows.get(0).get("specification"));
    assertEquals("ENABLED", rows.get(0).get("status"));
    verify(drugRepository).findByStatusIgnoreCase("ENABLED");
  }

  @Test
  void nonDoctorCannotReadDrugCatalog() {
    when(currentUserService.require(RoleType.DOCTOR)).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

    assertThrows(BusinessException.class, () -> service.availableDrugs());

    verify(drugRepository, never()).findByStatusIgnoreCase("ENABLED");
  }

  private MedicalRecord medicalRecord(Long id, Long patientId, Long doctorId) {
    MedicalRecord record = new MedicalRecord();
    record.setId(id);
    record.setPatientId(patientId);
    record.setDoctorId(doctorId);
    return record;
  }

  private Prescription prescription() {
    Prescription prescription = new Prescription();
    prescription.setId(50L);
    prescription.setPatientId(1L);
    prescription.setDoctorId(2L);
    prescription.setMedicalRecordId(null);
    prescription.setRegistrationId(null);
    prescription.setRiskLevel(null);
    prescription.setStatus("CONFIRMED");
    return prescription;
  }

  private Drug drug(Long id, String name, String specification, String contraindication, String interactionRule, String status) {
    Drug drug = new Drug();
    drug.setId(id);
    drug.setName(name);
    drug.setSpecification(specification);
    drug.setContraindication(contraindication);
    drug.setInteractionRule(interactionRule);
    drug.setStatus(status);
    return drug;
  }
}

package com.smartcloudbrain.prescription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.prescription.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.prescription.entity.Drug;
import com.smartcloudbrain.prescription.entity.MedicalRecord;
import com.smartcloudbrain.prescription.entity.Patient;
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
class PrescriptionServicePermissionTest {

  @Mock private AiGatewayService aiGatewayService;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private PrescriptionItemRepository prescriptionItemRepository;
  @Mock private PrescriptionCheckRecordRepository checkRecordRepository;
  @Mock private MedicalRecordRepository medicalRecordRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private DrugRepository drugRepository;
  @Mock private OutboxEventPublisher outboxEventPublisher;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private PrescriptionService prescriptionService;

  @Test
  void doctorCannotCreatePrescriptionForAnotherDoctorsRecord() {
    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setPatientId(1L);
    medicalRecord.setDoctorId(2L);
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(1L, RoleType.DOCTOR, "doctor"));
    when(medicalRecordRepository.findById(30L)).thenReturn(Optional.of(medicalRecord));

    PrescriptionCreateRequest request = new PrescriptionCreateRequest(
        1L,
        30L,
        "LOW",
        List.of(new DrugItem("acetaminophen", "0.5g", "when needed", "oral", 3, ""))
    );

    assertThrows(BusinessException.class, () -> prescriptionService.create(request));
    verify(prescriptionRepository, never()).save(any(Prescription.class));
  }

  @Test
  void patientCannotReadAnotherPatientsPrescriptionDetail() {
    Prescription prescription = new Prescription();
    prescription.setId(20L);
    prescription.setPatientId(2L);
    prescription.setDoctorId(10L);
    prescription.setMedicalRecordId(30L);
    prescription.setRiskLevel("LOW");
    prescription.setStatus("CONFIRMED");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(prescriptionRepository.findById(20L)).thenReturn(Optional.of(prescription));

    assertThrows(BusinessException.class, () -> prescriptionService.detail(20L));
  }

  @Test
  void doctorCanReadOwnPrescriptionDetailWithItems() {
    Prescription prescription = new Prescription();
    prescription.setId(20L);
    prescription.setPatientId(2L);
    prescription.setDoctorId(10L);
    prescription.setMedicalRecordId(30L);
    prescription.setRiskLevel("MEDIUM");
    prescription.setStatus("CONFIRMED");
    PrescriptionItem item = new PrescriptionItem();
    item.setPrescriptionId(20L);
    item.setDrugName("aspirin");
    item.setDosage("100mg");
    item.setFrequency("once daily");
    item.setUsageMethod("oral");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(10L, RoleType.DOCTOR, "doctor"));
    when(prescriptionRepository.findById(20L)).thenReturn(Optional.of(prescription));
    when(prescriptionItemRepository.findByPrescriptionId(20L)).thenReturn(List.of(item));

    Map<String, Object> detail = prescriptionService.detail(20L);

    assertEquals(20L, detail.get("prescriptionId"));
    assertEquals("MEDIUM", detail.get("riskLevel"));
    assertEquals(1, ((List<?>) detail.get("items")).size());
  }

  @Test
  void checkDoesNotPublishRiskNotificationBeforePrescriptionConfirmation() {
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(10L, RoleType.DOCTOR, "doctor"));
    when(patientRepository.findById(2L)).thenReturn(Optional.empty());
    when(medicalRecordRepository.findFirstByPatientIdAndDoctorIdOrderByIdDesc(2L, 10L)).thenReturn(Optional.empty());
    when(aiGatewayService.checkPrescription(any(PrescriptionCheckRequest.class)))
        .thenReturn(new PrescriptionCheckResponse("HIGH", "follow up", List.of("interaction"), false));
    when(checkRecordRepository.save(any(PrescriptionCheckRecord.class))).thenAnswer(invocation -> {
      PrescriptionCheckRecord record = invocation.getArgument(0);
      record.setId(77L);
      return record;
    });

    Map<String, Object> result = prescriptionService.check(new PrescriptionCheckRequest(2L, 10L, drugs()));

    assertEquals("HIGH", result.get("riskLevel"));
    assertEquals(77L, result.get("checkRecordId"));
    verify(outboxEventPublisher, never()).enqueue(any(), any(), any());
  }

  @Test
  void checkEnrichesDrugsWithCatalogKnowledgeBeforeAiReview() {
    Drug aspirin = new Drug();
    aspirin.setName("aspirin");
    aspirin.setSpecification("100mg tablet");
    aspirin.setContraindication("active bleeding");
    aspirin.setInteractionRule("avoid with warfarin");
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(10L, RoleType.DOCTOR, "doctor"));
    when(patientRepository.findById(2L)).thenReturn(Optional.empty());
    when(medicalRecordRepository.findFirstByPatientIdAndDoctorIdOrderByIdDesc(2L, 10L)).thenReturn(Optional.empty());
    when(drugRepository.findByStatusIgnoreCase("ENABLED")).thenReturn(List.of(aspirin));
    when(aiGatewayService.checkPrescription(any(PrescriptionCheckRequest.class)))
        .thenReturn(new PrescriptionCheckResponse("LOW", "ok", List.of(), false));
    when(checkRecordRepository.save(any(PrescriptionCheckRecord.class))).thenAnswer(invocation -> {
      PrescriptionCheckRecord record = invocation.getArgument(0);
      record.setId(78L);
      return record;
    });

    Map<String, Object> result = prescriptionService.check(new PrescriptionCheckRequest(2L, 10L, drugs()));

    ArgumentCaptor<PrescriptionCheckRequest> requestCaptor = ArgumentCaptor.forClass(PrescriptionCheckRequest.class);
    verify(aiGatewayService).checkPrescription(requestCaptor.capture());
    DrugItem enrichedDrug = requestCaptor.getValue().drugs().get(0);
    assertTrue(enrichedDrug.remark().contains("Drug catalog"));
    assertTrue(enrichedDrug.remark().contains("specification=100mg tablet"));
    assertTrue(enrichedDrug.remark().contains("contraindication=active bleeding"));
    assertEquals(1, ((List<?>) result.get("drugKnowledge")).size());
  }

  @Test
  void createPublishesRiskNotificationAfterPrescriptionConfirmation() {
    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setId(30L);
    medicalRecord.setPatientId(2L);
    medicalRecord.setDoctorId(10L);
    medicalRecord.setRegistrationId(40L);
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(10L, RoleType.DOCTOR, "doctor"));
    when(medicalRecordRepository.findById(30L)).thenReturn(Optional.of(medicalRecord));
    when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
      Prescription prescription = invocation.getArgument(0);
      prescription.setId(88L);
      return prescription;
    });

    Map<String, Object> result = prescriptionService.create(new PrescriptionCreateRequest(2L, 30L, "MEDIUM", drugs()));

    assertEquals(88L, result.get("prescriptionId"));
    verify(outboxEventPublisher).enqueue(any(), any(), any());
  }

  private static List<DrugItem> drugs() {
    return List.of(new DrugItem("aspirin", "100mg", "once daily", "oral", 3, ""));
  }
}

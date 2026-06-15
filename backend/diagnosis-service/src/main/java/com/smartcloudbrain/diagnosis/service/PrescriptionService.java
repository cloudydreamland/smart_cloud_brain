package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.diagnosis.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.diagnosis.entity.NotificationMessage;
import com.smartcloudbrain.diagnosis.entity.Prescription;
import com.smartcloudbrain.diagnosis.entity.PrescriptionCheckRecord;
import com.smartcloudbrain.diagnosis.entity.PrescriptionItem;
import com.smartcloudbrain.diagnosis.repository.NotificationMessageRepository;
import com.smartcloudbrain.diagnosis.repository.PrescriptionCheckRecordRepository;
import com.smartcloudbrain.diagnosis.repository.PrescriptionItemRepository;
import com.smartcloudbrain.diagnosis.repository.PrescriptionRepository;
import com.smartcloudbrain.diagnosis.security.CurrentUser;
import com.smartcloudbrain.diagnosis.security.CurrentUserService;
import com.smartcloudbrain.diagnosis.websocket.NotificationWebSocketHandler;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionService {

  private final AiGatewayService aiGatewayService;
  private final PrescriptionRepository prescriptionRepository;
  private final PrescriptionItemRepository prescriptionItemRepository;
  private final PrescriptionCheckRecordRepository checkRecordRepository;
  private final NotificationMessageRepository notificationRepository;
  private final NotificationWebSocketHandler notificationWebSocketHandler;
  private final CurrentUserService currentUserService;

  public PrescriptionService(
      AiGatewayService aiGatewayService,
      PrescriptionRepository prescriptionRepository,
      PrescriptionItemRepository prescriptionItemRepository,
      PrescriptionCheckRecordRepository checkRecordRepository,
      NotificationMessageRepository notificationRepository,
      NotificationWebSocketHandler notificationWebSocketHandler,
      CurrentUserService currentUserService
  ) {
    this.aiGatewayService = aiGatewayService;
    this.prescriptionRepository = prescriptionRepository;
    this.prescriptionItemRepository = prescriptionItemRepository;
    this.checkRecordRepository = checkRecordRepository;
    this.notificationRepository = notificationRepository;
    this.notificationWebSocketHandler = notificationWebSocketHandler;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public Map<String, Object> check(PrescriptionCheckRequest request) {
    CurrentUser currentUser = currentUserService.get();
    Long doctorId = request.doctorId() == null ? currentUser.userId() : request.doctorId();
    PrescriptionCheckResponse response = aiGatewayService.checkPrescription(new PrescriptionCheckRequest(request.patientId(), doctorId, request.drugs()));
    PrescriptionCheckRecord record = new PrescriptionCheckRecord();
    record.setPatientId(request.patientId());
    record.setDoctorId(doctorId);
    record.setRiskLevel(response.riskLevel());
    record.setSuggestions(response.suggestions());
    record.setInteractions(String.join("；", response.interactions()));
    record.setAiResultJson("{\"degraded\":" + response.degraded() + "}");
    checkRecordRepository.save(record);
    if ("HIGH".equals(response.riskLevel()) || "MEDIUM".equals(response.riskLevel())) {
      sendRiskNotification(doctorId, request.patientId(), null, response);
    }
    return Map.of(
        "riskLevel", response.riskLevel(),
        "suggestions", response.suggestions(),
        "interactions", response.interactions(),
        "degraded", response.degraded(),
        "checkRecordId", record.getId()
    );
  }

  @Transactional
  public Map<String, Object> create(PrescriptionCreateRequest request) {
    CurrentUser currentUser = currentUserService.get();
    Prescription prescription = new Prescription();
    prescription.setPatientId(request.patientId());
    prescription.setDoctorId(currentUser.userId());
    prescription.setMedicalRecordId(request.medicalRecordId());
    prescription.setRiskLevel(request.riskLevel() == null ? "UNREVIEWED" : request.riskLevel());
    prescription.setStatus("CONFIRMED");
    Prescription saved = prescriptionRepository.save(prescription);
    for (DrugItem drug : request.drugs()) {
      PrescriptionItem item = new PrescriptionItem();
      item.setPrescriptionId(saved.getId());
      item.setDrugName(drug.drugName());
      item.setDosage(drug.dosage());
      item.setFrequency(drug.frequency());
      item.setUsageMethod(drug.usageMethod());
      prescriptionItemRepository.save(item);
    }
    return prescriptionView(saved);
  }

  public List<Map<String, Object>> list() {
    CurrentUser currentUser = currentUserService.get();
    List<Prescription> prescriptions;
    if (currentUser.role() == RoleType.PATIENT) {
      prescriptions = prescriptionRepository.findByPatientId(currentUser.userId());
    } else if (currentUser.role() == RoleType.DOCTOR) {
      prescriptions = prescriptionRepository.findByDoctorId(currentUser.userId());
    } else {
      prescriptions = prescriptionRepository.findAll();
    }
    return prescriptions.stream().map(this::prescriptionView).toList();
  }

  private Map<String, Object> prescriptionView(Prescription prescription) {
    List<Map<String, Object>> items = prescriptionItemRepository.findByPrescriptionId(prescription.getId()).stream()
        .map(item -> Map.<String, Object>of(
            "drugName", item.getDrugName(),
            "dosage", item.getDosage(),
            "frequency", item.getFrequency(),
            "usageMethod", item.getUsageMethod()
        ))
        .toList();
    return Map.of(
        "prescriptionId", prescription.getId(),
        "patientId", prescription.getPatientId(),
        "doctorId", prescription.getDoctorId(),
        "medicalRecordId", prescription.getMedicalRecordId() == null ? 0L : prescription.getMedicalRecordId(),
        "riskLevel", prescription.getRiskLevel() == null ? "" : prescription.getRiskLevel(),
        "status", prescription.getStatus(),
        "items", items,
        "createdAt", prescription.getCreatedAt() == null ? "" : prescription.getCreatedAt().toString()
    );
  }

  private void sendRiskNotification(Long doctorId, Long patientId, Long prescriptionId, PrescriptionCheckResponse response) {
    NotificationMessage message = new NotificationMessage();
    message.setDoctorId(doctorId);
    message.setPatientId(patientId);
    message.setPrescriptionId(prescriptionId);
    message.setType("PRESCRIPTION_HIGH_RISK");
    message.setTitle("AI 处方风险提醒");
    message.setContent(response.suggestions());
    message.setRiskLevel(response.riskLevel());
    message.setReadStatus("UNREAD");
    notificationRepository.save(message);
    notificationWebSocketHandler.sendToDoctor(doctorId, """
        {"type":"PRESCRIPTION_HIGH_RISK","riskLevel":"%s","title":"AI 处方风险提醒","content":"%s"}
        """.formatted(response.riskLevel(), response.suggestions()));
  }
}

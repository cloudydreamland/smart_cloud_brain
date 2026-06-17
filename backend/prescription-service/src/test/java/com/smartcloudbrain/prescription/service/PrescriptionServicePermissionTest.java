package com.smartcloudbrain.prescription.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.prescription.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.prescription.entity.MedicalRecord;
import com.smartcloudbrain.prescription.entity.Prescription;
import com.smartcloudbrain.prescription.event.OutboxEventPublisher;
import com.smartcloudbrain.prescription.repository.MedicalRecordRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionCheckRecordRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionItemRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}

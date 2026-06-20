package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.admin.client.InternalAiClient;
import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.client.InternalTriageClient;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.entity.AiScheduleSuggestion;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.repository.AiScheduleSuggestionRepository;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.DrugRepository;
import com.smartcloudbrain.admin.repository.KnowledgeEntryRepository;
import com.smartcloudbrain.admin.repository.PromptTemplateRepository;
import com.smartcloudbrain.admin.repository.SystemDictRepository;
import com.smartcloudbrain.common.security.PasswordHashService;
import java.time.LocalDate;
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
class AdminCatalogServiceTest {

  @Mock private DepartmentRepository departmentRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private DrugRepository drugRepository;
  @Mock private PromptTemplateRepository promptTemplateRepository;
  @Mock private KnowledgeEntryRepository knowledgeEntryRepository;
  @Mock private SystemDictRepository systemDictRepository;
  @Mock private AiScheduleSuggestionRepository aiScheduleSuggestionRepository;
  @Mock private InternalDoctorClient internalDoctorClient;
  @Mock private InternalTriageClient internalTriageClient;
  @Mock private InternalAiClient internalAiClient;
  @Mock private PasswordHashService passwordHashService;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private AdminCatalogService adminCatalogService;

  @Test
  void publishScheduleDelegatesToDoctorServiceAndMarksSuggestionPublished() {
    AiScheduleSuggestion suggestion = new AiScheduleSuggestion();
    suggestion.setId(5L);
    suggestion.setDoctorId(2L);
    suggestion.setDepartmentId(3L);
    suggestion.setWorkDate(LocalDate.of(2026, 6, 17));
    suggestion.setTimeRange("09:00-12:00");
    suggestion.setCapacity(12);
    suggestion.setStatus("DRAFT");

    List<Map<String, Object>> published = List.of(Map.of(
        "id", 20L,
        "doctorId", 2L,
        "departmentId", 3L,
        "status", "PUBLISHED"
    ));
    when(aiScheduleSuggestionRepository.findAllById(List.of(5L))).thenReturn(List.of(suggestion));
    when(internalDoctorClient.publishSchedules(any())).thenReturn(published);
    when(aiScheduleSuggestionRepository.save(any(AiScheduleSuggestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

    List<Map<String, Object>> result = adminCatalogService.publishSchedule(new SchedulePublishRequest(List.of(5L)));

    assertEquals(published, result);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, Object>>> requestCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<AiScheduleSuggestion> suggestionCaptor = ArgumentCaptor.forClass(AiScheduleSuggestion.class);
    verify(internalDoctorClient).publishSchedules(requestCaptor.capture());
    verify(aiScheduleSuggestionRepository).save(suggestionCaptor.capture());

    Map<String, Object> schedule = requestCaptor.getValue().get(0);
    assertEquals(2L, schedule.get("doctorId"));
    assertEquals(3L, schedule.get("departmentId"));
    assertEquals("2026-06-17", schedule.get("workDate"));
    assertEquals("09:00-12:00", schedule.get("timeRange"));
    assertEquals(12, schedule.get("capacity"));
    assertEquals("PUBLISHED", suggestionCaptor.getValue().getStatus());
  }

  @Test
  void assignThenCloseTriageRecordThroughInternalTriageClient() {
    Doctor doctor = new Doctor();
    doctor.setId(8L);
    doctor.setName("Doctor Eight");

    when(doctorRepository.findById(8L)).thenReturn(Optional.of(doctor));
    when(internalTriageClient.assign(30L, 8L)).thenReturn(Map.of(
        "triageRecordId", 30L,
        "patientId", 1L,
        "assignedDoctorId", 8L,
        "status", "ASSIGNED"
    ));
    when(internalTriageClient.close(30L)).thenReturn(Map.of(
        "triageRecordId", 30L,
        "patientId", 1L,
        "assignedDoctorId", 8L,
        "status", "CLOSED"
    ));

    Map<String, Object> assigned = adminCatalogService.assignTriage(30L, 8L);
    Map<String, Object> closed = adminCatalogService.closeTriage(30L);

    assertEquals("ASSIGNED", assigned.get("status"));
    assertEquals(8L, assigned.get("assignedDoctorId"));
    assertEquals("Doctor Eight", assigned.get("assignedDoctorName"));
    assertEquals("CLOSED", closed.get("status"));
    assertNotNull(closed.get("assignedDoctorName"));
  }
}

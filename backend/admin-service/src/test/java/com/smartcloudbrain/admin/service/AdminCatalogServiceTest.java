package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.client.InternalAiClient;
import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.client.InternalTriageClient;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleGenerateRequest;
import com.smartcloudbrain.admin.dto.admin.AccountSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.entity.AdminUser;
import com.smartcloudbrain.admin.entity.AiScheduleSuggestion;
import com.smartcloudbrain.admin.entity.Department;
import com.smartcloudbrain.admin.entity.Drug;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.repository.AdminUserRepository;
import com.smartcloudbrain.admin.repository.AiScheduleSuggestionRepository;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.DrugRepository;
import com.smartcloudbrain.common.security.PasswordHashService;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
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
  @Mock private AiScheduleSuggestionRepository aiScheduleSuggestionRepository;
  @Mock private AdminUserRepository adminUserRepository;
  @Mock private InternalDoctorClient internalDoctorClient;
  @Mock private InternalAiClient internalAiClient;
  @Mock private InternalTriageClient internalTriageClient;
  @Mock private PasswordHashService passwordHashService;
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

  @Test
  void managesAdminAndDoctorAccountsWithRolePermissions() {
    AdminUser admin = new AdminUser();
    admin.setId(1L);
    admin.setUsername("admin2");
    admin.setName("Admin Two");
    admin.setStatus("ENABLED");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("Doctor Two");
    doctor.setPhone("13800000002");
    doctor.setDepartmentId(3L);
    doctor.setStatus("ENABLED");
    Department department = new Department();
    department.setId(3L);
    department.setName("Cardiology");

    when(adminUserRepository.findAll()).thenReturn(List.of(admin));
    when(doctorRepository.findAll()).thenReturn(List.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

    List<Map<String, Object>> accounts = adminCatalogService.accounts();

    assertEquals(2, accounts.size());
    assertEquals("ADMIN", accounts.get(0).get("role"));
    assertEquals("DOCTOR", accounts.get(1).get("role"));
    assertEquals(3, adminCatalogService.roles().size());

    when(adminUserRepository.findByUsername("newadmin")).thenReturn(Optional.empty());
    when(passwordHashService.encode("123456")).thenReturn("{bcrypt}hash");
    when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> {
      AdminUser value = invocation.getArgument(0);
      value.setId(9L);
      return value;
    });

    Map<String, Object> saved = adminCatalogService.saveAccount(new AccountSaveRequest(
        null, "ADMIN", "newadmin", "New Admin", "123456", null, null, null, "ENABLED"));

    assertEquals("ADMIN", saved.get("role"));
    assertEquals("newadmin", saved.get("account"));
  }

  @Test
  void generatesValidatedAiScheduleWithProviderState() {
    Department department = new Department();
    department.setId(3L);
    department.setCode("CARDIOLOGY");
    department.setName("心内科");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("王医生");
    doctor.setDepartmentId(3L);
    doctor.setStatus("ENABLED");
    List<AiScheduleSuggestion> saved = new ArrayList<>();

    when(departmentRepository.findAll()).thenReturn(List.of(department));
    when(doctorRepository.findAll()).thenReturn(List.of(doctor));
    when(internalDoctorClient.schedules()).thenReturn(List.of());
    when(internalAiClient.suggestSchedule(any())).thenReturn(new ScheduleSuggestResponse(
        List.of(new ScheduleSuggestionItem(2L, 3L, LocalDate.of(2026, 6, 21),
            "09:00-12:00", 16, "胸痛就诊需求较高")),
        "dify",
        false
    ));
    when(aiScheduleSuggestionRepository.findByStatusOrderByWorkDateAscDoctorIdAsc("DRAFT"))
        .thenAnswer(invocation -> List.copyOf(saved));
    when(aiScheduleSuggestionRepository.save(any(AiScheduleSuggestion.class))).thenAnswer(invocation -> {
      AiScheduleSuggestion value = invocation.getArgument(0);
      value.setId(9L);
      saved.add(value);
      return value;
    });

    List<Map<String, Object>> result = adminCatalogService.generateScheduleSuggestions(
        new ScheduleGenerateRequest(LocalDate.of(2026, 6, 21), 2));

    assertEquals(1, result.size());
    assertEquals("dify", result.get(0).get("source"));
    assertEquals(false, result.get(0).get("degraded"));
    assertEquals(16, result.get(0).get("capacity"));
  }

  @Test
  void rejectsAiScheduleThatConflictsWithExistingSlot() {
    Department department = new Department();
    department.setId(3L);
    department.setCode("CARDIOLOGY");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setDepartmentId(3L);
    doctor.setStatus("ENABLED");
    when(departmentRepository.findAll()).thenReturn(List.of(department));
    when(doctorRepository.findAll()).thenReturn(List.of(doctor));
    when(internalDoctorClient.schedules()).thenReturn(List.of(Map.of(
        "doctorId", 2L, "workDate", "2026-06-21", "timeRange", "09:00-12:00")));
    when(internalAiClient.suggestSchedule(any())).thenReturn(new ScheduleSuggestResponse(
        List.of(new ScheduleSuggestionItem(2L, 3L, LocalDate.of(2026, 6, 21),
            "09:00-12:00", 12, "重复建议")),
        "dify",
        false
    ));

    assertThrows(BusinessException.class, () -> adminCatalogService.generateScheduleSuggestions(
        new ScheduleGenerateRequest(LocalDate.of(2026, 6, 21), 1)));
  }

  @Test
  void coversCatalogCrudSearchAndInternalDelegates() {
    Department department = new Department();
    department.setId(3L);
    department.setCode("CARDIOLOGY");
    department.setName("心内科");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("王医生");
    doctor.setDepartmentId(3L);
    doctor.setStatus("ENABLED");
    Drug drug = new Drug();
    drug.setId(4L);
    drug.setName("阿司匹林");
    drug.setSpecification("100mg");
    drug.setStatus("ENABLED");

    when(departmentRepository.findAll()).thenReturn(List.of(department));
    when(departmentRepository.findByCode("GENERAL")).thenReturn(Optional.empty());
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
    when(departmentRepository.save(any())).thenAnswer(invocation -> {
      Department value = invocation.getArgument(0);
      if (value.getId() == null) value.setId(7L);
      return value;
    });
    when(passwordHashService.encode(any())).thenReturn("hash");
    when(doctorRepository.save(any())).thenAnswer(invocation -> {
      Doctor value = invocation.getArgument(0);
      if (value.getId() == null) value.setId(8L);
      return value;
    });
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    lenient().when(drugRepository.findAll()).thenReturn(List.of(drug));
    when(internalDoctorClient.schedules()).thenReturn(List.of(Map.of("id", 1L)));
    when(internalTriageClient.list()).thenReturn(List.of(Map.of("triageRecordId", 1L, "patientId", 1L, "status", "PENDING")));
    when(internalTriageClient.detail(1L)).thenReturn(Map.of("triageRecordId", 1L, "patientId", 1L, "status", "PENDING"));

    assertEquals(1, adminCatalogService.departments().size());
    assertEquals("GENERAL", adminCatalogService.saveDepartment(
        new DepartmentSaveRequest(null, "GENERAL", "全科", null)).get("code"));
    assertEquals("CARDIOLOGY", adminCatalogService.saveDepartment(
        new DepartmentSaveRequest(3L, "CARDIOLOGY", "心内科", "说明")).get("code"));
    assertEquals("王医生", adminCatalogService.saveDoctor(
        new DoctorSaveRequest(2L, "王医生", "1", "new", 3L, "主任", "胸痛", null)).get("name"));
    assertEquals("新医生", adminCatalogService.saveDoctor(
        new DoctorSaveRequest(null, "新医生", "2", "", 3L, null, null, null)).get("name"));
    drug.setContraindication("活动性出血禁用");
    assertEquals(1, adminCatalogService.schedules().size());
    assertEquals(1, adminCatalogService.triageDesk().size());
    assertEquals(1L, adminCatalogService.triageDetail(1L).get("triageRecordId"));
  }

  @Test
  void rejectsEveryInvalidAiScheduleShapeAndCoversDraftDelegates() {
    Department department = new Department();
    department.setId(3L);
    department.setCode("CARDIOLOGY");
    department.setName("心内科");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("医生");
    doctor.setDepartmentId(3L);
    doctor.setStatus("ENABLED");
    LocalDate start = LocalDate.of(2026, 6, 21);
    when(departmentRepository.findAll()).thenReturn(List.of(department));
    when(doctorRepository.findAll()).thenReturn(List.of()).thenReturn(List.of(doctor));
    when(internalDoctorClient.schedules()).thenReturn(List.of(
        Map.of("doctorId", 2L),
        Map.of("doctorId", 2L, "workDate", "bad-date", "timeRange", "09:00-12:00")));

    assertThrows(BusinessException.class, () -> adminCatalogService.generateScheduleSuggestions(
        new ScheduleGenerateRequest(start, 1)));

    when(internalAiClient.suggestSchedule(any())).thenReturn(
        new ScheduleSuggestResponse(List.of(), "dify", false),
        new ScheduleSuggestResponse(List.of(new ScheduleSuggestionItem(9L, 3L, start, "09:00-12:00", 12, "x")), "dify", false),
        new ScheduleSuggestResponse(List.of(new ScheduleSuggestionItem(2L, 3L, start.plusDays(2), "09:00-12:00", 12, "x")), "dify", false),
        new ScheduleSuggestResponse(List.of(new ScheduleSuggestionItem(2L, 3L, start, "09:00-12:00", 0, "x")), "dify", false),
        new ScheduleSuggestResponse(List.of(new ScheduleSuggestionItem(2L, 3L, start, "bad", 12, "x")), "dify", false),
        new ScheduleSuggestResponse(List.of(new ScheduleSuggestionItem(2L, 3L, start, "12:00-09:00", 12, "x")), "dify", false)
    );
    for (int i = 0; i < 6; i++) {
      assertThrows(BusinessException.class, () -> adminCatalogService.generateScheduleSuggestions(
          new ScheduleGenerateRequest(start, 1)));
    }

    AiScheduleSuggestion suggestion = new AiScheduleSuggestion();
    suggestion.setId(5L);
    suggestion.setDoctorId(null);
    suggestion.setDepartmentId(null);
    suggestion.setWorkDate(start);
    suggestion.setTimeRange("09:00-12:00");
    suggestion.setCapacity(12);
    suggestion.setStatus("DRAFT");
    when(aiScheduleSuggestionRepository.findById(5L)).thenReturn(Optional.of(suggestion));
    assertEquals("", adminCatalogService.scheduleSuggestionDetail(5L).get("doctorName"));

    AiScheduleSuggestion publishable = new AiScheduleSuggestion();
    publishable.setId(6L);
    publishable.setDoctorId(2L);
    publishable.setDepartmentId(3L);
    publishable.setWorkDate(start);
    publishable.setTimeRange("09:00-12:00");
    publishable.setCapacity(12);
    publishable.setStatus("DRAFT");
    when(aiScheduleSuggestionRepository.findByStatusOrderByWorkDateAscDoctorIdAsc("DRAFT"))
        .thenReturn(List.of(publishable));
    when(internalDoctorClient.publishSchedules(any())).thenReturn(List.of(Map.of("id", 1L)));
    assertEquals(1, adminCatalogService.publishSchedule(new SchedulePublishRequest(List.of())).size());
  }
}

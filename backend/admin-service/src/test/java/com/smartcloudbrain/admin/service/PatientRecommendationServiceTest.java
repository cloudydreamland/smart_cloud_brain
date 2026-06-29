package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSortRequest;
import com.smartcloudbrain.admin.entity.Department;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.entity.PatientRecommendation;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.PatientRecommendationRepository;
import com.smartcloudbrain.common.exception.BusinessException;
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
class PatientRecommendationServiceTest {

  @Mock private PatientRecommendationRepository repository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private DoctorRepository doctorRepository;
  @InjectMocks private PatientRecommendationService service;

  @Test
  void publicDepartmentListFiltersMissingTargets() {
    PatientRecommendation valid = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    PatientRecommendation missing = recommendation(2L, "DEPARTMENT", 99L, "ENABLED", false);
    Department department = department(10L, "心内科", "胸痛与慢病管理");

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DEPARTMENT", "ENABLED"))
        .thenReturn(List.of(valid, missing));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
    when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

    List<Map<String, Object>> rows = service.publicList("DEPARTMENT");

    assertEquals(1, rows.size());
    assertEquals("心内科", rows.get(0).get("targetName"));
    assertEquals("胸痛与慢病管理", rows.get(0).get("description"));
  }

  @Test
  void publicDoctorListFiltersDisabledDoctors() {
    PatientRecommendation enabled = recommendation(1L, "DOCTOR", 11L, "ENABLED", false);
    PatientRecommendation disabled = recommendation(2L, "DOCTOR", 12L, "ENABLED", false);
    Doctor doctor = doctor(11L, "王医生", "ENABLED");
    Doctor disabledDoctor = doctor(12L, "李医生", "DISABLED");

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DOCTOR", "ENABLED"))
        .thenReturn(List.of(enabled, disabled));
    when(doctorRepository.findById(11L)).thenReturn(Optional.of(doctor));
    when(doctorRepository.findById(12L)).thenReturn(Optional.of(disabledDoctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department(3L, "神经内科", "神经系统疾病")));

    List<Map<String, Object>> rows = service.publicList("doctor");

    assertEquals(1, rows.size());
    assertEquals("王医生", rows.get(0).get("targetName"));
    assertEquals("神经内科", rows.get(0).get("departmentName"));
  }

  @Test
  void saveRejectsUnknownTypeAndMissingTarget() {
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "BAD", 1L, "", "", "", "", 0, "ENABLED"), 7L));
    when(departmentRepository.findById(404L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "DEPARTMENT", 404L, "", "", "", "", 0, "ENABLED"), 7L));
  }

  @Test
  void saveNormalizesTypeStatusAndAuditFields() {
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "胸痛")));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> {
      PatientRecommendation recommendation = invocation.getArgument(0);
      recommendation.setId(5L);
      return recommendation;
    });

    Map<String, Object> row = service.save(new PatientRecommendationSaveRequest(
        null, "department", 10L, " 首页展示 ", "", "", "", null, null), 8L);

    assertEquals(5L, row.get("id"));
    assertEquals("DEPARTMENT", row.get("recommendType"));
    assertEquals("首页展示", row.get("title"));
    assertEquals("ENABLED", row.get("status"));
    assertEquals(8L, row.get("createdBy"));
    assertEquals(8L, row.get("updatedBy"));
  }

  @Test
  void sortRejectsEmptyListAndCrossTypeRows() {
    assertThrows(BusinessException.class, () -> service.sort(new PatientRecommendationSortRequest(List.of()), 1L));

    PatientRecommendation department = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    PatientRecommendation doctor = recommendation(2L, "DOCTOR", 11L, "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(department));
    when(repository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> service.sort(new PatientRecommendationSortRequest(List.of(
        new PatientRecommendationSortRequest.SortItem(1L, 10),
        new PatientRecommendationSortRequest.SortItem(2L, 20)
    )), 9L));
  }

  @Test
  void deleteMarksRecommendationDeleted() {
    PatientRecommendation recommendation = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(recommendation));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));

    service.delete(1L, 6L);

    ArgumentCaptor<PatientRecommendation> captor = ArgumentCaptor.forClass(PatientRecommendation.class);
    verify(repository).save(captor.capture());
    assertEquals(true, captor.getValue().getDeleted());
    assertEquals(6L, captor.getValue().getUpdatedBy());
  }

  private PatientRecommendation recommendation(Long id, String type, Long targetId, String status, boolean deleted) {
    PatientRecommendation recommendation = new PatientRecommendation();
    recommendation.setId(id);
    recommendation.setRecommendType(type);
    recommendation.setTargetId(targetId);
    recommendation.setStatus(status);
    recommendation.setDeleted(deleted);
    recommendation.setSort(0);
    return recommendation;
  }

  private Department department(Long id, String name, String description) {
    Department department = new Department();
    department.setId(id);
    department.setName(name);
    department.setDescription(description);
    return department;
  }

  private Doctor doctor(Long id, String name, String status) {
    Doctor doctor = new Doctor();
    doctor.setId(id);
    doctor.setName(name);
    doctor.setDepartmentId(3L);
    doctor.setTitle("主任医师");
    doctor.setSpecialty("头痛眩晕");
    doctor.setStatus(status);
    return doctor;
  }
}

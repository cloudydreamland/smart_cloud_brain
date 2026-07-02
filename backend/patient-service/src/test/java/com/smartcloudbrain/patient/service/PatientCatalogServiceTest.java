package com.smartcloudbrain.patient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.patient.entity.Department;
import com.smartcloudbrain.patient.entity.Doctor;
import com.smartcloudbrain.patient.repository.DepartmentRepository;
import com.smartcloudbrain.patient.repository.DoctorRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientCatalogServiceTest {

  @Mock private DepartmentRepository departmentRepository;
  @Mock private DoctorRepository doctorRepository;
  @InjectMocks private PatientCatalogService patientCatalogService;

  // ─── departments ───────────────────────────────────────────────────

  @Test
  void departmentsReturnsAllDepartments() {
    Department d1 = department(1L, "ICU", "Intensive Care", "Critical care unit");
    Department d2 = department(2L, "PED", "Pediatrics", "Children care");
    when(departmentRepository.findAll()).thenReturn(List.of(d1, d2));

    List<Map<String, Object>> result = patientCatalogService.departments();

    assertEquals(2, result.size());
    assertEquals(1L, result.get(0).get("id"));
    assertEquals("ICU", result.get(0).get("code"));
    assertEquals("Intensive Care", result.get(0).get("name"));
    assertEquals("Critical care unit", result.get(0).get("description"));
    assertEquals(2L, result.get(1).get("id"));
    assertEquals("PED", result.get(1).get("code"));
  }

  @Test
  void departmentsReturnsEmptyListWhenNoDepartments() {
    when(departmentRepository.findAll()).thenReturn(List.of());

    List<Map<String, Object>> result = patientCatalogService.departments();

    assertTrue(result.isEmpty());
  }

  @Test
  void departmentsHandlesNullFields() {
    Department d = new Department();
    d.setId(1L);
    when(departmentRepository.findAll()).thenReturn(List.of(d));

    List<Map<String, Object>> result = patientCatalogService.departments();

    assertEquals(1, result.size());
    assertEquals("", result.get(0).get("code"));
    assertEquals("", result.get(0).get("name"));
    assertEquals("", result.get(0).get("description"));
  }

  // ─── doctors (with departmentId) ───────────────────────────────────

  @Test
  void doctorsByDepartmentReturnsFilteredDoctors() {
    Department dept = department(1L, "ICU", "Intensive Care", "desc");
    Doctor doc = doctor(10L, "Dr. Smith", "13900000001", 1L, "Chief", "Cardiology", "ENABLED");
    when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of(doc));
    when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

    List<Map<String, Object>> result = patientCatalogService.doctors(1L);

    assertEquals(1, result.size());
    Map<String, Object> view = result.get(0);
    assertEquals(10L, view.get("id"));
    assertEquals("Dr. Smith", view.get("name"));
    assertEquals("13900000001", view.get("phone"));
    assertEquals(1L, view.get("departmentId"));
    assertEquals("ICU", view.get("departmentCode"));
    assertEquals("Intensive Care", view.get("departmentName"));
    assertEquals("Chief", view.get("title"));
    assertEquals("Cardiology", view.get("specialty"));
    assertEquals("ENABLED", view.get("status"));
  }

  @Test
  void doctorsByDepartmentReturnsEmptyWhenNoneFound() {
    when(doctorRepository.findByDepartmentId(99L)).thenReturn(List.of());

    List<Map<String, Object>> result = patientCatalogService.doctors(99L);

    assertTrue(result.isEmpty());
  }

  // ─── doctors (null departmentId = all) ─────────────────────────────

  @Test
  void doctorsNullDepartmentIdReturnsAllDoctors() {
    Department dept = department(1L, "ICU", "Intensive Care", "desc");
    Doctor doc1 = doctor(10L, "Dr. Smith", "13900000001", 1L, "Chief", "Cardiology", "ENABLED");
    Doctor doc2 = doctor(11L, "Dr. Jones", "13900000002", 1L, "Attending", "Neurology", "ENABLED");
    when(doctorRepository.findAll()).thenReturn(List.of(doc1, doc2));
    when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

    List<Map<String, Object>> result = patientCatalogService.doctors(null);

    assertEquals(2, result.size());
    assertEquals("Dr. Smith", result.get(0).get("name"));
    assertEquals("Dr. Jones", result.get(1).get("name"));
  }

  @Test
  void doctorViewWithNullDepartmentIdShowsDefaults() {
    Doctor doc = doctor(10L, "Dr. Smith", "13900000001", null, "Chief", "Cardiology", "ENABLED");
    when(doctorRepository.findAll()).thenReturn(List.of(doc));

    List<Map<String, Object>> result = patientCatalogService.doctors(null);

    assertEquals(1, result.size());
    Map<String, Object> view = result.get(0);
    assertEquals(0L, view.get("departmentId"));
    assertEquals("", view.get("departmentCode"));
    assertEquals("", view.get("departmentName"));
  }

  @Test
  void doctorViewWithDepartmentNotFoundShowsDefaults() {
    Doctor doc = doctor(10L, "Dr. Smith", "13900000001", 99L, "Chief", "Cardiology", "ENABLED");
    when(doctorRepository.findByDepartmentId(99L)).thenReturn(List.of(doc));
    when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

    List<Map<String, Object>> result = patientCatalogService.doctors(99L);

    assertEquals(1, result.size());
    Map<String, Object> view = result.get(0);
    assertEquals(99L, view.get("departmentId"));
    assertEquals("", view.get("departmentCode"));
    assertEquals("", view.get("departmentName"));
  }

  @Test
  void doctorViewWithNullFieldsShowsDefaults() {
    Doctor doc = new Doctor();
    doc.setId(10L);
    doc.setName("Dr. Minimal");
    when(doctorRepository.findAll()).thenReturn(List.of(doc));

    List<Map<String, Object>> result = patientCatalogService.doctors(null);

    assertEquals(1, result.size());
    Map<String, Object> view = result.get(0);
    assertEquals(10L, view.get("id"));
    assertEquals("Dr. Minimal", view.get("name"));
    assertEquals("", view.get("phone"));
    assertEquals(0L, view.get("departmentId"));
    assertEquals("", view.get("departmentCode"));
    assertEquals("", view.get("departmentName"));
    assertEquals("", view.get("title"));
    assertEquals("", view.get("specialty"));
    assertEquals("ENABLED", view.get("status"));
  }

  @Test
  void doctorViewWithNullStatusFallsBackToEnabled() {
    Doctor doc = new Doctor();
    doc.setId(10L);
    doc.setName("Dr. Test");
    doc.setStatus(null);
    when(doctorRepository.findAll()).thenReturn(List.of(doc));

    List<Map<String, Object>> result = patientCatalogService.doctors(null);

    assertEquals("ENABLED", result.get(0).get("status"));
  }

  // ─── helpers ───────────────────────────────────────────────────────

  private static Department department(Long id, String code, String name, String desc) {
    Department d = new Department();
    d.setId(id);
    d.setCode(code);
    d.setName(name);
    d.setDescription(desc);
    return d;
  }

  private static Doctor doctor(Long id, String name, String phone, Long deptId,
      String title, String specialty, String status) {
    Doctor d = new Doctor();
    d.setId(id);
    d.setName(name);
    d.setPhone(phone);
    d.setDepartmentId(deptId);
    d.setTitle(title);
    d.setSpecialty(specialty);
    d.setStatus(status);
    return d;
  }
}

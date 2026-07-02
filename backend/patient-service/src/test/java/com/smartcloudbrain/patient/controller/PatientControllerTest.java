package com.smartcloudbrain.patient.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.patient.dto.PatientProfileSaveRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorDeleteRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorSaveRequest;
import com.smartcloudbrain.patient.service.PatientCatalogService;
import com.smartcloudbrain.patient.service.PatientService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

  @Mock private PatientService patientService;
  @Mock private PatientCatalogService patientCatalogService;

  private PatientController controller;

  @BeforeEach
  void setUp() {
    controller = new PatientController(patientService, patientCatalogService);
  }

  @Test
  void info_returnsSuccess() {
    Map<String, Object> expected = Map.of("id", 1, "name", "张三", "phone", "13800000001");
    when(patientService.currentPatientInfo()).thenReturn(expected);

    Result<?> result = controller.info();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientService).currentPatientInfo();
  }

  @Test
  void saveProfile_returnsSuccess() {
    PatientProfileSaveRequest request = new PatientProfileSaveRequest(
        "张三", "男", 30, "无", "无", "北京市", "李四", "13900000002", "A", 175, null
    );
    Map<String, Object> expected = Map.of("id", 1, "name", "张三", "saved", true);
    when(patientService.saveProfile(any(PatientProfileSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveProfile(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientService).saveProfile(request);
  }

  @Test
  void visitors_returnsSuccess() {
    List<Map<String, Object>> expected = List.of(
        Map.of("id", 1, "name", "李四", "relationship", "家属"),
        Map.of("id", 2, "name", "王五", "relationship", "朋友")
    );
    when(patientService.visitors()).thenReturn(expected);

    Result<?> result = controller.visitors();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientService).visitors();
  }

  @Test
  void saveVisitor_returnsSuccess() {
    PatientVisitorSaveRequest request = new PatientVisitorSaveRequest(
        null, "李四", "家属", "13900000002", "男", 28, null, null, null, null, null, null, null, null
    );
    Map<String, Object> expected = Map.of("id", 1, "name", "李四", "saved", true);
    when(patientService.saveVisitor(any(PatientVisitorSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveVisitor(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientService).saveVisitor(request);
  }

  @Test
  void deleteVisitor_returnsSuccess() {
    PatientVisitorDeleteRequest request = new PatientVisitorDeleteRequest(1L);
    Map<String, Object> expected = Map.of("deleted", true);
    when(patientService.deleteVisitor(any(PatientVisitorDeleteRequest.class))).thenReturn(expected);

    Result<?> result = controller.deleteVisitor(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientService).deleteVisitor(request);
  }

  @Test
  void departments_returnsSuccess() {
    List<Map<String, Object>> expected = List.of(
        Map.of("id", 1, "name", "内科"),
        Map.of("id", 2, "name", "外科")
    );
    when(patientCatalogService.departments()).thenReturn(expected);

    Result<?> result = controller.departments();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientCatalogService).departments();
  }

  @Test
  void doctors_withDepartmentId_returnsSuccess() {
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "name", "张医生"));
    when(patientCatalogService.doctors(1L)).thenReturn(expected);

    Result<?> result = controller.doctors(1L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientCatalogService).doctors(1L);
  }

  @Test
  void doctors_nullDepartmentId_returnsSuccess() {
    List<Map<String, Object>> expected = List.of(
        Map.of("id", 1, "name", "张医生"),
        Map.of("id", 2, "name", "李医生")
    );
    when(patientCatalogService.doctors(null)).thenReturn(expected);

    Result<?> result = controller.doctors(null);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(patientCatalogService).doctors(null);
  }
}

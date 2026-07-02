package com.smartcloudbrain.medicalrecord.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.medicalrecord.dto.medical.MedicalRecordSaveRequest;
import com.smartcloudbrain.medicalrecord.service.AiGatewayService;
import com.smartcloudbrain.medicalrecord.service.MedicalRecordService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedicalRecordControllerTest {

  private AiGatewayService aiGatewayService;
  private MedicalRecordService medicalRecordService;
  private MedicalRecordController controller;

  @BeforeEach
  void setUp() {
    aiGatewayService = mock(AiGatewayService.class);
    medicalRecordService = mock(MedicalRecordService.class);
    controller = new MedicalRecordController(aiGatewayService, medicalRecordService);
  }

  @Test
  void save_returnsSuccess() {
    MedicalRecordSaveRequest request = new MedicalRecordSaveRequest(
        1L, "头痛", "持续性头痛", null, null, "偏头痛", "休息", true);
    Map<String, Object> expected = Map.of("medicalRecordId", 10L, "diagnosis", "偏头痛");
    when(medicalRecordService.save(any(MedicalRecordSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.save(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(medicalRecordService).save(request);
  }

  @Test
  void list_returnsSuccess() {
    List<Map<String, Object>> expected = List.of(
        Map.of("medicalRecordId", 1L, "diagnosis", "感冒"),
        Map.of("medicalRecordId", 2L, "diagnosis", "发烧")
    );
    when(medicalRecordService.list()).thenReturn(expected);

    Result<?> result = controller.list();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(medicalRecordService).list();
  }

  @Test
  void detail_returnsSuccess() {
    Map<String, Object> expected = Map.of("medicalRecordId", 5L, "diagnosis", "高血压");
    when(medicalRecordService.detail(5L)).thenReturn(expected);

    Result<?> result = controller.detail(5L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(medicalRecordService).detail(5L);
  }

  @Test
  void generate_returnsSuccess() {
    MedicalRecordGenerateRequest rawRequest = new MedicalRecordGenerateRequest(1L, null, "头痛三天");
    MedicalRecordGenerateRequest enrichedRequest = new MedicalRecordGenerateRequest(
        1L, null, "头痛三天", 100L, "张三", 30, "男", "", "", 2L, "", "");
    MedicalRecordGenerateResponse response = new MedicalRecordGenerateResponse(
        "头痛", "持续性头痛", "无", "正常", "偏头痛", "休息", false);
    when(medicalRecordService.buildGenerateRequest(any(MedicalRecordGenerateRequest.class)))
        .thenReturn(enrichedRequest);
    when(aiGatewayService.generateMedicalRecord(enrichedRequest)).thenReturn(response);

    Result<?> result = controller.generate(rawRequest);

    assertEquals(0, result.code());
    assertNotNull(result.data());
    verify(medicalRecordService).buildGenerateRequest(rawRequest);
    verify(aiGatewayService).generateMedicalRecord(enrichedRequest);
  }

  @Test
  void generateStream_returnsEmitter() {
    MedicalRecordGenerateRequest rawRequest = new MedicalRecordGenerateRequest(1L, "dept", "头痛");
    MedicalRecordGenerateRequest enrichedRequest = new MedicalRecordGenerateRequest(
        1L, "dept", "头痛", 100L, "张三", 30, "男", "", "", 2L, "", "");
    MedicalRecordGenerateResponse streamResponse = new MedicalRecordGenerateResponse(
        "头痛", "持续性头痛", "无", "正常", "偏头痛", "休息", false);
    when(medicalRecordService.buildGenerateRequest(any(MedicalRecordGenerateRequest.class)))
        .thenReturn(enrichedRequest);
    when(aiGatewayService.generateMedicalRecord(enrichedRequest)).thenReturn(streamResponse);

    assertNotNull(controller.generateStream(1L, "头痛", "dept"));
    verify(medicalRecordService).buildGenerateRequest(any(MedicalRecordGenerateRequest.class));
  }
}

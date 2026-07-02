package com.smartcloudbrain.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DoctorWorkspaceServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @InjectMocks private DoctorWorkspaceService service;

  @Test
  void dashboardReturnsAllExpectedKeys() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyLong()))
        .thenReturn(5L, 3L, 10L, 2L);
    doReturn(List.of()).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    Map<String, Object> result = service.dashboard(1L);

    assertNotNull(result.get("todayRegistrations"));
    assertNotNull(result.get("pendingRegistrations"));
    assertNotNull(result.get("completedRegistrations"));
    assertNotNull(result.get("upcomingSchedules"));
    assertNotNull(result.get("queue"));
    assertNotNull(result.get("schedules"));
  }

  @Test
  void dashboardHandlesNullCountGracefully() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyLong()))
        .thenReturn(null);
    doReturn(List.of()).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    Map<String, Object> result = service.dashboard(1L);

    assertEquals(0L, result.get("todayRegistrations"));
    assertEquals(0L, result.get("pendingRegistrations"));
    assertEquals(0L, result.get("completedRegistrations"));
    assertEquals(0L, result.get("upcomingSchedules"));
  }

  @Test
  void queueReturnsListFromJdbcTemplate() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("registrationId", 1L);
    row.put("patientName", "Alice");
    row.put("status", "CREATED");
    doReturn(List.of(row)).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    List<Map<String, Object>> result = service.queue(1L);

    assertEquals(1, result.size());
    assertEquals(1L, result.get(0).get("registrationId"));
  }

  @Test
  void dashboardAggregatesQueueAndSchedules() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyLong()))
        .thenReturn(1L, 0L, 0L, 1L);

    Map<String, Object> queueRow = new LinkedHashMap<>();
    queueRow.put("registrationId", 42L);
    queueRow.put("patientName", "Bob");

    Map<String, Object> scheduleRow = new LinkedHashMap<>();
    scheduleRow.put("id", 7L);
    scheduleRow.put("workDate", "2026-07-01");

    doReturn(List.of(queueRow), List.of(scheduleRow))
        .when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    Map<String, Object> result = service.dashboard(1L);

    assertEquals(1L, result.get("todayRegistrations"));
    assertEquals(1, ((List<?>) result.get("queue")).size());
    assertEquals(1, ((List<?>) result.get("schedules")).size());
  }
}

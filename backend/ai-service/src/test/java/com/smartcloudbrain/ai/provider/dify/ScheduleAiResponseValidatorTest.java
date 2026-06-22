package com.smartcloudbrain.ai.provider.dify;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartcloudbrain.aiapi.dto.ExistingSchedule;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleDepartmentCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleAiResponseValidatorTest {

  private static final LocalDate START = LocalDate.of(2026, 6, 21);

  @Test
  void addsExactAllowedDatesAndDoctorPairsToPrompt() {
    String value = ScheduleAiResponseValidator.constrainedPrompt(
        new PromptResolveResponse(1L, "SCHEDULE", "GENERAL", "name", "base prompt", "{}", "v1"),
        request(1L, 1L, List.of())
    );

    assertTrue(value.contains("2026-06-21,2026-06-22"));
    assertTrue(value.contains("1->1"));
  }

  @Test
  void acceptsValidSuggestionAndRejectsUntrustedFields() {
    ScheduleSuggestRequest normal = request(1L, 1L,
        List.of(new ExistingSchedule(1L, START.minusDays(1), "09:00-12:00")));
    ScheduleSuggestionItem valid = item(1L, 1L, START, "09:00-12:00", 12);
    assertDoesNotThrow(() -> ScheduleAiResponseValidator.validate(normal, List.of(valid)));

    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        normal, List.of(item(9L, 1L, START, "09:00-12:00", 12))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        request(1L, 2L, List.of()), List.of(item(1L, 2L, START, "09:00-12:00", 12))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        normal, List.of(item(1L, 1L, START.minusDays(1), "09:00-12:00", 12))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        normal, List.of(item(1L, 1L, START, "09:00-12:00", 0))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        normal, List.of(item(1L, 1L, START, "bad", 12))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        normal, List.of(item(1L, 1L, START, "12:00-09:00", 12))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        normal, List.of(item(1L, 1L, START, "25:00-26:00", 12))));
    assertThrows(IllegalStateException.class, () -> ScheduleAiResponseValidator.validate(
        request(1L, 1L, List.of(new ExistingSchedule(1L, START, "09:00-12:00"))),
        List.of(valid)));
  }

  private static ScheduleSuggestRequest request(
      Long allowedDepartmentId,
      Long doctorDepartmentId,
      List<ExistingSchedule> existing
  ) {
    return new ScheduleSuggestRequest(
        START,
        2,
        List.of(new ScheduleDoctorCandidate(1L, "王医生", doctorDepartmentId, "CARDIOLOGY", "胸痛", true)),
        List.of(new ScheduleDepartmentCandidate(allowedDepartmentId, "CARDIOLOGY", "心内科")),
        existing
    );
  }

  private static ScheduleSuggestionItem item(
      Long doctorId,
      Long departmentId,
      LocalDate date,
      String timeRange,
      int capacity
  ) {
    return new ScheduleSuggestionItem(doctorId, departmentId, date, timeRange, capacity, "建议");
  }
}

package com.smartcloudbrain.ai.provider.dify;

import com.smartcloudbrain.aiapi.dto.ExistingSchedule;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class ScheduleAiResponseValidator {

  private ScheduleAiResponseValidator() {
  }

  static String constrainedPrompt(PromptResolveResponse prompt, ScheduleSuggestRequest request) {
    String dates = IntStream.range(0, request.days())
        .mapToObj(offset -> request.startDate().plusDays(offset).toString())
        .collect(Collectors.joining(","));
    String doctors = request.doctors().stream()
        .filter(ScheduleDoctorCandidate::enabled)
        .map(item -> item.doctorId() + "->" + item.departmentId())
        .collect(Collectors.joining(","));
    return prompt.templateContent()
        + "\nHARD CONSTRAINTS: allowed workDate values are exactly [" + dates + "]"
        + "; allowed doctorId->departmentId pairs are exactly [" + doctors + "]"
        + ". Do not invent IDs or dates. Return compact JSON only.";
  }

  static void validate(ScheduleSuggestRequest request, List<ScheduleSuggestionItem> suggestions) {
    Map<Long, Long> doctorDepartments = new HashMap<>();
    request.doctors().stream().filter(ScheduleDoctorCandidate::enabled)
        .forEach(item -> doctorDepartments.put(item.doctorId(), item.departmentId()));
    Set<Long> departmentIds = request.departments().stream()
        .map(item -> item.departmentId()).collect(Collectors.toSet());
    LocalDate endDate = request.startDate().plusDays(request.days() - 1L);
    Set<String> occupied = request.existingSchedules().stream()
        .map(ScheduleAiResponseValidator::scheduleKey).collect(Collectors.toCollection(HashSet::new));

    for (ScheduleSuggestionItem item : suggestions) {
      if (!doctorDepartments.containsKey(item.doctorId())
          || !doctorDepartments.get(item.doctorId()).equals(item.departmentId())) {
        throw new IllegalStateException("Dify schedule returned an unknown doctor or mismatched department");
      }
      if (!departmentIds.contains(item.departmentId())) {
        throw new IllegalStateException("Dify schedule returned an unknown department");
      }
      if (item.workDate().isBefore(request.startDate()) || item.workDate().isAfter(endDate)) {
        throw new IllegalStateException("Dify schedule returned a date outside the requested range");
      }
      if (item.capacity() < 1 || item.capacity() > 100) {
        throw new IllegalStateException("Dify schedule returned an invalid capacity");
      }
      validateTimeRange(item.timeRange());
      if (!occupied.add(scheduleKey(item))) {
        throw new IllegalStateException("Dify schedule returned a duplicate or occupied doctor slot");
      }
    }
  }

  private static void validateTimeRange(String timeRange) {
    String[] parts = timeRange == null ? new String[0] : timeRange.split("-", -1);
    try {
      if (parts.length != 2 || !LocalTime.parse(parts[0]).isBefore(LocalTime.parse(parts[1]))) {
        throw new IllegalStateException("Dify schedule returned an invalid time range");
      }
    } catch (DateTimeParseException ex) {
      throw new IllegalStateException("Dify schedule returned an invalid time range", ex);
    }
  }

  private static String scheduleKey(ExistingSchedule item) {
    return item.doctorId() + "|" + item.workDate() + "|" + item.timeRange();
  }

  private static String scheduleKey(ScheduleSuggestionItem item) {
    return item.doctorId() + "|" + item.workDate() + "|" + item.timeRange();
  }
}

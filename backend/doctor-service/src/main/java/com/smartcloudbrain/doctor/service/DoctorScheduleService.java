package com.smartcloudbrain.doctor.service;

import com.smartcloudbrain.doctor.dto.internal.InternalSchedulePublishRequest;
import com.smartcloudbrain.doctor.dto.internal.InternalScheduleCancelRequest;
import com.smartcloudbrain.doctor.dto.internal.InternalScheduleSaveRequest;
import com.smartcloudbrain.doctor.entity.AppointmentSlot;
import com.smartcloudbrain.doctor.entity.Department;
import com.smartcloudbrain.doctor.entity.Doctor;
import com.smartcloudbrain.doctor.entity.DoctorSchedule;
import com.smartcloudbrain.doctor.repository.AppointmentSlotRepository;
import com.smartcloudbrain.doctor.repository.DepartmentRepository;
import com.smartcloudbrain.doctor.repository.DoctorRepository;
import com.smartcloudbrain.doctor.repository.DoctorScheduleRepository;
import com.smartcloudbrain.doctor.repository.RegistrationRepository;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorScheduleService {

  private final DoctorScheduleRepository doctorScheduleRepository;
  private final AppointmentSlotRepository appointmentSlotRepository;
  private final RegistrationRepository registrationRepository;
  private final DoctorRepository doctorRepository;
  private final DepartmentRepository departmentRepository;

  public DoctorScheduleService(
      DoctorScheduleRepository doctorScheduleRepository,
      AppointmentSlotRepository appointmentSlotRepository,
      RegistrationRepository registrationRepository,
      DoctorRepository doctorRepository,
      DepartmentRepository departmentRepository
  ) {
    this.doctorScheduleRepository = doctorScheduleRepository;
    this.appointmentSlotRepository = appointmentSlotRepository;
    this.registrationRepository = registrationRepository;
    this.doctorRepository = doctorRepository;
    this.departmentRepository = departmentRepository;
  }

  @Transactional
  public List<Map<String, Object>> publishSchedules(InternalSchedulePublishRequest request) {
    List<InternalSchedulePublishRequest.ScheduleItem> items = request == null || request.schedules() == null
        ? List.of()
        : request.schedules();
    List<DoctorSchedule> pendingSchedules = new ArrayList<>();
    for (InternalSchedulePublishRequest.ScheduleItem item : items) {
      validateScheduleValues(
          item == null ? null : item.doctorId(),
          item == null ? null : item.departmentId(),
          item == null ? null : item.workDate(),
          item == null ? null : item.timeRange(),
          item == null ? null : item.capacity(),
          "PUBLISHED",
          null,
          pendingSchedules
      );
      DoctorSchedule pending = new DoctorSchedule();
      pending.setDoctorId(item.doctorId());
      pending.setDepartmentId(item.departmentId());
      pending.setWorkDate(item.workDate());
      pending.setTimeRange(item.timeRange());
      pending.setCapacity(item.capacity());
      pending.setStatus("PUBLISHED");
      pendingSchedules.add(pending);
    }
    for (InternalSchedulePublishRequest.ScheduleItem item : items) {
      DoctorSchedule schedule = new DoctorSchedule();
      schedule.setDoctorId(item.doctorId());
      schedule.setDepartmentId(item.departmentId());
      schedule.setWorkDate(item.workDate());
      schedule.setTimeRange(item.timeRange());
      schedule.setCapacity(item.capacity());
      schedule.setStatus("PUBLISHED");
      schedule.setUpdatedAt(LocalDateTime.now());
      DoctorSchedule savedSchedule = doctorScheduleRepository.save(schedule);

      AppointmentSlot slot = new AppointmentSlot();
      slot.setScheduleId(savedSchedule.getId());
      slot.setDoctorId(savedSchedule.getDoctorId());
      slot.setDepartmentId(savedSchedule.getDepartmentId());
      slot.setStartTime(slotStart(savedSchedule.getWorkDate(), savedSchedule.getTimeRange()));
      slot.setEndTime(slotEnd(savedSchedule.getWorkDate(), savedSchedule.getTimeRange()));
      slot.setCapacity(savedSchedule.getCapacity());
      slot.setRemainingCapacity(savedSchedule.getCapacity());
      slot.setStatus("AVAILABLE");
      slot.setUpdatedAt(LocalDateTime.now());
      appointmentSlotRepository.save(slot);
    }
    return schedules();
  }

  public List<Map<String, Object>> schedules() {
    return doctorScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAscDoctorIdAsc(LocalDate.now()).stream()
        .map(this::scheduleView)
        .toList();
  }

  public List<Map<String, Object>> schedules(LocalDate startDate, LocalDate endDate, Long departmentId, Long doctorId, String status) {
    LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
    LocalDate end = endDate == null ? LocalDate.now().plusDays(90) : endDate;
    return doctorScheduleRepository.findByWorkDateGreaterThanEqualAndWorkDateLessThanEqualOrderByWorkDateAscDoctorIdAsc(start, end).stream()
        .filter(schedule -> departmentId == null || departmentId.equals(schedule.getDepartmentId()))
        .filter(schedule -> doctorId == null || doctorId.equals(schedule.getDoctorId()))
        .filter(schedule -> status == null || status.isBlank() || status.equalsIgnoreCase(schedule.getStatus()))
        .map(this::scheduleView)
        .toList();
  }

  @Transactional
  public Map<String, Object> saveSchedule(InternalScheduleSaveRequest request) {
    validateScheduleRequest(request);
    DoctorSchedule schedule = request.id() == null
        ? new DoctorSchedule()
        : doctorScheduleRepository.findById(request.id()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    schedule.setDoctorId(request.doctorId());
    schedule.setDepartmentId(request.departmentId());
    schedule.setWorkDate(request.workDate());
    schedule.setTimeRange(request.timeRange());
    schedule.setCapacity(request.capacity());
    schedule.setStatus(request.status() == null || request.status().isBlank() ? "PUBLISHED" : request.status());
    schedule.setUpdatedAt(LocalDateTime.now());
    DoctorSchedule saved = doctorScheduleRepository.save(schedule);
    upsertSlot(saved);
    return scheduleView(saved);
  }

  @Transactional
  public Map<String, Object> cancelSchedule(InternalScheduleCancelRequest request) {
    if (request == null || request.scheduleId() == null) {
      throw new BusinessException(400, "scheduleId is required");
    }
    DoctorSchedule schedule = doctorScheduleRepository.findById(request.scheduleId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    AppointmentSlot slot = appointmentSlotRepository.findByScheduleId(schedule.getId()).orElse(null);
    if (slot != null && registrationRepository.existsBySlotIdAndStatusNot(slot.getId(), "CANCELLED")) {
      throw new BusinessException(400, "Cannot cancel schedule with active registrations");
    }
    schedule.setStatus("CANCELLED");
    schedule.setUpdatedAt(LocalDateTime.now());
    if (slot != null) {
      slot.setStatus("CANCELLED");
      slot.setUpdatedAt(LocalDateTime.now());
      appointmentSlotRepository.save(slot);
    }
    return scheduleView(doctorScheduleRepository.save(schedule));
  }

  public List<Map<String, Object>> slots() {
    return appointmentSlotRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(LocalDateTime.now()).stream()
        .map(this::slotView)
        .toList();
  }

  private Map<String, Object> scheduleView(DoctorSchedule schedule) {
    AppointmentSlot slot = appointmentSlotRepository.findByScheduleId(schedule.getId()).orElse(null);
    int booked = slot == null ? 0 : Math.max(0, (slot.getCapacity() == null ? 0 : slot.getCapacity()) - (slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity()));
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", schedule.getId());
    view.put("doctorId", schedule.getDoctorId());
    view.put("doctorName", doctorName(schedule.getDoctorId()));
    view.put("departmentId", schedule.getDepartmentId());
    view.put("departmentName", departmentName(schedule.getDepartmentId()));
    view.put("workDate", schedule.getWorkDate().toString());
    view.put("timeRange", schedule.getTimeRange());
    view.put("capacity", schedule.getCapacity());
    view.put("booked", booked);
    view.put("remainingCapacity", slot == null ? schedule.getCapacity() : slot.getRemainingCapacity());
    view.put("slotId", slot == null ? 0L : slot.getId());
    view.put("status", schedule.getStatus());
    return view;
  }

  private void validateScheduleRequest(InternalScheduleSaveRequest request) {
    if (request == null) {
      throw new BusinessException(400, "doctorId, departmentId and workDate are required");
    }
    validateScheduleValues(
        request.doctorId(),
        request.departmentId(),
        request.workDate(),
        request.timeRange(),
        request.capacity(),
        request.status(),
        request.id(),
        List.of()
    );
  }

  private void validateScheduleValues(
      Long doctorId,
      Long departmentId,
      LocalDate workDate,
      String timeRange,
      Integer capacity,
      String status,
      Long currentScheduleId,
      List<DoctorSchedule> pendingSchedules
  ) {
    if (doctorId == null || departmentId == null || workDate == null) {
      throw new BusinessException(400, "doctorId, departmentId and workDate are required");
    }
    Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!departmentId.equals(doctor.getDepartmentId())) {
      throw new BusinessException(400, "Doctor does not belong to selected department");
    }
    if (capacity == null || capacity < 1 || capacity > 100) {
      throw new BusinessException(400, "capacity must be between 1 and 100");
    }
    TimeRange candidate = parseTimeRange(timeRange);
    if ("CANCELLED".equalsIgnoreCase(status)) {
      return;
    }

    List<DoctorSchedule> occupiedSchedules = new ArrayList<>(
        doctorScheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate)
    );
    occupiedSchedules.addAll(pendingSchedules);
    boolean overlaps = occupiedSchedules.stream()
        .filter(existing -> existing != null
            && doctorId.equals(existing.getDoctorId())
            && workDate.equals(existing.getWorkDate())
            && !"CANCELLED".equalsIgnoreCase(existing.getStatus())
            && (currentScheduleId == null || !currentScheduleId.equals(existing.getId())))
        .map(existing -> parseTimeRange(existing.getTimeRange()))
        .anyMatch(existing -> candidate.start().isBefore(existing.end()) && existing.start().isBefore(candidate.end()));
    if (overlaps) {
      throw new BusinessException(400, "该医生在所选日期已有重叠排班");
    }
  }

  private TimeRange parseTimeRange(String value) {
    if (value == null || !value.matches("^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$")) {
      throw new BusinessException(400, "排班时段格式必须为 HH:mm-HH:mm");
    }
    String[] parts = value.split("-", -1);
    try {
      LocalTime start = LocalTime.parse(parts[0]);
      LocalTime end = LocalTime.parse(parts[1]);
      if (!start.isBefore(end)) {
        throw new BusinessException(400, "排班结束时间必须晚于开始时间");
      }
      return new TimeRange(start, end);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(400, "排班时段格式必须为 HH:mm-HH:mm");
    }
  }

  private record TimeRange(LocalTime start, LocalTime end) {}

  private void upsertSlot(DoctorSchedule schedule) {
    AppointmentSlot slot = appointmentSlotRepository.findByScheduleId(schedule.getId()).orElseGet(AppointmentSlot::new);
    int booked = 0;
    if (slot.getId() != null) {
      booked = Math.max(0, (slot.getCapacity() == null ? 0 : slot.getCapacity()) - (slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity()));
    }
    if (schedule.getCapacity() < booked) {
      throw new BusinessException(400, "capacity cannot be lower than existing registrations");
    }
    slot.setScheduleId(schedule.getId());
    slot.setDoctorId(schedule.getDoctorId());
    slot.setDepartmentId(schedule.getDepartmentId());
    slot.setStartTime(slotStart(schedule.getWorkDate(), schedule.getTimeRange()));
    slot.setEndTime(slotEnd(schedule.getWorkDate(), schedule.getTimeRange()));
    slot.setCapacity(schedule.getCapacity());
    slot.setRemainingCapacity(schedule.getCapacity() - booked);
    slot.setStatus("CANCELLED".equalsIgnoreCase(schedule.getStatus()) ? "CANCELLED" : "AVAILABLE");
    slot.setUpdatedAt(LocalDateTime.now());
    appointmentSlotRepository.save(slot);
  }

  private Map<String, Object> slotView(AppointmentSlot slot) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("slotId", slot.getId());
    view.put("scheduleId", slot.getScheduleId());
    view.put("doctorId", slot.getDoctorId());
    view.put("doctorName", doctorName(slot.getDoctorId()));
    view.put("departmentId", slot.getDepartmentId());
    view.put("departmentName", departmentName(slot.getDepartmentId()));
    view.put("startTime", slot.getStartTime().toString());
    view.put("endTime", slot.getEndTime() == null ? "" : slot.getEndTime().toString());
    view.put("capacity", slot.getCapacity() == null ? 0 : slot.getCapacity());
    view.put("remainingCapacity", slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity());
    view.put("status", slot.getStatus());
    return view;
  }

  private String doctorName(Long doctorId) {
    if (doctorId == null) {
      return "";
    }
    return doctorRepository.findById(doctorId).map(Doctor::getName).orElse("");
  }

  private String departmentName(Long departmentId) {
    if (departmentId == null) {
      return "";
    }
    return departmentRepository.findById(departmentId).map(Department::getName).orElse("");
  }

  private LocalDateTime slotStart(LocalDate workDate, String timeRange) {
    String start = Objects.requireNonNullElse(timeRange, "09:00-12:00").split("-")[0];
    return LocalDateTime.parse(workDate + "T" + start + ":00");
  }

  private LocalDateTime slotEnd(LocalDate workDate, String timeRange) {
    String[] parts = Objects.requireNonNullElse(timeRange, "09:00-12:00").split("-");
    String end = parts.length > 1 ? parts[1] : "12:00";
    return LocalDateTime.parse(workDate + "T" + end + ":00");
  }
}

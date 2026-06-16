package com.smartcloudbrain.doctor.service;

import com.smartcloudbrain.doctor.dto.internal.InternalSchedulePublishRequest;
import com.smartcloudbrain.doctor.entity.AppointmentSlot;
import com.smartcloudbrain.doctor.entity.Department;
import com.smartcloudbrain.doctor.entity.Doctor;
import com.smartcloudbrain.doctor.entity.DoctorSchedule;
import com.smartcloudbrain.doctor.repository.AppointmentSlotRepository;
import com.smartcloudbrain.doctor.repository.DepartmentRepository;
import com.smartcloudbrain.doctor.repository.DoctorRepository;
import com.smartcloudbrain.doctor.repository.DoctorScheduleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  private final DoctorRepository doctorRepository;
  private final DepartmentRepository departmentRepository;

  public DoctorScheduleService(
      DoctorScheduleRepository doctorScheduleRepository,
      AppointmentSlotRepository appointmentSlotRepository,
      DoctorRepository doctorRepository,
      DepartmentRepository departmentRepository
  ) {
    this.doctorScheduleRepository = doctorScheduleRepository;
    this.appointmentSlotRepository = appointmentSlotRepository;
    this.doctorRepository = doctorRepository;
    this.departmentRepository = departmentRepository;
  }

  @Transactional
  public List<Map<String, Object>> publishSchedules(InternalSchedulePublishRequest request) {
    List<InternalSchedulePublishRequest.ScheduleItem> items = request == null || request.schedules() == null
        ? List.of()
        : request.schedules();
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

  public List<Map<String, Object>> slots() {
    return appointmentSlotRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(LocalDateTime.now()).stream()
        .map(this::slotView)
        .toList();
  }

  private Map<String, Object> scheduleView(DoctorSchedule schedule) {
    return Map.of(
        "id", schedule.getId(),
        "doctorId", schedule.getDoctorId(),
        "doctorName", doctorName(schedule.getDoctorId()),
        "departmentId", schedule.getDepartmentId(),
        "departmentName", departmentName(schedule.getDepartmentId()),
        "workDate", schedule.getWorkDate().toString(),
        "timeRange", schedule.getTimeRange(),
        "capacity", schedule.getCapacity(),
        "status", schedule.getStatus()
    );
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

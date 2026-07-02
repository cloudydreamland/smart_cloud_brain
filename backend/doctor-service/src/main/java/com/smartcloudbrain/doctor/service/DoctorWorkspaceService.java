package com.smartcloudbrain.doctor.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DoctorWorkspaceService {

  private final JdbcTemplate jdbcTemplate;

  public DoctorWorkspaceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> dashboard(Long doctorId) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("todayRegistrations", count("""
        SELECT COUNT(*) FROM registration
        WHERE doctor_id = ? AND CAST(appointment_time AS DATE) = CURRENT_DATE
        """, doctorId));
    view.put("pendingRegistrations", count("""
        SELECT COUNT(*) FROM registration
        WHERE doctor_id = ? AND status IN ('CREATED', 'CHECKED_IN', 'IN_PROGRESS')
        """, doctorId));
    view.put("completedRegistrations", count("""
        SELECT COUNT(*) FROM registration
        WHERE doctor_id = ? AND status = 'COMPLETED'
        """, doctorId));
    view.put("upcomingSchedules", count("""
        SELECT COUNT(*) FROM doctor_schedule
        WHERE doctor_id = ? AND work_date >= CURRENT_DATE AND status <> 'CANCELLED'
        """, doctorId));
    view.put("queue", queue(doctorId));
    view.put("schedules", schedules(doctorId));
    return view;
  }

  public List<Map<String, Object>> queue(Long doctorId) {
    return jdbcTemplate.queryForList("""
        SELECT r.id AS "registrationId",
               r.patient_id AS "patientId",
               COALESCE(r.owner_patient_id, r.patient_id) AS "ownerPatientId",
               COALESCE(NULLIF(r.subject_type, ''), NULLIF(r.visitor_type, ''), 'ACCOUNT') AS "subjectType",
               COALESCE(r.subject_id, r.visitor_id, r.patient_id) AS "subjectId",
               COALESCE(NULLIF(r.subject_name, ''), NULLIF(r.visitor_name, ''), p.name) AS "subjectName",
               COALESCE(NULLIF(r.subject_relationship, ''), NULLIF(r.visitor_relationship, ''), '本人') AS "subjectRelationship",
               COALESCE(NULLIF(r.subject_name, ''), NULLIF(r.visitor_name, ''), p.name) AS "patientName",
               COALESCE(NULLIF(r.subject_gender, ''), NULLIF(r.visitor_gender, ''), p.gender) AS "gender",
               COALESCE(r.subject_age, r.visitor_age, p.age) AS "age",
               COALESCE(r.subject_id, r.visitor_id, r.patient_id) AS "visitorId",
               COALESCE(NULLIF(r.subject_type, ''), NULLIF(r.visitor_type, ''), 'ACCOUNT') AS "visitorType",
               COALESCE(NULLIF(r.subject_relationship, ''), NULLIF(r.visitor_relationship, ''), '本人') AS "visitorRelationship",
               r.department_id AS "departmentId",
               dep.name AS "departmentName",
               r.triage_record_id AS "triageRecordId",
               r.slot_id AS "slotId",
               r.appointment_time AS "appointmentTime",
               r.status,
               t.chief_complaint AS "chiefComplaint",
               t.recommended_department AS "recommendedDepartment",
               t.reason AS "triageReason",
               t.status AS "triageStatus"
        FROM registration r
        LEFT JOIN patient p ON p.id = r.patient_id
        LEFT JOIN department dep ON dep.id = r.department_id
        LEFT JOIN triage_record t ON t.id = r.triage_record_id
        WHERE r.doctor_id = ?
          AND r.status IN ('CREATED', 'CHECKED_IN', 'IN_PROGRESS')
        ORDER BY r.appointment_time ASC, r.id ASC
        """, doctorId);
  }

  private List<Map<String, Object>> schedules(Long doctorId) {
    return jdbcTemplate.queryForList("""
        SELECT ds.id,
               ds.department_id AS "departmentId",
               dep.name AS "departmentName",
               ds.work_date AS "workDate",
               ds.time_range AS "timeRange",
               ds.capacity,
               ds.status
        FROM doctor_schedule ds
        LEFT JOIN department dep ON dep.id = ds.department_id
        WHERE ds.doctor_id = ? AND ds.work_date >= ?
        ORDER BY ds.work_date ASC, ds.time_range ASC, ds.id ASC
        LIMIT 7
        """, doctorId, LocalDate.now());
  }

  private long count(String sql, Long doctorId) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, doctorId);
    return value == null ? 0L : value;
  }
}

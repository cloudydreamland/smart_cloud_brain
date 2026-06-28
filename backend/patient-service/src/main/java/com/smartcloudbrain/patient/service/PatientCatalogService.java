package com.smartcloudbrain.patient.service;

import com.smartcloudbrain.patient.entity.Department;
import com.smartcloudbrain.patient.entity.Doctor;
import com.smartcloudbrain.patient.repository.DepartmentRepository;
import com.smartcloudbrain.patient.repository.DoctorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PatientCatalogService {

  private final DepartmentRepository departmentRepository;
  private final DoctorRepository doctorRepository;

  public PatientCatalogService(DepartmentRepository departmentRepository, DoctorRepository doctorRepository) {
    this.departmentRepository = departmentRepository;
    this.doctorRepository = doctorRepository;
  }

  public List<Map<String, Object>> departments() {
    return departmentRepository.findAll().stream().map(department -> Map.<String, Object>of(
        "id", department.getId(),
        "code", value(department.getCode()),
        "name", value(department.getName()),
        "description", value(department.getDescription())
    )).toList();
  }

  public List<Map<String, Object>> doctors(Long departmentId) {
    List<Doctor> doctors = departmentId == null
        ? doctorRepository.findAll()
        : doctorRepository.findByDepartmentId(departmentId);
    return doctors.stream().map(this::doctorView).toList();
  }

  private Map<String, Object> doctorView(Doctor doctor) {
    Department department = doctor.getDepartmentId() == null
        ? null
        : departmentRepository.findById(doctor.getDepartmentId()).orElse(null);
    return Map.of(
        "id", doctor.getId(),
        "name", value(doctor.getName()),
        "phone", value(doctor.getPhone()),
        "departmentId", doctor.getDepartmentId() == null ? 0L : doctor.getDepartmentId(),
        "departmentCode", department == null ? "" : value(department.getCode()),
        "departmentName", department == null ? "" : value(department.getName()),
        "title", value(doctor.getTitle()),
        "specialty", value(doctor.getSpecialty()),
        "status", value(doctor.getStatus(), "ENABLED")
    );
  }

  private String value(String value) {
    return value(value, "");
  }

  private String value(String value, String fallback) {
    return value == null ? fallback : value;
  }
}

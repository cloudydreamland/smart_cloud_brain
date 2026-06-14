package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.diagnosis.entity.Department;
import com.smartcloudbrain.diagnosis.entity.Doctor;
import com.smartcloudbrain.diagnosis.repository.DepartmentRepository;
import com.smartcloudbrain.diagnosis.repository.DoctorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final DepartmentRepository departmentRepository;

  public DoctorService(DoctorRepository doctorRepository, DepartmentRepository departmentRepository) {
    this.doctorRepository = doctorRepository;
    this.departmentRepository = departmentRepository;
  }

  public List<Map<String, Object>> listDoctors(Long departmentId) {
    List<Doctor> doctors = departmentId == null ? doctorRepository.findAll() : doctorRepository.findByDepartmentId(departmentId);
    return doctors.stream().map(this::doctorView).toList();
  }

  public Map<String, Object> doctorDetail(Long id) {
    return doctorView(doctorRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  public Map<String, Object> doctorView(Doctor doctor) {
    Department department = departmentRepository.findById(doctor.getDepartmentId()).orElse(null);
    return Map.of(
        "id", doctor.getId(),
        "name", doctor.getName(),
        "phone", doctor.getPhone() == null ? "" : doctor.getPhone(),
        "departmentId", doctor.getDepartmentId(),
        "departmentCode", department == null ? "" : department.getCode(),
        "departmentName", department == null ? "" : department.getName(),
        "title", doctor.getTitle() == null ? "" : doctor.getTitle(),
        "specialty", doctor.getSpecialty() == null ? "" : doctor.getSpecialty(),
        "status", doctor.getStatus() == null ? "ENABLED" : doctor.getStatus()
    );
  }
}

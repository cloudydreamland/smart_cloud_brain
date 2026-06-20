package com.smartcloudbrain.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.doctor.entity.Department;
import com.smartcloudbrain.doctor.entity.Doctor;
import com.smartcloudbrain.doctor.repository.DepartmentRepository;
import com.smartcloudbrain.doctor.repository.DoctorRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {
  @Mock private DoctorRepository doctorRepository;
  @Mock private DepartmentRepository departmentRepository;
  @InjectMocks private DoctorService service;

  @Test
  void listsAllAndDepartmentDoctorsWithEnrichedDepartment() {
    Department department = new Department();
    department.setId(3L);
    department.setCode("CARDIOLOGY");
    department.setName("心内科");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("医生");
    doctor.setDepartmentId(3L);
    when(doctorRepository.findAll()).thenReturn(List.of(doctor));
    when(doctorRepository.findByDepartmentId(3L)).thenReturn(List.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

    assertEquals(1, service.listDoctors(null).size());
    assertEquals("CARDIOLOGY", service.listDoctors(3L).get(0).get("departmentCode"));
  }

  @Test
  void readsDoctorDetailAndDepartmentsWithNullDefaults() {
    Department department = new Department();
    department.setId(3L);
    department.setCode("GENERAL");
    department.setName("全科");
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    doctor.setName("医生");
    doctor.setDepartmentId(3L);
    when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.empty());
    when(departmentRepository.findAll()).thenReturn(List.of(department));

    assertEquals("ENABLED", service.doctorDetail(2L).get("status"));
    assertEquals("", service.departments().get(0).get("description"));
  }

  @Test
  void rejectsMissingDoctor() {
    when(doctorRepository.findById(9L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.doctorDetail(9L));
  }
}

package com.smartcloudbrain.registration.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.registration.entity.Registration;
import com.smartcloudbrain.registration.repository.AppointmentSlotRepository;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import com.smartcloudbrain.registration.repository.PatientRepository;
import com.smartcloudbrain.registration.repository.RegistrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServicePermissionTest {

  @Mock private RegistrationRepository registrationRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private AppointmentSlotRepository appointmentSlotRepository;
  @Mock private CurrentUserService currentUserService;
  @InjectMocks private RegistrationService registrationService;

  @Test
  void patientCannotCancelAnotherPatientsRegistration() {
    Registration registration = new Registration();
    registration.setPatientId(2L);
    registration.setDoctorId(1L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "patient"));
    when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

    assertThrows(BusinessException.class, () -> registrationService.cancel(10L));
    verify(registrationRepository, never()).save(any());
  }
}

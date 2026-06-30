package com.smartcloudbrain.auth.repository;

import com.smartcloudbrain.auth.entity.PatientEmailVerificationCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientEmailVerificationCodeRepository extends JpaRepository<PatientEmailVerificationCode, Long> {

  Optional<PatientEmailVerificationCode> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);

  Optional<PatientEmailVerificationCode> findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(String email, String purpose);
}

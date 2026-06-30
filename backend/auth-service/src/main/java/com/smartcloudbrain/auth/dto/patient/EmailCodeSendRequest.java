package com.smartcloudbrain.auth.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeSendRequest(
    @NotBlank @Email String email,
    String phone,
    String purpose
) {
}

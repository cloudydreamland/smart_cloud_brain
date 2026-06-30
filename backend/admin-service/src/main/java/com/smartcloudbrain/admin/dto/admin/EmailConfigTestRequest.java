package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailConfigTestRequest(
    @NotBlank @Email String toAddress
) {
}

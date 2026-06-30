package com.smartcloudbrain.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequest(
    @NotBlank @Email String toAddress,
    @NotBlank String subject,
    @NotBlank String content
) {
}

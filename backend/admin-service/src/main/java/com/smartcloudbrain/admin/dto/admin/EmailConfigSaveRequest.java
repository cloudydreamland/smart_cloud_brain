package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailConfigSaveRequest(
    @NotBlank String host,
    @NotNull Integer port,
    @NotBlank String username,
    String password,
    @NotBlank @Email String fromAddress,
    String fromName,
    Boolean sslEnabled,
    Boolean starttlsEnabled,
    Boolean enabled
) {
}

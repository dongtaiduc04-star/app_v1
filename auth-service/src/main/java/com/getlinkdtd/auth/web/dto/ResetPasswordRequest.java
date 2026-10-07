package com.getlinkdtd.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 128) String token,
        @NotBlank @Size(min = 10, max = 25) String newPassword) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[token=<redacted>, newPassword=<redacted>]";
    }
}

package com.getlinkdtd.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank @Size(max = 254) String identifier) {
    @Override
    public String toString() {
        return "ForgotPasswordRequest[identifier=<redacted>]";
    }
}

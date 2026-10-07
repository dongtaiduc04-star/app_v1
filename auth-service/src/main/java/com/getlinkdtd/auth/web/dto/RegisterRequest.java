package com.getlinkdtd.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 30) String username,
        @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 25) String password) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", password=<redacted>]";
    }
}

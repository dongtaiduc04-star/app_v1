package com.getlinkdtd.auth.web.dto;

import com.getlinkdtd.auth.domain.RoleName;
import com.getlinkdtd.auth.domain.User;
import com.getlinkdtd.auth.domain.UserStatus;

import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String username,
        String email,
        UserStatus status,
        List<String> roles) {
    public static AccountResponse from(User user) {
        return new AccountResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream().map(RoleName::name).sorted().toList());
    }
}

package com.getlinkdtd.link.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 50) String displayName,
        @Size(max = 160) String bio,
        @Pattern(regexp = "aurora|light|sunset|midnight") String backgroundTheme,
        @Pattern(regexp = "soft|pill|square|outline") String buttonStyle,
        @Pattern(regexp = "system|arial|tahoma") String fontFamily) {
}

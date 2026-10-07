package com.getlinkdtd.link.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank @Size(max = 60) String title,
        @NotBlank @Size(max = 2048) String destinationUrl,
        @Size(max = 40) String icon) {
}

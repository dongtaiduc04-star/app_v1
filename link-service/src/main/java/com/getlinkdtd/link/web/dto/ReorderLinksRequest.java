package com.getlinkdtd.link.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReorderLinksRequest(
        @NotEmpty @Size(max = 30) List<UUID> linkIds) {
}

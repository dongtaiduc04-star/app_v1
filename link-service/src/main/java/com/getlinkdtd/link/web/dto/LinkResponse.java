package com.getlinkdtd.link.web.dto;

import com.getlinkdtd.link.domain.Link;

import java.util.UUID;

public record LinkResponse(
        UUID id,
        String title,
        String destinationUrl,
        String icon,
        int position,
        boolean enabled,
        long clickCount,
        long version) {
    public static LinkResponse from(Link link) {
        return new LinkResponse(link.getId(), link.getTitle(), link.getDestinationUrl(), link.getIcon(),
                link.getPosition(), link.isEnabled(), link.getClickCount(), link.getVersion());
    }
}

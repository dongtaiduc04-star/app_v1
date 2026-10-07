package com.getlinkdtd.link.web.dto;

import com.getlinkdtd.link.domain.Link;
import com.getlinkdtd.link.domain.Profile;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String backgroundTheme,
        String buttonStyle,
        String fontFamily,
        String status,
        long version,
        List<LinkResponse> links) {
    public static ProfileResponse from(Profile profile, List<Link> links) {
        return new ProfileResponse(
                profile.getId(), profile.getUsername(), profile.getDisplayName(), profile.getBio(),
                profile.getAvatarUrl(), profile.getBackgroundTheme(), profile.getButtonStyle(),
                profile.getFontFamily(), profile.getStatus().name(), profile.getVersion(),
                links.stream().map(LinkResponse::from).toList());
    }
}

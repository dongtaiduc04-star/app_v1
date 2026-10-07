package com.getlinkdtd.link.service;

import com.getlinkdtd.link.domain.Link;
import com.getlinkdtd.link.domain.Profile;
import com.getlinkdtd.link.domain.ProfileStatus;
import com.getlinkdtd.link.repository.LinkRepository;
import com.getlinkdtd.link.repository.ProfileRepository;
import com.getlinkdtd.link.web.ApiException;
import com.getlinkdtd.link.web.dto.CreateLinkRequest;
import com.getlinkdtd.link.web.dto.LinkResponse;
import com.getlinkdtd.link.web.dto.ProfileResponse;
import com.getlinkdtd.link.web.dto.UpdateLinkRequest;
import com.getlinkdtd.link.web.dto.UpdateProfileRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfileLinkService {
    private final ProfileRepository profiles;
    private final LinkRepository links;
    private final AvatarStorageService avatars;
    private final int maxLinks;

    public ProfileLinkService(ProfileRepository profiles, LinkRepository links, AvatarStorageService avatars,
                              @Value("${getlink.link.max-links-per-profile:30}") int maxLinks) {
        this.profiles = profiles;
        this.links = links;
        this.avatars = avatars;
        this.maxLinks = maxLinks;
    }

    @Transactional
    public ProfileResponse ownedProfile(Jwt jwt) {
        Profile profile = owned(jwt);
        return ProfileResponse.from(profile, links.findByProfileIdOrderByPositionAsc(profile.getId()));
    }

    @Transactional(readOnly = true)
    public ProfileResponse publicProfile(String username) {
        Profile profile = profiles.findByUsernameIgnoreCase(username)
                .filter(value -> value.getStatus() == ProfileStatus.ACTIVE)
                .orElseThrow(() -> notFound("Profile not found"));
        return ProfileResponse.from(profile,
                links.findByProfileIdAndEnabledTrueOrderByPositionAsc(profile.getId()));
    }

    @Transactional
    public ProfileResponse updateProfile(Jwt jwt, UpdateProfileRequest request) {
        Profile profile = owned(jwt);
        profile.update(request.displayName().trim(), nullable(request.bio()), profile.getAvatarUrl());
        profile.updateAppearance(
                request.backgroundTheme() == null ? profile.getBackgroundTheme() : request.backgroundTheme(),
                request.buttonStyle() == null ? profile.getButtonStyle() : request.buttonStyle(),
                request.fontFamily() == null ? profile.getFontFamily() : request.fontFamily());
        return ProfileResponse.from(profile, links.findByProfileIdOrderByPositionAsc(profile.getId()));
    }

    @Transactional
    public ProfileResponse updateAvatar(Jwt jwt, MultipartFile file) {
        Profile profile = owned(jwt);
        String previousUrl = profile.getAvatarUrl();
        String filename = avatars.store(file);
        String avatarUrl = "/api/links/avatars/" + filename;
        profile.update(profile.getDisplayName(), profile.getBio(), avatarUrl);
        profiles.flush();
        avatars.deleteFromPublicUrl(previousUrl);
        return ProfileResponse.from(profile, links.findByProfileIdOrderByPositionAsc(profile.getId()));
    }

    @Transactional
    public LinkResponse createLink(Jwt jwt, CreateLinkRequest request) {
        Profile profile = owned(jwt);
        long count = links.countByProfileId(profile.getId());
        if (count >= maxLinks) {
            throw new ApiException(HttpStatus.CONFLICT, "LINK_LIMIT_REACHED", "Link limit reached");
        }
        Link link = new Link(profile, request.title().trim(), validatedUrl(request.destinationUrl()), (int) count);
        link.update(link.getTitle(), link.getDestinationUrl(), nullable(request.icon()), true);
        return LinkResponse.from(links.save(link));
    }

    @Transactional
    public LinkResponse updateLink(Jwt jwt, UUID linkId, UpdateLinkRequest request) {
        Link link = ownedLink(jwt, linkId);
        link.update(request.title().trim(), validatedUrl(request.destinationUrl()), nullable(request.icon()), request.enabled());
        return LinkResponse.from(link);
    }

    @Transactional
    public void deleteLink(Jwt jwt, UUID linkId) {
        Link link = ownedLink(jwt, linkId);
        UUID profileId = link.getProfile().getId();
        links.delete(link);
        links.flush();
        normalizePositions(links.findByProfileIdOrderByPositionAsc(profileId));
    }

    @Transactional
    public List<LinkResponse> reorder(Jwt jwt, List<UUID> linkIds) {
        Profile profile = owned(jwt);
        List<Link> existing = links.findByProfileIdOrderByPositionAsc(profile.getId());
        if (linkIds.size() != existing.size() || new HashSet<>(linkIds).size() != linkIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LINK_ORDER", "Order must contain every link exactly once");
        }
        var byId = existing.stream().collect(java.util.stream.Collectors.toMap(Link::getId, link -> link));
        if (!byId.keySet().equals(new HashSet<>(linkIds))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LINK_ORDER", "Order contains an unknown link");
        }
        for (int index = 0; index < linkIds.size(); index++) {
            byId.get(linkIds.get(index)).moveTo(1000 + index);
        }
        links.flush();
        for (int index = 0; index < linkIds.size(); index++) {
            byId.get(linkIds.get(index)).moveTo(index);
        }
        links.flush();
        return linkIds.stream().map(byId::get).map(LinkResponse::from).toList();
    }

    @Transactional
    public String recordClick(UUID linkId) {
        Link link = links.findByIdAndEnabledTrue(linkId)
                .filter(value -> value.getProfile().getStatus() == ProfileStatus.ACTIVE)
                .orElseThrow(() -> notFound("Link not found"));
        link.recordClick();
        return link.getDestinationUrl();
    }

    private Profile owned(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "Invalid access token subject");
        }
        String username = jwt.getClaimAsString("username");
        if (username == null || username.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "Username claim is missing");
        }
        UUID finalUserId = userId;
        return profiles.findByUserId(userId).orElseGet(() ->
                profiles.save(new Profile(finalUserId, username.toLowerCase(Locale.ROOT), username)));
    }

    private Link ownedLink(Jwt jwt, UUID linkId) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "Invalid access token subject");
        }
        return links.findByIdAndProfileUserId(linkId, userId)
                .orElseThrow(() -> notFound("Link not found"));
    }

    private void normalizePositions(List<Link> values) {
        for (int index = 0; index < values.size(); index++) {
            values.get(index).moveTo(1000 + index);
        }
        links.flush();
        for (int index = 0; index < values.size(); index++) {
            values.get(index).moveTo(index);
        }
        links.flush();
    }

    private String validatedUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new URISyntaxException(value, "Only absolute HTTP(S) URLs are allowed");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Only absolute HTTP(S) URLs are allowed");
        }
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}

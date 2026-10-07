package com.getlinkdtd.link.web;

import com.getlinkdtd.link.service.ProfileLinkService;
import com.getlinkdtd.link.service.AvatarStorageService;
import com.getlinkdtd.link.web.dto.CreateLinkRequest;
import com.getlinkdtd.link.web.dto.LinkResponse;
import com.getlinkdtd.link.web.dto.ProfileResponse;
import com.getlinkdtd.link.web.dto.ReorderLinksRequest;
import com.getlinkdtd.link.web.dto.UpdateLinkRequest;
import com.getlinkdtd.link.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProfileLinkController {
    private final ProfileLinkService service;
    private final AvatarStorageService avatars;

    public ProfileLinkController(ProfileLinkService service, AvatarStorageService avatars) {
        this.service = service;
        this.avatars = avatars;
    }

    @GetMapping("/profiles/{username}")
    ProfileResponse publicProfile(@PathVariable String username) {
        return service.publicProfile(username);
    }

    @GetMapping("/me/profile")
    ProfileResponse ownedProfile(@AuthenticationPrincipal Jwt jwt) {
        return service.ownedProfile(jwt);
    }

    @PatchMapping("/me/profile")
    ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(jwt, request);
    }

    @PostMapping(value = "/me/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ProfileResponse uploadAvatar(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        return service.updateAvatar(jwt, file);
    }

    @GetMapping("/avatars/{filename}")
    ResponseEntity<Resource> avatar(@PathVariable String filename) {
        AvatarStorageService.StoredAvatar avatar = avatars.load(filename);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic().immutable())
                .contentType(avatar.mediaType())
                .body(avatar.resource());
    }

    @GetMapping("/me/links")
    List<LinkResponse> links(@AuthenticationPrincipal Jwt jwt) {
        return service.ownedProfile(jwt).links();
    }

    @PostMapping("/me/links")
    ResponseEntity<LinkResponse> createLink(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createLink(jwt, request));
    }

    @PatchMapping("/me/links/{linkId}")
    LinkResponse updateLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID linkId,
                            @Valid @RequestBody UpdateLinkRequest request) {
        return service.updateLink(jwt, linkId, request);
    }

    @DeleteMapping("/me/links/{linkId}")
    ResponseEntity<Void> deleteLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID linkId) {
        service.deleteLink(jwt, linkId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/links/order")
    List<LinkResponse> reorder(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ReorderLinksRequest request) {
        return service.reorder(jwt, request.linkIds());
    }

    @GetMapping("/r/{linkId}")
    ResponseEntity<Void> redirect(@PathVariable UUID linkId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(service.recordClick(linkId)))
                .build();
    }
}
